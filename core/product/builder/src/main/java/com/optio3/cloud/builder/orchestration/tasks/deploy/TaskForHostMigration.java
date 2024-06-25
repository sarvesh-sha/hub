/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.optio3.cloud.builder.model.customer.CustomerServiceDesiredState;
import com.optio3.cloud.builder.model.customer.CustomerServiceDesiredStateRole;
import com.optio3.cloud.builder.model.deployment.DeploymentHost;
import com.optio3.cloud.builder.model.deployment.DeploymentHostConfig;
import com.optio3.cloud.builder.model.jobs.output.RegistryImageReleaseStatus;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.customer.BackupKind;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceBackupRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.logic.BackgroundActivityStatus;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.Exceptions;

public class TaskForHostMigration extends BaseDeployTask
{
    public enum State
    {
        CreateBackup,
        TransferBackup,
        TerminateHost,
        CreateHost,
        ReloadBackup
    }

    public String                                     serviceDisplayName;
    public RecordLocator<CustomerServiceBackupRecord> loc_backup;
    public List<DeploymentHostConfig>                 newHostsConfig;

    public RecordLocator<BackgroundActivityRecord>       loc_activity_backup;
    public List<RecordLocator<BackgroundActivityRecord>> loc_activities_hostTermination;
    public List<RecordLocator<BackgroundActivityRecord>> loc_activities_hostCreation;
    public RecordLocator<BackgroundActivityRecord>       loc_activity_restore;

    public static BackgroundActivityRecord scheduleTask(RecordLocked<CustomerServiceRecord> lock_targetService,
                                                        Duration timeout) throws
                                                                          Exception
    {
        Exceptions.requireNotNull(lock_targetService, InvalidArgumentException.class, "No customer service provided");

        CustomerServiceRecord targetService = lock_targetService.get();

        BackgroundActivityRecord rec_task = BaseDeployTask.scheduleActivity(lock_targetService, TaskForHostMigration.class, (t) ->
        {
            CustomerRecord rec_cust = targetService.getCustomer();
            t.serviceDisplayName = String.format("%s / %s", rec_cust.getName(), targetService.getName());

            t.loggerInstance.info("Migration host for '%s'", t.serviceDisplayName);
            t.initializeTimeout(timeout);
        });

        BackgroundActivityRecord rec_taskPrevious = targetService.getCurrentActivityIfNotDone();
        if (rec_taskPrevious != null)
        {
            rec_task.transitionToWaiting(rec_taskPrevious, null);
        }

        return rec_task;
    }

    //--//

    @Override
    public void configureContext()
    {
        loggerInstance = CustomerServiceRecord.buildContextualLogger(loggerInstance, getTargetServiceLocator());
    }

    @Override
    public String getTitle()
    {
        return String.format("Migration Host '%s'", serviceDisplayName);
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, needsSession = true)
    public void state_CreateBackup(SessionHolder sessionHolder) throws
                                                                Exception
    {
        RecordLocked<CustomerServiceRecord> lock_svc = getTargetServiceLocked(sessionHolder, 2, TimeUnit.MINUTES);
        CustomerServiceRecord               rec_svc  = lock_svc.get();

        if (loc_activity_backup == null)
        {
            CustomerServiceDesiredState desiredState = rec_svc.prepareDesiredState();
            desiredState.createBackup = BackupKind.HostMigration;
            for (CustomerServiceDesiredStateRole custRole : desiredState.roles)
            {
                switch (custRole.role)
                {
                    case hub:
                    case database:
                        custRole.shutdown = true;
                        break;
                }
            }

            BackgroundActivityRecord activity = TaskForDesiredState.scheduleTask(lock_svc, desiredState, Duration.of(3, ChronoUnit.HOURS));
            loc_activity_backup = sessionHolder.createLocator(activity);

            waitForSubActivity(loc_activity_backup, null);
        }
        else
        {
            BackgroundActivityRecord activity = sessionHolder.fromLocatorOrNull(loc_activity_backup);
            if (activity == null || activity.getStatus() != BackgroundActivityStatus.COMPLETED)
            {
                markAsFailed("Backup failed");
                return;
            }

            TaskForDesiredState h = (TaskForDesiredState) activity.getHandler(sessionHolder);
            loc_backup = h.loc_fromBackup;

            // Reschedule to persist state.
            continueAtState(State.TransferBackup);
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, needsSession = true)
    public void state_TransferBackup(SessionHolder sessionHolder) throws
                                                                  Exception
    {
        CustomerServiceBackupRecord rec_backup = sessionHolder.fromLocatorOrNull(loc_backup);
        if (rec_backup == null)
        {
            markAsFailed("Backup transfer failed");
            return;
        }

        if (rec_backup.isPendingTransfer())
        {
            rescheduleSleeping(15, TimeUnit.SECONDS);
            return;
        }

        // Reschedule to persist state.
        continueAtState(State.TerminateHost);
    }

    @BackgroundActivityMethod(stateClass = State.class, needsSession = true)
    public void state_TerminateHost(SessionHolder sessionHolder) throws
                                                                 Exception
    {
        RecordLocked<CustomerServiceRecord> lock_svc = getTargetServiceLocked(sessionHolder, 2, TimeUnit.MINUTES);
        CustomerServiceRecord               rec_svc  = lock_svc.get();

        if (loc_activities_hostTermination == null)
        {
            List<RecordLocator<BackgroundActivityRecord>> activities = Lists.newArrayList();

            List<DeploymentHostConfig> newHostsConfig = Lists.newArrayList();
            for (DeploymentHost host : DeploymentHostRecord.getHostsInService(sessionHolder, rec_svc.getSysId()))
            {
                RegistryTaggedImageRecord rec_taggedImage  = RegistryTaggedImageRecord.findMatch(sessionHolder, DeploymentRole.deployer, host.architecture, RegistryImageReleaseStatus.Release);
                DeploymentInstance        hostInstanceType = host.ensureInstanceType();

                if (rec_taggedImage != null && hostInstanceType.deployerClass != null)
                {
                    var config = new DeploymentHostConfig();
                    config.instanceType = rec_svc.getInstanceType();
                    config.roles        = host.roles.toArray(new DeploymentRole[0]);
                    config.imageId      = rec_taggedImage.getSysId();
                    newHostsConfig.add(config);

                    RecordLocked<DeploymentHostRecord> lock_host = sessionHolder.getEntityWithLock(DeploymentHostRecord.class, host.sysId, 2, TimeUnit.MINUTES);
                    BackgroundActivityRecord           activity  = TaskForHostTermination.scheduleTask(lock_host, Duration.of(2, ChronoUnit.HOURS));
                    activities.add(sessionHolder.createLocator(activity));
                }
            }

            waitForSubActivities(activities, null);
            this.loc_activities_hostTermination = activities;
            this.newHostsConfig                 = newHostsConfig;
        }
        else
        {
            for (RecordLocator<BackgroundActivityRecord> loc_activity : loc_activities_hostTermination)
            {
                BackgroundActivityRecord activity = sessionHolder.fromLocatorOrNull(loc_activity);
                if (activity == null || activity.getStatus() != BackgroundActivityStatus.COMPLETED)
                {
                    markAsFailed("Host termination failed");
                    return;
                }
            }

            // Reschedule to persist state.
            continueAtState(State.CreateHost);
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, needsSession = true)
    public void state_CreateHost(SessionHolder sessionHolder) throws
                                                              Exception
    {
        RecordLocked<CustomerServiceRecord> lock_svc = getTargetServiceLocked(sessionHolder, 2, TimeUnit.MINUTES);

        if (loc_activities_hostCreation == null)
        {
            List<RecordLocator<BackgroundActivityRecord>> activities = Lists.newArrayList();
            for (DeploymentHostConfig config : newHostsConfig)
            {
                BaseHostDeployTask.ActivityWithHost activityWithHost = TaskForHostCreation.scheduleTask(lock_svc, config, Duration.of(20, ChronoUnit.MINUTES));
                activities.add(activityWithHost.activity);
            }

            waitForSubActivities(activities, null);
            this.loc_activities_hostCreation = activities;
        }
        else
        {
            for (RecordLocator<BackgroundActivityRecord> loc_activity : loc_activities_hostCreation)
            {
                BackgroundActivityRecord activity = sessionHolder.fromLocatorOrNull(loc_activity);
                if (activity == null || activity.getStatus() != BackgroundActivityStatus.COMPLETED)
                {
                    markAsFailed("Host creation failed");
                    return;
                }
            }

            // Reschedule to persist state.
            continueAtState(State.ReloadBackup);
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, needsSession = true)
    public void state_ReloadBackup(SessionHolder sessionHolder) throws
                                                                Exception
    {
        RecordLocked<CustomerServiceRecord> lock_svc = getTargetServiceLocked(sessionHolder, 2, TimeUnit.MINUTES);
        CustomerServiceRecord               rec_svc  = lock_svc.get();

        if (loc_activity_restore == null)
        {
            CustomerServiceDesiredState desiredState = rec_svc.prepareDesiredState();
            desiredState.fromBackup = TypedRecordIdentity.newTypedInstance(loc_backup);
            for (CustomerServiceDesiredStateRole custRole : desiredState.roles)
            {
                switch (custRole.role)
                {
                    case gateway:
                        custRole.shutdownIfDifferent = true;
                        custRole.launchIfMissing = true;
                        break;

                    default:
                        custRole.shutdown = true;
                        custRole.launch = true;
                        break;
                }
            }

            BackgroundActivityRecord activity = TaskForDesiredState.scheduleTask(lock_svc, desiredState, Duration.of(3, ChronoUnit.HOURS));
            loc_activity_restore = sessionHolder.createLocator(activity);

            waitForSubActivity(loc_activity_restore, null);
        }
        else
        {
            BackgroundActivityRecord activity = sessionHolder.fromLocatorOrNull(loc_activity_restore);
            if (activity == null || activity.getStatus() != BackgroundActivityStatus.COMPLETED)
            {
                markAsFailed("Restore failed");
                return;
            }

            markAsCompleted();
        }
    }
}
