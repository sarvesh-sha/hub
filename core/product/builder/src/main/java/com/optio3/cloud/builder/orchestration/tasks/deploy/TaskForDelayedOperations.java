/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import java.util.concurrent.TimeUnit;

import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.deployment.DelayedOperation;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.logging.Severity;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;

public class TaskForDelayedOperations extends BaseDeployTask
{
    public static void scheduleTask(RecordLocked<DeploymentHostRecord> targetHost) throws
                                                                                   Exception
    {
        Exceptions.requireNotNull(targetHost, InvalidArgumentException.class, "No host provided");

        SessionHolder                       sessionHolder  = targetHost.getSessionHolder();
        RecordLocator<DeploymentHostRecord> loc_targetHost = targetHost.createLocator();

        var loggerInstance = DeploymentHostRecord.buildContextualLogger(DelayedOperation.LoggerInstance, loc_targetHost);

        for (var ri : BackgroundActivityRecord.findHandlers(sessionHolder, false, true, TaskForDelayedOperations.class, loc_targetHost))
        {
            BackgroundActivityRecord rec_task = sessionHolder.fromIdentityOrNull(ri);
            if (rec_task != null)
            {
                //
                // Wakeup existing task.
                //
                if (loggerInstance.isEnabled(Severity.Debug))
                {
                    DeploymentHostRecord rec_host = targetHost.get();
                    loggerInstance.debug("Found task '%s' for host '%s': %s", rec_task.getDisplayName(), rec_host.getDisplayName(), rec_task.getStatus());
                }

                switch (rec_task.getStatus())
                {
                    case ACTIVE:
                    case SLEEPING:
                    case WAITING:
                        rec_task.transitionToActive(null);
                        break;
                }

                return;
            }
        }

        if (loggerInstance.isEnabled(Severity.Debug))
        {
            DeploymentHostRecord rec_host = targetHost.get();
            loggerInstance.debug("Queuing delayed op for host '%s'", rec_host.getDisplayName());
        }

        BaseDeployTask.scheduleActivity(targetHost, null, TaskForDelayedOperations.class, (t) ->
        {
        });
    }

    //--//

    @Override
    public void configureContext()
    {
        loggerInstance = DeploymentHostRecord.buildContextualLogger(loggerInstance, getTargetHostLocator());
    }

    @Override
    public String getTitle()
    {
        return String.format("Process delayed operations for '%s'", getHostDisplayName());
    }

    @BackgroundActivityMethod(needsSession = true, autoRetry = true)
    public void process(SessionHolder sessionHolder) throws
                                                     Exception
    {
        RecordLocked<DeploymentHostRecord> lock_host = getTargetHostOrNull(sessionHolder, 10, TimeUnit.MINUTES);
        if (lock_host == null)
        {
            // Host deleted, exit.
            markAsCompleted();
            return;
        }

        DeploymentHostRecord rec_host = lock_host.get();
        if (!rec_host.hasHeartbeat())
        {
            // Not online, no point in continuing.
            markAsCompleted();
            return;
        }

        DelayedOperation.NextAction nextAction = DeploymentHostRecord.processDelayedOperations(lock_host);
        if (nextAction != null)
        {
            DelayedOperation.NextAction.WaitForActivity waitFor = Reflection.as(nextAction, DelayedOperation.NextAction.WaitForActivity.class);
            if (waitFor != null)
            {
                waitForSubActivity(sessionHolder.createLocator(waitFor.activity), null);
                return;
            }

            DelayedOperation.NextAction.Sleep sleep = Reflection.as(nextAction, DelayedOperation.NextAction.Sleep.class);
            if (sleep != null)
            {
                rescheduleSleeping(sleep.seconds, TimeUnit.SECONDS);
                return;
            }
        }

        if (rec_host.hasDelayedOperations())
        {
            rescheduleSleeping(5, TimeUnit.SECONDS);
        }
        else
        {
            markAsCompleted();
        }
    }
}
