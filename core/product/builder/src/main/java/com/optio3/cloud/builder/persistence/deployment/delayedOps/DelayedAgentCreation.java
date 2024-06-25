/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment.delayedOps;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Objects;
import com.optio3.cloud.builder.model.jobs.output.RegistryImageReleaseStatus;
import com.optio3.cloud.builder.orchestration.tasks.deploy.BaseDeployTask;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForAgentCreation;
import com.optio3.cloud.builder.persistence.deployment.DelayedOperation;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryImageRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("DelayedAgentCreation")
public class DelayedAgentCreation extends DelayedOperation implements DelayedOperation.IPreprocessState
{
    public RecordLocator<RegistryTaggedImageRecord> loc_image;
    public String                                   raw_image;
    public boolean                                  activate;

    //--//

    public static boolean queue(RecordLocked<DeploymentHostRecord> lock_target,
                                String imageTag,
                                boolean activate) throws
                                                  Exception
    {
        DelayedAgentCreation op = new DelayedAgentCreation();
        op.raw_image = imageTag;
        op.activate  = activate;
        op.priority  = 300; // This is likely an emergency agent.

        return DeploymentHostRecord.queueUnique(lock_target, op);
    }

    public static boolean queue(RecordLocked<DeploymentHostRecord> lock_target,
                                RegistryTaggedImageRecord rec_taggedImage,
                                boolean activate) throws
                                                  Exception
    {
        SessionHolder sessionHolder = lock_target.getSessionHolder();

        DelayedAgentCreation op = new DelayedAgentCreation();
        op.loc_image = sessionHolder.createLocator(rec_taggedImage);
        op.activate  = activate;
        op.priority  = 250;

        return DeploymentHostRecord.queueUnique(lock_target, op);
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        DelayedAgentCreation that = Reflection.as(o, DelayedAgentCreation.class);
        if (that != null)
        {
            return Objects.equal(loc_image, that.loc_image) && StringUtils.equals(raw_image, that.raw_image);
        }

        return false;
    }

    @Override
    public boolean mightRequireImagePull()
    {
        return loc_image != null;
    }

    @Override
    public boolean validate(SessionHolder sessionHolder)
    {
        if (raw_image != null)
        {
            return true;
        }

        RegistryTaggedImageRecord rec_taggedImage = sessionHolder.fromLocatorOrNull(loc_image);
        return rec_taggedImage != null;
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

        RegistryTaggedImageRecord rec_taggedImage = sessionHolder.fromLocator(loc_image);
        if (rec_taggedImage != null)
        {
            switch (BoxingUtils.get(rec_taggedImage.getReleaseStatus(), RegistryImageReleaseStatus.None))
            {
                case ReleaseCandidate:
                case Release:
                    activate = true;
                    break;
            }

            RegistryImageRecord rec_targetImage = rec_taggedImage.getImage();

            if (!targetHost.canStartNewAgent(rec_targetImage))
            {
                loggerInstance.warn("Task running image '%s' already present on host '%s', exiting...", rec_taggedImage.getTag(), targetHost.getDisplayName());
                return new NextAction.Done();
            }
        }

        List<DeploymentAgentRecord> agents       = targetHost.getAgents();
        int                         activeAgents = 0;
        for (DeploymentAgentRecord agent : agents)
        {
            switch (agent.getStatus())
            {
                case Ready:
                case Initialized:
                    activeAgents++;
                    break;
            }
        }

        if (activeAgents > 2)
        {
            loggerInstance.warn("Too many agents (%d) on host '%s', delaying creation...", agents.size(), targetHost.getDisplayName());
            return new NextAction.Sleep(10 * 60);
        }

        loggerInstance.info("Queueing delayed agent creation for host '%s'", targetHost.getDisplayName());

        BaseDeployTask.ActivityWithTask activityWithTask = TaskForAgentCreation.scheduleTask(lock_targetHost, rec_taggedImage, raw_image, activate);
        return new NextAction.WaitForActivity(sessionHolder.fromLocator(activityWithTask.activity));
    }

    @Override
    public String getSummary(SessionHolder sessionHolder)
    {
        if (raw_image != null)
        {
            return String.format("Create agent with image '%s'", raw_image);
        }

        RegistryTaggedImageRecord rec_taggedImage = sessionHolder.fromLocator(loc_image);
        return String.format("Create agent with image '%s'", rec_taggedImage.getTag());
    }

    //--//

    @Override
    public void preprocessState(SessionHolder sessionHolder,
                                List<DelayedOperation> ops)
    {
        // Delete all other agent creations with the same priority.
        removeOperations(ops, DelayedAgentCreation.class, (op) -> op.priority == priority);
    }
}
