/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment.delayedOps;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Objects;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForHub;
import com.optio3.cloud.builder.model.deployment.DeploymentHostOnlineSession;
import com.optio3.cloud.builder.model.deployment.DeploymentHostOnlineSessions;
import com.optio3.cloud.builder.model.deployment.DeploymentHostServiceDetails;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForContainerTermination;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DelayedOperation;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryImageRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.hub.model.GatewayQueueStatus;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.logging.ILogger;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;
import com.optio3.util.TimeUtils;

@JsonTypeName("DelayedTaskTermination")
public class DelayedTaskTermination extends DelayedOperation
{
    public RecordLocator<DeploymentTaskRecord> loc_task;
    public boolean                             ignoreOfflineDelay;

    //--//

    public static boolean queue(SessionHolder sessionHolder,
                                DeploymentTaskRecord rec_task,
                                boolean ignoreOfflineDelay) throws
                                                            Exception
    {
        DeploymentHostRecord               rec_target  = rec_task.getDeployment();
        RecordLocked<DeploymentHostRecord> lock_target = sessionHolder.optimisticallyUpgradeToLocked(rec_target, 2, TimeUnit.MINUTES);

        DelayedTaskTermination op = new DelayedTaskTermination();
        op.loc_task           = sessionHolder.createLocator(rec_task);
        op.ignoreOfflineDelay = ignoreOfflineDelay;

        return DeploymentHostRecord.queueUnique(lock_target, op);
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        DelayedTaskTermination that = Reflection.as(o, DelayedTaskTermination.class);
        if (that == null)
        {
            return false;
        }

        return Objects.equal(loc_task, that.loc_task);
    }

    @Override
    public boolean mightRequireImagePull()
    {
        return false;
    }

    @Override
    public boolean validate(SessionHolder sessionHolder)
    {
        DeploymentTaskRecord rec_task = sessionHolder.fromLocatorOrNull(loc_task);
        return rec_task != null && rec_task.getDockerId() != null;
    }

    @Override
    public String getSummary(SessionHolder sessionHolder)
    {
        DeploymentTaskRecord rec_task = sessionHolder.fromLocatorOrNull(loc_task);

        RegistryImageRecord rec_image = rec_task.getImageReference();
        if (rec_image != null)
        {
            for (RegistryTaggedImageRecord rec_taggedImage : rec_image.getReferencingTags())
            {
                return String.format("Terminate task '%s/%s'", rec_task.getDockerId(), rec_taggedImage.getTag());
            }
        }

        return String.format("Terminate task '%s'", rec_task.getDockerId());
    }

    @Override
    public NextAction process() throws
                                Exception
    {
        NextAction nextAction = shouldSleep(Math.min(retries, 30));
        if (nextAction != null)
        {
            return nextAction;
        }

        DeploymentTaskRecord rec_task   = sessionHolder.fromLocator(loc_task);
        DeploymentHostRecord targetHost = lock_targetHost.get();

        if (!ignoreOfflineDelay)
        {
            DeploymentRole role = rec_task.getRole();
            if (!targetHost.hasRole(role))
            {
                // No matching role, we can terminate the task immediately.
                ignoreOfflineDelay = true;
            }
        }

        if (!ignoreOfflineDelay)
        {
            nextAction = shouldDrain(loggerInstance, sessionHolder, targetHost);
            if (nextAction != null)
            {
                return nextAction;
            }
        }

        loggerInstance.info("Queueing delayed task termination for host '%s'", targetHost.getDisplayName());

        return new NextAction.WaitForActivity(TaskForContainerTermination.scheduleTask(sessionHolder, rec_task, -1));
    }

    public static NextAction shouldDrain(ILogger loggerInstance,
                                         SessionHolder sessionHolder,
                                         DeploymentHostRecord targetHost)
    {
        DeploymentHostOnlineSessions sessions      = targetHost.getOnlineSessions();
        DeploymentHostOnlineSession  sessionActive = sessions.accessLastSession(false);
        if (sessionActive == null)
        {
            return new NextAction.Sleep(60);
        }

        CustomerServiceRecord rec_svc = targetHost.getCustomerService();
        if (rec_svc == null)
        {
            // Not bound to any role, okay to terminate immediately.
            return null;
        }

        if (rec_svc.findAnyTaskForRole(sessionHolder, DeploymentStatus.Ready, DeploymentRole.hub) != null)
        {
            try
            {
                DeployLogicForHub            logic   = DeployLogicForHub.fromRecord(sessionHolder, rec_svc);
                DeploymentHostServiceDetails details = logic.getGatewayDetails(targetHost, true);

                if (details != null)
                {
                    GatewayQueueStatus qs = details.queueStatus;
                    if (qs != null)
                    {
                        if (BoxingUtils.get(qs.numberOfBatchedEntries, 0) == 0)
                        {
                            // Pretty much drained, okay to proceed.
                            return null;
                        }

                        int entries = BoxingUtils.get(qs.numberOfUnbatchedEntries, 0) + BoxingUtils.get(qs.numberOfBatchedEntries, 0);
                        int batches = BoxingUtils.get(qs.numberOfBatches, 0);
                        loggerInstance.info("Task termination for host '%s' delayed, since it has %,d entries in %,d batches to drain (oldest %s)", targetHost.getDisplayName(), entries, batches, qs.oldestEntry);

                        return new NextAction.Sleep(10 * 60);
                    }
                }
            }
            catch (Throwable t)
            {
                // Ignore Hub failures, retry later.
                return new NextAction.Sleep(10 * 60);
            }
        }

        DeploymentHostOnlineSession sessionClosed = sessions.accessLastSession(true);
        if (sessionClosed != null)
        {
            Duration diff = Duration.between(sessionClosed.end, sessionActive.start);

            //
            // After an offline period, delay by one minute per hour offline.
            //
            long offlineInHours = Math.max(3600, diff.getSeconds()) / 3600;
            long delayInMinutes = Math.max(10, Math.min(2 * 60, offlineInHours));

            ZonedDateTime startPST   = sessionActive.start.withZoneSameInstant(ZoneId.of("America/Los_Angeles"));
            ZonedDateTime delayUntil = startPST.plus(delayInMinutes, ChronoUnit.MINUTES);

            if (TimeUtils.wasUpdatedRecently(sessionActive.start, (int) delayInMinutes, TimeUnit.MINUTES))
            {
                // If the unit has not been online long enough, delay termination.
                loggerInstance.info("Task termination for host '%s' delayed by %d minutes (until %s), since it's been offline for %d hours",
                                    targetHost.getDisplayName(),
                                    delayInMinutes,
                                    delayUntil,
                                    offlineInHours);
                return new NextAction.Sleep(60);
            }
            else
            {
                loggerInstance.info("Host '%s' has been offline for %d hours, delayed by %d minutes (until %s)", targetHost.getDisplayName(), offlineInHours, delayInMinutes, delayUntil);
            }
        }

        return null;
    }
}
