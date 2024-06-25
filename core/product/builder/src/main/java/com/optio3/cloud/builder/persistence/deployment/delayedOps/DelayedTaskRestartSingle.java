/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment.delayedOps;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForContainerRestart;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.deployment.DelayedOperation;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;
import com.optio3.util.TimeUtils;

@JsonTypeName("DelayedTaskRestartSingle")
public class DelayedTaskRestartSingle extends DelayedOperation
{
    public RecordLocator<DeploymentTaskRecord> loc_task;
    public boolean                             stopFirst;

    //--//

    public static boolean queue(RecordLocked<DeploymentHostRecord> lock_target,
                                DeploymentTaskRecord rec_task,
                                boolean stopFirst) throws
                                                   Exception
    {
        SessionHolder sessionHolder = lock_target.getSessionHolder();

        DelayedTaskRestartSingle op = new DelayedTaskRestartSingle();
        op.loc_task  = sessionHolder.createLocator(rec_task);
        op.stopFirst = stopFirst;
        op.priority  = -100; // Low priority.

        return DeploymentHostRecord.queueUnique(lock_target, op);
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        DelayedTaskRestartSingle that = Reflection.as(o, DelayedTaskRestartSingle.class);
        if (that != null)
        {
            if (this.stopFirst == that.stopFirst && Objects.equals(that.loc_task, this.loc_task))
            {
                return true;
            }
        }

        return false;
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
        return String.format("Restarting task '%s'", rec_task.getDockerId());
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

        DeploymentHostRecord targetHost = lock_targetHost.get();

        try
        {
            RecordLocked<DeploymentTaskRecord> lock_task = sessionHolder.fromLocatorWithLockOrNull(loc_task, 2, TimeUnit.MINUTES);
            if (lock_task != null)
            {
                DeploymentTaskRecord rec_task = lock_task.get();

                String dockerId = rec_task.getDockerId();
                if (dockerId != null)
                {
                    int countTaskRestart = BoxingUtils.get(targetHost.getMetadata(DeploymentHostRecord.WellKnownMetadata.countTaskRestart), 0);

                    ZonedDateTime lastTaskRestart = targetHost.getMetadata(DeploymentHostRecord.WellKnownMetadata.lastTaskRestart);
                    if (lastTaskRestart != null)
                    {
                        if (!TimeUtils.isTimeoutExpired(lastTaskRestart.plus(countTaskRestart, ChronoUnit.MINUTES)))
                        {
                            // Exponential back-off.
                            return new NextAction.Sleep(30);
                        }
                    }

                    targetHost.putMetadata(DeploymentHostRecord.WellKnownMetadata.lastTaskRestart, TimeUtils.now());
                    targetHost.putMetadata(DeploymentHostRecord.WellKnownMetadata.countTaskRestart, countTaskRestart + 1);

                    DeploymentRole role = rec_task.getRole();

                    if (stopFirst)
                    {
                        loggerInstance.warn("Stopping and restarting task '%s' with role '%s' on host '%s'", dockerId, role, targetHost.getDisplayName());

                        BackgroundActivityRecord rec_activity = TaskForContainerRestart.scheduleTask(lock_task, true);
                        return new NextAction.WaitForActivity(rec_activity);
                    }
                    else
                    {
                        if (rec_task.getStatus() == DeploymentStatus.Stopped)
                        {
                            loggerInstance.warn("Restarting task '%s' with role '%s' on host '%s'", dockerId, role, targetHost.getDisplayName());

                            BackgroundActivityRecord rec_activity = TaskForContainerRestart.scheduleTask(lock_task, false);
                            return new NextAction.WaitForActivity(rec_activity);
                        }
                    }
                }

                targetHost.putMetadata(DeploymentHostRecord.WellKnownMetadata.countTaskRestart, null);
            }
        }
        catch (Throwable t)
        {
            loggerInstance.error("Failed to restart task on host '%s': %s", targetHost.getDisplayName(), t);
        }

        return new NextAction.Done();
    }
}
