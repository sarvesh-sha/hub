/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForHub;
import com.optio3.cloud.builder.model.customer.CustomerService;
import com.optio3.cloud.builder.model.customer.CustomerServiceDesiredState;
import com.optio3.cloud.builder.model.customer.CustomerServiceDesiredStateRole;
import com.optio3.cloud.builder.model.customer.RoleAndArchitectureWithImage;
import com.optio3.cloud.builder.model.deployment.DeploymentGlobalDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentHost;
import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentTask;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.customer.BackupKind;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceBackupRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.customer.DatabaseMode;
import com.optio3.cloud.builder.persistence.customer.RoleAndArchitecture;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedImagePull;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedTaskCreation;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedTaskTermination;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.logic.BackgroundActivityStatus;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.WellKnownDockerImageLabel;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.BiConsumerWithException;
import org.apache.commons.lang3.StringUtils;

public class TaskForDesiredState extends BaseDeployTask
{
    public enum State
    {
        PullImages,
        Plan,
        StopTasksPreBackup,
        CreateBackup,
        StopTasks,
        StartTasks
    }

    public static class ImageDetails extends RoleAndArchitectureWithImage
    {
        public String imageSha;
    }

    public String                                     serviceDisplayName;
    public CustomerServiceDesiredState                desiredState;
    public List<ImageDetails>                         imagesDetails;
    public boolean                                    restoreServiceSettings;
    public boolean                                    dontStopHub;
    public boolean                                    isRollback;
    public RecordLocator<CustomerServiceBackupRecord> loc_fromBackup;
    public List<ActivityWithTask>                     subTasks;

    //--//

    public static BackgroundActivityRecord scheduleTask(RecordLocked<CustomerServiceRecord> lock_targetService,
                                                        CustomerServiceDesiredState desiredState,
                                                        Duration timeout) throws
                                                                          Exception
    {
        Exceptions.requireNotNull(lock_targetService, InvalidArgumentException.class, "No customer service provided");

        SessionHolder         sessionHolder = lock_targetService.getSessionHolder();
        CustomerServiceRecord targetService = lock_targetService.get();

        //
        // Make sure all the records are good before starting the activity.
        //
        CustomerServiceBackupRecord rec_fromBackup = sessionHolder.fromIdentity(desiredState.fromBackup);

        //
        // Resolve the spec to actual record, to make sure we have a good configuration.
        //
        List<ImageDetails> imagesDetails = Lists.newArrayList();
        for (CustomerServiceDesiredStateRole stateRole : desiredState.roles)
        {
            final RegistryTaggedImageRecord rec_taggedImage = sessionHolder.fromIdentity(stateRole.image);

            ImageDetails imageDetails = new ImageDetails();
            imageDetails.role         = stateRole.role;
            imageDetails.architecture = stateRole.architecture;
            imageDetails.image        = TypedRecordIdentity.newTypedInstance(rec_taggedImage);
            imageDetails.imageSha     = rec_taggedImage != null ? rec_taggedImage.getImageSha() : null;
            imagesDetails.add(imageDetails);
        }

        BackgroundActivityRecord rec_task = BaseDeployTask.scheduleActivity(lock_targetService, TaskForDesiredState.class, (t) ->
        {
            CustomerRecord rec_cust           = targetService.getCustomer();
            String         serviceDisplayName = String.format("%s / %s", rec_cust.getName(), targetService.getName());

            t.loggerInstance.info("Creating desired state for '%s'", serviceDisplayName);
            t.initializeTimeout(timeout);

            t.serviceDisplayName     = serviceDisplayName;
            t.isRollback             = rec_fromBackup != null;
            t.loc_fromBackup         = sessionHolder.createLocator(rec_fromBackup);
            t.restoreServiceSettings = t.loc_fromBackup != null;
            t.desiredState           = desiredState;

            t.imagesDetails = imagesDetails;
        });

        BackgroundActivityRecord rec_taskPrevious = targetService.getCurrentActivityIfNotDone();
        if (rec_taskPrevious != null)
        {
            rec_task.transitionToWaiting(rec_taskPrevious, null);
        }

        targetService.setCurrentActivity(rec_task);

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
        return String.format("Desired Service State for '%s'", serviceDisplayName);
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, needsSession = true)
    public void state_PullImages(SessionHolder sessionHolder) throws
                                                              Exception
    {
        CustomerServiceRecord rec_svc  = getTargetService(sessionHolder);
        CustomerRecord        rec_cust = rec_svc.getCustomer();

        loggerInstance.info("Desired state processing for '%s / %s': state %s", rec_cust.getName(), rec_svc.getName(), stateMachine);

        if (subTasks == null)
        {
            subTasks = enumeratePotentialRoles(sessionHolder, rec_svc, false, (globalDescriptor, roleAndArchitecture, host) ->
            {
                DeploymentOperationalStatus operationalStatus = host.operationalStatus;
                if (!operationalStatus.acceptsNewTasks())
                {
                    // Don't start image pulls on hosts that are not ready to run them.
                    return null;
                }

                final CustomerServiceDesiredStateRole roleSpec = desiredState.lookup(roleAndArchitecture);
                if (roleSpec != null)
                {
                    RegistryTaggedImageRecord rec_taggedImage = getImageForRole(sessionHolder, roleAndArchitecture);
                    boolean                   shouldLaunch    = false;

                    if (roleSpec.launch)
                    {
                        shouldLaunch = true;
                    }
                    else if (!host.collectTasksForRole(null, DeploymentStatus.Ready, roleSpec.role, rec_taggedImage.getImageSha(), true))
                    {
                        if (roleSpec.launchIfMissing || roleSpec.launchIfMissingAndIdle)
                        {
                            shouldLaunch = true;
                        }
                    }

                    if (shouldLaunch)
                    {
                        RecordLocked<DeploymentHostRecord> lock_host = sessionHolder.getEntityWithLock(DeploymentHostRecord.class, host.sysId, 2, TimeUnit.MINUTES);

                        if (shouldWaitForResults(roleAndArchitecture))
                        {
                            BackgroundActivityRecord activity = TaskForImagePull.scheduleTask(lock_host, rec_taggedImage, false);

                            ActivityWithTask res = new ActivityWithTask();
                            res.activity = sessionHolder.createLocator(activity);
                            return res;
                        }
                        else
                        {
                            DelayedImagePull.queue(lock_host, rec_taggedImage, null);
                        }
                    }
                }

                return null;
            });
        }

        checkSubTasks(sessionHolder, null, (activity, rec_task) ->
        {
            TaskForImagePull handler = (TaskForImagePull) activity.getHandler(sessionHolder);

            RegistryTaggedImageRecord rec_taggedImage = sessionHolder.fromLocator(handler.loc_image);
            DeploymentRole            targetService   = rec_taggedImage.getTargetService();
            if (targetService != null && targetService.asyncImagePull)
            {
                //
                // Okay to fail an async task, we can launch it later.
                //
                RecordLocked<DeploymentHostRecord> lock_host = handler.getTargetHost(sessionHolder, 2, TimeUnit.MINUTES);
                DelayedImagePull.queue(lock_host, rec_taggedImage, null);
                return;
            }

            throw Exceptions.newRuntimeException("Desired state processing for '%s / %s' failed to pull image '%s'", rec_cust.getName(), rec_svc.getName(), rec_taggedImage.getTag());
        });

        //
        // Only give tasks 20 minutes to complete download.
        // Late tasks will be moved to queued desired state.
        //
        if (trackSubTasksIfNotDone(sessionHolder, TimeUtils.future(20, TimeUnit.MINUTES)))
        {
            return;
        }

        // Reschedule to persist state.
        continueAtState(State.Plan);
    }

    @BackgroundActivityMethod(stateClass = State.class, needsSession = true)
    public void state_Plan(SessionHolder sessionHolder) throws
                                                        Exception
    {
        if (desiredState.createBackup == BackupKind.HostMigration)
        {
            dontStopHub = false; // Stop hub and recreate database.
        }
        else if (desiredState.createBackup != null)
        {
            CustomerServiceRecord rec_svc = getTargetService(sessionHolder);

            switch (rec_svc.getDbMode())
            {
                case H2OnDisk:
                    dontStopHub = true;
                    break;

                case MariaDB:
                {
                    List<RoleAndArchitectureWithImage> currentImages = rec_svc.getRoleImages();

                    RegistryTaggedImageRecord img_current_hub      = null;
                    RegistryTaggedImageRecord img_current_database = null;

                    RegistryTaggedImageRecord       img_expected_hub      = null;
                    RegistryTaggedImageRecord       img_expected_database = null;
                    CustomerServiceDesiredStateRole role_database         = null;

                    for (RoleAndArchitectureWithImage currentImage : currentImages)
                    {
                        switch (currentImage.role)
                        {
                            case database:
                                img_current_database = sessionHolder.fromIdentityOrNull(currentImage.image);
                                break;

                            case hub:
                                img_current_hub = sessionHolder.fromIdentityOrNull(currentImage.image);
                                break;
                        }
                    }

                    for (CustomerServiceDesiredStateRole role : desiredState.roles)
                    {
                        switch (role.role)
                        {
                            case database:
                                role_database = role;
                                img_expected_database = sessionHolder.fromIdentity(role.image);
                                break;

                            case hub:
                                img_expected_hub = sessionHolder.fromIdentity(role.image);
                                break;
                        }
                    }

                    if (role_database != null && img_current_database == img_expected_database)
                    {
                        //
                        // 1) Same database version.
                        //
                        if (img_current_hub != null && img_expected_hub != null)
                        {
                            String schemaCurrent  = img_current_hub.findLabelOrDefault(WellKnownDockerImageLabel.DatabaseSchema, null);
                            String schemaExpected = img_expected_hub.findLabelOrDefault(WellKnownDockerImageLabel.DatabaseSchema, null);

                            if (schemaCurrent != null && StringUtils.equals(schemaCurrent, schemaExpected))
                            {
                                //
                                // 2) Same database schema => we can leave the database running and don't need to stop the Hub before a backup.
                                //
                                role_database.resetFlags();
                                role_database.launchIfMissing = true;
                                dontStopHub                   = true;
                            }
                            else
                            {
                                loggerInstance.info("Can't bypass database shutdown, different schemas: %s != %s", schemaCurrent, schemaExpected);
                            }
                        }
                    }
                    else
                    {
                        loggerInstance.info("Can't bypass database shutdown, different database images");
                    }
                }
                break;
            }
        }
        else
        {
            dontStopHub = true;
        }

        continueAtState(State.StopTasksPreBackup);
    }

    @BackgroundActivityMethod(stateClass = State.class, needsSession = true)
    public void state_StopTasksPreBackup(SessionHolder sessionHolder) throws
                                                                      Exception
    {
        if (subTasks == null && !dontStopHub)
        {
            CustomerServiceRecord rec_svc = getTargetService(sessionHolder);

            subTasks = enumerateExistingRoles(sessionHolder, rec_svc, DeploymentStatus.Ready, true, (globalDescriptor, roleAndArchitecture, host, task) ->
            {
                if (roleAndArchitecture.getRole() == DeploymentRole.hub)
                {
                    int waitForShutdown;

                    try
                    {
                        DeployLogicForHub hubLogic = getLogicForHub();
                        hubLogic.login(false);

                        com.optio3.cloud.client.hub.api.AdminTasksApi adminProxy = hubLogic.createHubProxy(com.optio3.cloud.client.hub.api.AdminTasksApi.class);
                        adminProxy.shutdown();

                        waitForShutdown = 60;
                    }
                    catch (Throwable t)
                    {
                        // If this fails, it's because the site has not been upgraded to have the correct Admin API.
                        waitForShutdown = -1;
                    }

                    DeploymentTaskRecord rec_task = sessionHolder.getEntityOrNull(DeploymentTaskRecord.class, task.sysId);
                    if (rec_task != null)
                    {
                        BackgroundActivityRecord activity = TaskForContainerTermination.scheduleTask(sessionHolder, rec_task, waitForShutdown);

                        ActivityWithTask res = new ActivityWithTask();
                        res.activity = sessionHolder.createLocator(activity);
                        res.task     = sessionHolder.createLocator(rec_task);

                        return res;
                    }
                }

                return null;
            });
        }

        checkSubTasks(sessionHolder, null, (activity, rec_task) ->
        {
            if (rec_task != null && rec_task.getStatus() == DeploymentStatus.Ready)
            {
                CustomerServiceRecord rec_svc  = getTargetService(sessionHolder);
                CustomerRecord        rec_cust = rec_svc.getCustomer();

                throw Exceptions.newRuntimeException("Desired state processing for '%s / %s' failed to stop task '%s'", rec_cust.getName(), rec_svc.getName(), rec_task.getImage());
            }
        });

        //
        // Only give tasks 5 minutes to terminate.
        // Late tasks will be moved to queued desired state.
        //
        if (trackSubTasksIfNotDone(sessionHolder, TimeUtils.future(5, TimeUnit.MINUTES)))
        {
            return;
        }

        continueAtState(State.CreateBackup);
    }

    @BackgroundActivityMethod(stateClass = State.class, needsSession = true)
    public void state_CreateBackup(SessionHolder sessionHolder) throws
                                                                Exception
    {
        CustomerServiceRecord rec_svc = getTargetService(sessionHolder);

        if (subTasks == null && desiredState.createBackup != null)
        {
            RecordHelper<DeploymentTaskRecord> helper = sessionHolder.createHelper(DeploymentTaskRecord.class);
            helper.lockTableUntilEndOfTransaction(10, TimeUnit.MINUTES);

            ActivityWithTask activityWithTask = null;

            switch (rec_svc.getDbMode())
            {
                case H2OnDisk:
                {
                    activityWithTask = TaskForHubBackup.scheduleTask(sessionHolder, rec_svc, desiredState.createBackup, Duration.of(30, ChronoUnit.MINUTES));
                    break;
                }

                case MariaDB:
                {
                    DeploymentTaskRecord rec_task = rec_svc.findAnyTaskForRole(sessionHolder, DeploymentStatus.Ready, DeploymentRole.database);
                    if (rec_task != null)
                    {
                        activityWithTask = TaskForMariaDbBackup.scheduleTask(sessionHolder, rec_task, desiredState.createBackup, Duration.of(3, ChronoUnit.HOURS));
                    }

                    break;
                }
            }

            if (activityWithTask != null)
            {
                this.subTasks = Lists.newArrayList(activityWithTask);
            }
        }

        checkSubTasks(sessionHolder, (activity, rec_task) ->
        {
            switch (rec_svc.getDbMode())
            {
                case H2OnDisk:
                {
                    TaskForHubBackup h = (TaskForHubBackup) activity.getHandler(sessionHolder);
                    loc_fromBackup = h.loc_backup;
                    break;
                }

                case MariaDB:
                {
                    TaskForMariaDbBackup h = (TaskForMariaDbBackup) activity.getHandler(sessionHolder);
                    loc_fromBackup = h.loc_backup;
                    break;
                }
            }
        }, (activity, rec_task) ->
                      {
                          CustomerRecord rec_cust = rec_svc.getCustomer();

                          throw Exceptions.newRuntimeException("Desired state processing for '%s / %s' failed to create a backup", rec_cust.getName(), rec_svc.getName());
                      });

        if (trackSubTasksIfNotDone(sessionHolder, null))
        {
            return;
        }

        continueAtState(State.StopTasks);
    }

    @BackgroundActivityMethod(stateClass = State.class, needsSession = true, autoRetry = true, maxRetries = 10)
    public void state_StopTasks(SessionHolder sessionHolder) throws
                                                             Exception
    {
        CustomerServiceRecord rec_svc = getTargetService(sessionHolder);

        for (boolean checkAgain = true; checkAgain; )
        {
            if (subTasks == null)
            {
                subTasks = enumerateExistingRoles(sessionHolder, rec_svc, null, true, (globalDescriptor, roleAndArchitecture, host, task) ->
                {
                    CustomerServiceDesiredStateRole roleSpec = desiredState.lookup(roleAndArchitecture);
                    boolean                         shutdown = false;

                    //
                    // Only shutdown tasks if:
                    //
                    if (roleSpec == null)
                    {
                        // 1) They are not part of the spec.
                        shutdown = true;
                    }
                    else
                    {
                        if (roleSpec.shutdown)
                        {
                            // 2) They are explicitly listed for shutdown.
                            shutdown = true;
                        }
                        else if (roleSpec.shutdownIfDifferent)
                        {
                            // 3) The image doesn't match the expected one.
                            ImageDetails imageDetails = findImageDetails(roleAndArchitecture);
                            shutdown = imageDetails == null || !StringUtils.equals(task.image, imageDetails.imageSha);
                        }
                    }

                    if (shutdown)
                    {
                        DeploymentTaskRecord rec_task = sessionHolder.getEntityOrNull(DeploymentTaskRecord.class, task.sysId);
                        if (rec_task != null)
                        {
                            if (shouldWaitForResults(roleAndArchitecture))
                            {
                                BackgroundActivityRecord activity = TaskForContainerTermination.scheduleTask(sessionHolder, rec_task, -1);

                                ActivityWithTask res = new ActivityWithTask();
                                res.activity = sessionHolder.createLocator(activity);
                                res.task     = sessionHolder.createLocator(rec_task);

                                return res;
                            }
                            else
                            {
                                DelayedTaskTermination.queue(sessionHolder, rec_task, false);
                            }
                        }
                    }

                    return null;
                });

                if (subTasks == null)
                {
                    checkAgain = false;
                }
            }

            checkSubTasks(sessionHolder, null, (activity, rec_task) ->
            {
                if (rec_task != null && rec_task.getStatus() == DeploymentStatus.Ready)
                {
                    DeploymentRole role = rec_task.getRole();
                    if (role != null && role.asyncImagePull)
                    {
                        //
                        // Okay to fail an async task, we can launch it later.
                        //
                        DelayedTaskTermination.queue(sessionHolder, rec_task, false);
                        return;
                    }

                    CustomerRecord rec_cust = rec_svc.getCustomer();

                    throw Exceptions.newRuntimeException("Desired state processing for '%s / %s' failed to stop task '%s'", rec_cust.getName(), rec_svc.getName(), rec_task.getImage());
                }
            });

            //
            // Only give tasks 5 minutes to terminate.
            // Late tasks will be moved to queued desired state.
            //
            if (trackSubTasksIfNotDone(sessionHolder, TimeUtils.future(5, TimeUnit.MINUTES)))
            {
                return;
            }
        }

        continueAtState(State.StartTasks);
    }

    @BackgroundActivityMethod(stateClass = State.class, needsSession = true, autoRetry = true, maxRetries = 10)
    public void state_StartTasks(SessionHolder sessionHolder) throws
                                                              Exception
    {
        CustomerServiceRecord rec_svc = getTargetService(sessionHolder);

        for (boolean checkAgain = true; checkAgain; )
        {
            if (subTasks == null)
            {
                RecordHelper<DeploymentTaskRecord> helper = sessionHolder.createHelper(DeploymentTaskRecord.class);
                helper.lockTableUntilEndOfTransaction(10, TimeUnit.MINUTES);

                subTasks = enumeratePotentialRolesWithNoTask(sessionHolder, rec_svc, (globalDescriptor, roleAndArchitecture, host) ->
                {
                    CustomerServiceDesiredStateRole roleSpec = desiredState.lookup(roleAndArchitecture);
                    if (roleSpec != null)
                    {
                        final DeploymentRole      role            = roleAndArchitecture.getRole();
                        RegistryTaggedImageRecord rec_taggedImage = getImageForRole(sessionHolder, roleAndArchitecture);
                        boolean                   shouldLaunch    = false;

                        if (roleSpec.launch)
                        {
                            shouldLaunch = true;
                        }
                        else if (!host.collectTasksForRole(null, DeploymentStatus.Ready, roleSpec.role, rec_taggedImage.getImageSha(), true))
                        {
                            if (roleSpec.launchIfMissing)
                            {
                                shouldLaunch = true;
                            }
                            else if (roleSpec.launchIfMissingAndIdle && !host.delayedOperations)
                            {
                                BuilderApplication.LoggerInstance.warn("Host '%s' not running expected task for role '%s', starting...", host.computeDisplayName(), roleSpec.role);
                                shouldLaunch = true;
                            }
                        }

                        if (shouldLaunch)
                        {
                            RecordLocked<DeploymentHostRecord> lock_host = sessionHolder.getEntityWithLock(DeploymentHostRecord.class, host.sysId, 2, TimeUnit.MINUTES);

                            if (shouldWaitForResults(roleAndArchitecture))
                            {
                                switch (role)
                                {
                                    case database:
                                    {
                                        CustomerServiceBackupRecord rec_backup = rec_svc.getDbMode() == DatabaseMode.MariaDB ? sessionHolder.fromLocator(loc_fromBackup) : null;

                                        return TaskForMariaDbCreation.scheduleTask(sessionHolder,
                                                                                   lock_host.get(),
                                                                                   rec_taggedImage,
                                                                                   rec_backup,
                                                                                   restoreServiceSettings,
                                                                                   Duration.of(2, ChronoUnit.HOURS));
                                    }

                                    case hub:
                                    {
                                        CustomerServiceBackupRecord rec_backup = rec_svc.getDbMode() == DatabaseMode.H2OnDisk ? sessionHolder.fromLocator(loc_fromBackup) : null;

                                        return TaskForHubCreation.scheduleTask(lock_host, rec_taggedImage, rec_backup, restoreServiceSettings, Duration.of(3, ChronoUnit.HOURS));
                                    }

                                    case gateway:
                                    {
                                        return TaskForGatewayCreation.scheduleTask(lock_host, null, rec_taggedImage);
                                    }

                                    case reporter:
                                    {
                                        return TaskForReporterCreation.scheduleTask(lock_host, null, rec_taggedImage);
                                    }

                                    default:
                                    {
                                        CustomerRecord rec_cust = rec_svc.getCustomer();

                                        throw Exceptions.newRuntimeException("Desired state processing for '%s / %s' failed due to unexpected role '%s'",
                                                                             rec_cust.getName(),
                                                                             rec_svc.getName(),
                                                                             roleAndArchitecture);
                                    }
                                }
                            }
                            else
                            {
                                DelayedTaskCreation.queue(lock_host, role, rec_taggedImage, null);
                            }
                        }
                    }

                    return null;
                });

                if (subTasks == null)
                {
                    checkAgain = false;
                }
            }

            checkSubTasks(sessionHolder, null, (activity, rec_task) ->
            {
                if (rec_task != null)
                {
                    TaskForGatewayCreation handler = Reflection.as(activity.getHandler(sessionHolder), TaskForGatewayCreation.class);
                    if (handler != null)
                    {
                        //
                        // Okay to fail a Gateway task, we can launch it later.
                        //
                        DeploymentHostRecord               rec_host  = rec_task.getDeployment();
                        RecordLocked<DeploymentHostRecord> lock_host = sessionHolder.optimisticallyUpgradeToLocked(rec_host, 2, TimeUnit.MINUTES);

                        DelayedTaskCreation.queue(lock_host, DeploymentRole.gateway, sessionHolder.fromLocator(handler.loc_image), null);
                        return;
                    }

                    CustomerRecord rec_cust = rec_svc.getCustomer();

                    throw Exceptions.newRuntimeException("Desired state processing for '%s / %s' failed to start task '%s'", rec_cust.getName(), rec_svc.getName(), rec_task.getImage());
                }
            });

            //
            // Only give tasks 5 minutes to start.
            // Late tasks will be moved to queued desired state.
            //
            if (trackSubTasksIfNotDone(sessionHolder, TimeUtils.future(5, TimeUnit.MINUTES)))
            {
                return;
            }
        }

        //
        // Update the image configuration based on the running tasks.
        //
        List<RoleAndArchitectureWithImage> currentImages = Lists.newArrayList();

        for (ImageDetails imagesDetail : imagesDetails)
        {
            RoleAndArchitectureWithImage.add(currentImages, imagesDetail.role, imagesDetail.architecture, imagesDetail.image);
        }

        rec_svc.setRoleImages(currentImages);

        String roleOrigin = null;

        if (isRollback)
        {
            CustomerServiceBackupRecord rec_backup = sessionHolder.fromLocatorOrNull(loc_fromBackup);
            if (rec_backup != null && rec_backup.getTrigger() != BackupKind.HostMigration)
            {
                CustomerServiceRecord rec_svcBackup = rec_backup.getCustomerService();
                if (!SessionHolder.sameEntity(rec_svc, rec_svcBackup))
                {
                    roleOrigin = String.format("Cross-Service Origin: %s at %s", rec_svcBackup.getDisplayName(), rec_backup.getFileId());
                }
                else
                {
                    roleOrigin = String.format("Rollback from %s", rec_backup.getFileId());
                }
            }
        }

        rec_svc.setRoleOrigin(roleOrigin);

        //
        // Copy settings to the active side.
        //
        rec_svc.setExtraConfigLinesActive(rec_svc.getExtraConfigLines());

        //
        // Reset certificate failure flag, to notify the UI.
        //
        rec_svc.putMetadata(CustomerServiceRecord.WellKnownMetadata.certificateFailure, null);

        markAsCompleted();
    }

    private ImageDetails findImageDetails(RoleAndArchitecture roleAndArchitecture)
    {
        return RoleAndArchitectureWithImage.locate(imagesDetails, roleAndArchitecture.getRole(), roleAndArchitecture.getArchitecture());
    }

    private boolean trackSubTasksIfNotDone(SessionHolder sessionHolder,
                                           ZonedDateTime forcedWakeup) throws
                                                                       Exception
    {
        if (subTasks != null)
        {
            List<RecordLocator<BackgroundActivityRecord>> subActivities = Lists.newArrayList();

            for (ActivityWithTask subTask : subTasks)
            {
                BackgroundActivityRecord subActivity = sessionHolder.fromLocator(subTask.activity);

                BackgroundActivityStatus status = subActivity.getStatus();
                if (!status.isDone())
                {
                    subActivities.add(subTask.activity);
                }
            }

            if (!subActivities.isEmpty())
            {
                waitForSubActivities(subActivities, forcedWakeup);
                return true;
            }
        }

        return false;
    }

    private void checkSubTasks(SessionHolder sessionHolder,
                               BiConsumerWithException<BackgroundActivityRecord, DeploymentTaskRecord> callbackSuccess,
                               BiConsumerWithException<BackgroundActivityRecord, DeploymentTaskRecord> callbackFailure) throws
                                                                                                                        Exception
    {
        if (subTasks != null)
        {
            boolean tasksStillActive = false;

            for (ActivityWithTask subTask : subTasks)
            {
                BackgroundActivityRecord subActivity = sessionHolder.fromLocatorOrNull(subTask.activity);
                if (subActivity == null)
                {
                    continue;
                }

                BackgroundActivityStatus status = subActivity.getStatus();

                if (status.isDone())
                {
                    if (status != BackgroundActivityStatus.COMPLETED)
                    {
                        if (callbackFailure != null)
                        {
                            callbackFailure.accept(subActivity, sessionHolder.fromLocatorOrNull(subTask.task));
                        }
                    }
                    else
                    {
                        if (callbackSuccess != null)
                        {
                            callbackSuccess.accept(subActivity, sessionHolder.fromLocatorOrNull(subTask.task));
                        }
                    }
                }
                else
                {
                    tasksStillActive = true;
                }
            }

            if (!tasksStillActive)
            {
                subTasks = null;
            }
        }
    }

    private boolean shouldWaitForResults(RoleAndArchitecture roleAndArchitecture)
    {
        return !roleAndArchitecture.getRole().asyncImagePull;
    }

    //--//

    private static DeploymentRole[] s_orderForExistingRoles = new DeploymentRole[] { DeploymentRole.reporter, DeploymentRole.gateway, DeploymentRole.hub, DeploymentRole.database };

    @FunctionalInterface
    interface ExistingRolesCallback
    {
        ActivityWithTask apply(DeploymentGlobalDescriptor globalDescriptor,
                               RoleAndArchitecture roleAndArchitecture,
                               DeploymentHost host,
                               DeploymentTask task) throws
                                                    Exception;
    }

    private List<ActivityWithTask> enumerateExistingRoles(SessionHolder sessionHolder,
                                                          CustomerServiceRecord rec_svc,
                                                          DeploymentStatus desiredState,
                                                          boolean stopAtFirstRoleWithSubtasks,
                                                          ExistingRolesCallback callback) throws
                                                                                          Exception
    {
        DeploymentGlobalDescriptor.Settings settings = new DeploymentGlobalDescriptor.Settings();
        settings.loadImages      = true;
        settings.loadDeployments = true;
        settings.loadServices    = true;

        DeploymentGlobalDescriptor globalDescriptor = DeploymentGlobalDescriptor.get(sessionHolder, settings);
        CustomerService            svc              = globalDescriptor.getService(rec_svc);

        List<ActivityWithTask> lst = null;

        for (DeploymentRole role : s_orderForExistingRoles)
        {
            for (DeploymentHost host : svc.findHostsForRole(DeploymentStatus.Ready, role))
            {
                if (!host.hasRole(role))
                {
                    continue;
                }

                for (DeploymentTask task : host.getTasksForRole(desiredState, role, null, true))
                {
                    ActivityWithTask res = callback.apply(globalDescriptor, new RoleAndArchitecture(role, host.architecture), host, task);
                    if (res != null)
                    {
                        if (lst == null)
                        {
                            lst = Lists.newArrayList();
                        }

                        lst.add(res);
                    }
                }

                if (stopAtFirstRoleWithSubtasks && lst != null)
                {
                    return lst;
                }
            }
        }

        return lst;
    }

    //--//

    private static final DeploymentRole[] s_orderForPotentialRoles = new DeploymentRole[] { DeploymentRole.database, DeploymentRole.hub, DeploymentRole.gateway, DeploymentRole.reporter };

    @FunctionalInterface
    interface PotentialRolesCallback
    {
        ActivityWithTask apply(DeploymentGlobalDescriptor globalDescriptor,
                               RoleAndArchitecture roleAndArchitecture,
                               DeploymentHost host) throws
                                                    Exception;
    }

    private List<ActivityWithTask> enumeratePotentialRoles(SessionHolder sessionHolder,
                                                           CustomerServiceRecord rec_svc,
                                                           boolean stopAtFirstRoleWithSubtasks,
                                                           PotentialRolesCallback callback) throws
                                                                                            Exception
    {
        DeploymentGlobalDescriptor.Settings settings = new DeploymentGlobalDescriptor.Settings();
        settings.loadImages      = true;
        settings.loadDeployments = true;
        settings.loadServices    = true;

        DeploymentGlobalDescriptor globalDescriptor = DeploymentGlobalDescriptor.get(sessionHolder, settings);
        CustomerService            svc              = globalDescriptor.getService(rec_svc);

        List<ActivityWithTask> lst = null;

        for (DeploymentRole role : s_orderForPotentialRoles)
        {
            for (DeploymentHost host : svc.findHostsForRole(DeploymentStatus.Ready, role))
            {
                MetadataMap hostMetadata = host.decodeMetadata();

                DeploymentInstance instanceType = DeploymentHostRecord.WellKnownMetadata.instanceType.get(hostMetadata);
                if (!instanceType.hasAgent)
                {
                    continue;
                }

                ActivityWithTask res = callback.apply(globalDescriptor, new RoleAndArchitecture(role, host.architecture), host);
                if (res != null)
                {
                    if (lst == null)
                    {
                        lst = Lists.newArrayList();
                    }

                    lst.add(res);
                }

                if (stopAtFirstRoleWithSubtasks && lst != null)
                {
                    return lst;
                }
            }
        }

        return lst;
    }

    private List<ActivityWithTask> enumeratePotentialRolesWithNoTask(SessionHolder sessionHolder,
                                                                     CustomerServiceRecord rec_svc,
                                                                     PotentialRolesCallback callback) throws
                                                                                                      Exception
    {
        return enumeratePotentialRoles(sessionHolder, rec_svc, true, (globalDescriptor, roleAndArchitecture, host) ->
        {
            DeploymentOperationalStatus status = host.operationalStatus;
            if (status.acceptsNewTasks() && host.architecture == roleAndArchitecture.getArchitecture())
            {
                RegistryTaggedImageRecord rec_taggedImage = getImageForRole(sessionHolder, roleAndArchitecture);
                if (rec_taggedImage != null)
                {
                    if (!host.collectTasksForRole(null, DeploymentStatus.Ready, roleAndArchitecture.getRole(), rec_taggedImage.getImageSha(), true))
                    {
                        return callback.apply(globalDescriptor, roleAndArchitecture, host);
                    }
                }
            }

            return null;
        });
    }

    //--//

    private RegistryTaggedImageRecord getImageForRole(SessionHolder sessionHolder,
                                                      RoleAndArchitecture roleAndArchitecture)
    {
        ImageDetails imageDetails = findImageDetails(roleAndArchitecture);
        return imageDetails != null ? sessionHolder.fromIdentityOrNull(imageDetails.image) : null;
    }
}
