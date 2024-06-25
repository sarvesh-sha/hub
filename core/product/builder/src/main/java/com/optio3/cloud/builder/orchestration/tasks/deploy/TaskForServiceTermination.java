/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.logic.BackgroundActivityStatus;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.Exceptions;

public class TaskForServiceTermination extends BaseDeployTask
{
    public enum State
    {
        TerminateCloudInstances,
        TerminateGateways
    }

    public String                                  serviceDisplayName;
    public RecordLocator<BackgroundActivityRecord> loc_activity_hostTermination;

    public static BackgroundActivityRecord scheduleTask(RecordLocked<CustomerServiceRecord> lock_targetService,
                                                        Duration timeout) throws
                                                                          Exception
    {
        Exceptions.requireNotNull(lock_targetService, InvalidArgumentException.class, "No customer service provided");

        CustomerServiceRecord targetService = lock_targetService.get();

        BackgroundActivityRecord rec_task = BaseDeployTask.scheduleActivity(lock_targetService, TaskForServiceTermination.class, (t) ->
        {
            CustomerRecord rec_cust = targetService.getCustomer();
            t.serviceDisplayName = String.format("%s / %s", rec_cust.getName(), targetService.getName());

            t.loggerInstance.info("Terminating service '%s'", t.serviceDisplayName);
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
        return String.format("Terminate Service '%s'", serviceDisplayName);
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, needsSession = true)
    public void state_TerminateCloudInstances(SessionHolder sessionHolder) throws
                                                                           Exception
    {
        RecordLocked<CustomerServiceRecord> lock_svc = getTargetServiceLocked(sessionHolder, 2, TimeUnit.MINUTES);
        CustomerServiceRecord               rec_svc  = lock_svc.get();

        if (loc_activity_hostTermination != null)
        {
            BackgroundActivityRecord activity = sessionHolder.fromLocatorOrNull(loc_activity_hostTermination);
            if (activity == null || activity.getStatus() != BackgroundActivityStatus.COMPLETED)
            {
                markAsFailed("Host termination failed");
                return;
            }

            loc_activity_hostTermination = null;
        }

        for (var desc : DeploymentHostRecord.describe(sessionHolder.createHelper(DeploymentHostRecord.class), false, rec_svc.getSysId()))
        {
            if (desc.status == DeploymentStatus.Ready && desc.operationalStatus == DeploymentOperationalStatus.operational)
            {
                if (desc.hasAnyRoles(DeploymentRole.hub, DeploymentRole.database))
                {
                    RecordLocked<DeploymentHostRecord> lock_host = sessionHolder.getEntityWithLock(DeploymentHostRecord.class, desc.ri.sysId, 2, TimeUnit.MINUTES);
                    BackgroundActivityRecord           activity  = TaskForHostTermination.scheduleTask(lock_host, Duration.of(2, ChronoUnit.HOURS));
                    loc_activity_hostTermination = sessionHolder.createLocator(activity);
                    waitForSubActivity(loc_activity_hostTermination, null);
                    return;
                }
            }
        }

        // Reschedule to persist state.
        continueAtState(State.TerminateGateways);
    }

    @BackgroundActivityMethod(stateClass = State.class, needsSession = true)
    public void state_TerminateGateways(SessionHolder sessionHolder) throws
                                                                     Exception
    {
        RecordLocked<CustomerServiceRecord> lock_svc = getTargetServiceLocked(sessionHolder, 2, TimeUnit.MINUTES);
        CustomerServiceRecord               rec_svc  = lock_svc.get();

        RecordHelper<DeploymentHostRecord> helper = sessionHolder.createHelper(DeploymentHostRecord.class);
        for (var desc : DeploymentHostRecord.describe(helper, false, rec_svc.getSysId()))
        {
            if (desc.status == DeploymentStatus.Ready && desc.operationalStatus != DeploymentOperationalStatus.retired)
            {
                if (desc.hasAnyRoles(DeploymentRole.gateway))
                {
                    RecordLocked<DeploymentHostRecord> lock_host = sessionHolder.getEntityWithLock(DeploymentHostRecord.class, desc.ri.sysId, 2, TimeUnit.MINUTES);
                    DeploymentHostRecord               rec_host  = lock_host.get();

                    try (ValidationResultsHolder validation = new ValidationResultsHolder(sessionHolder, false, false))
                    {
                        rec_host.setOperationalStatus(DeploymentOperationalStatus.retired);
                        rec_host.cleanupState(validation, helper);

                        // Reschedule to persist the state.
                        rescheduleDelayed(100, TimeUnit.MILLISECONDS);
                        return;
                    }
                }
            }
        }

        markAsCompleted();
    }
}
