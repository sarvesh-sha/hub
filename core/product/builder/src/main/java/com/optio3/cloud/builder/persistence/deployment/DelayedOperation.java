/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.Lists;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedAgentBatteryConfiguration;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedAgentCreation;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedAgentTermination;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedBootOptionsPull;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedBootOptionsPush;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedFileTransfer;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedImagePruning;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedImagePull;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedTaskCreation;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedTaskRestartSingle;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedTaskTermination;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedWaypointUpdate;
import com.optio3.cloud.logic.BackgroundActivityStatus;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.logging.ILogger;
import com.optio3.logging.Logger;
import com.optio3.util.TimeUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = DelayedAgentCreation.class),
                @JsonSubTypes.Type(value = DelayedAgentBatteryConfiguration.class),
                @JsonSubTypes.Type(value = DelayedAgentTermination.class),
                @JsonSubTypes.Type(value = DelayedBootOptionsPull.class),
                @JsonSubTypes.Type(value = DelayedBootOptionsPush.class),
                @JsonSubTypes.Type(value = DelayedFileTransfer.class),
                @JsonSubTypes.Type(value = DelayedImagePruning.class),
                @JsonSubTypes.Type(value = DelayedImagePull.class),
                @JsonSubTypes.Type(value = DelayedTaskCreation.class),
                @JsonSubTypes.Type(value = DelayedTaskRestartSingle.class),
                @JsonSubTypes.Type(value = DelayedTaskTermination.class),
                @JsonSubTypes.Type(value = DelayedWaypointUpdate.class) })
public abstract class DelayedOperation
{
    public interface IPreprocessState
    {
        void preprocessState(SessionHolder sessionHolder,
                             List<DelayedOperation> ops);
    }

    public static abstract class NextAction
    {
        public static class WaitForActivity extends NextAction
        {
            public final BackgroundActivityRecord activity;

            public WaitForActivity(BackgroundActivityRecord activity)
            {
                this.activity = activity;
            }
        }

        public static class Sleep extends NextAction
        {
            public final int seconds;

            public Sleep(int seconds)
            {
                this.seconds = seconds;
            }
        }

        public static class Done extends NextAction
        {
            public Done()
            {
            }
        }
    }

    public static final Logger LoggerInstance = new Logger(DelayedOperation.class);

    public String description; // Only set when returning values from REST calls.

    public int priority;

    public RecordLocator<BackgroundActivityRecord> loc_activity;

    public ZonedDateTime createdOn;
    public ZonedDateTime lastActivation;
    public int           retries;

    @JsonIgnore
    protected SessionHolder sessionHolder;

    @JsonIgnore
    protected RecordLocked<DeploymentHostRecord> lock_targetHost;

    @JsonIgnore
    protected DeployLogicForAgent agentLogic;

    @JsonIgnore
    protected ILogger loggerInstance;

    //--//

    protected DelayedOperation()
    {
        loggerInstance = LoggerInstance.createSubLogger(getClass());
    }

    public void prepare(RecordLocked<DeploymentHostRecord> lock_targetHost)
    {
        this.sessionHolder   = lock_targetHost.getSessionHolder();
        this.lock_targetHost = lock_targetHost;

        DeploymentHostRecord targetHost = lock_targetHost.get();

        agentLogic = new DeployLogicForAgent(sessionHolder, targetHost);

        loggerInstance = DeploymentHostRecord.buildContextualLogger(loggerInstance, sessionHolder.createLocator(targetHost));
        loggerInstance.debug("Preparing operation '%s' for host '%s'", getClass().getSimpleName(), targetHost.getDisplayName());
    }

    @JsonIgnore
    public BackgroundActivityRecord getActivity()
    {
        return sessionHolder.fromLocatorOrNull(loc_activity);
    }

    @JsonIgnore
    public void setActivity(BackgroundActivityRecord activity)
    {
        loc_activity = sessionHolder.createLocator(activity);
    }

    protected NextAction shouldSleep(int minutesBetweenActivations)
    {
        if (loc_activity != null)
        {
            BackgroundActivityRecord activity = getActivity();

            BackgroundActivityStatus status = activity != null ? activity.getStatus() : BackgroundActivityStatus.COMPLETED;

            if (!status.isDone())
            {
                return new NextAction.WaitForActivity(activity);
            }

            loc_activity = null;

            if (status == BackgroundActivityStatus.COMPLETED)
            {
                return new NextAction.Done();
            }
        }

        ZonedDateTime now = TimeUtils.now();

        if (lastActivation != null)
        {
            ZonedDateTime nextActivation = lastActivation.plus(minutesBetweenActivations, ChronoUnit.MINUTES);
            if (nextActivation.isAfter(now))
            {
                Duration diff = Duration.between(now, nextActivation);
                return new NextAction.Sleep((int) diff.toSeconds());
            }
        }

        lastActivation = now;
        retries++;
        return null;
    }

    protected <T extends DelayedOperation> T findOperation(List<DelayedOperation> ops,
                                                           Class<T> clz,
                                                           Predicate<T> filter)
    {
        Iterator<DelayedOperation> it = ops.iterator();
        while (it.hasNext())
        {
            DelayedOperation op = it.next();

            if (clz.isInstance(op))
            {
                T op2 = clz.cast(op);
                if (filter.test(op2))
                {
                    return op2;
                }
            }
        }

        return null;
    }

    protected <T extends DelayedOperation> List<T> removeOperations(List<DelayedOperation> ops,
                                                                    Class<T> clz,
                                                                    Predicate<T> filter)
    {
        List<T> res = Lists.newArrayList();

        Iterator<DelayedOperation> it = ops.iterator();
        while (it.hasNext())
        {
            DelayedOperation op = it.next();

            if (clz.isInstance(op))
            {
                T op2 = clz.cast(op);
                if (filter.test(op2))
                {
                    it.remove();

                    res.add(op2);
                }
            }
        }

        return res;
    }

    //--//

    public abstract boolean mightRequireImagePull();

    public abstract boolean validate(SessionHolder sessionHolder);

    public abstract String getSummary(SessionHolder sessionHolder);

    public abstract NextAction process() throws
                                         Exception;
}
