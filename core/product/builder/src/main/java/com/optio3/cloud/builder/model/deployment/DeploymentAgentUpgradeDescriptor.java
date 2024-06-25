/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.builder.model.jobs.output.RegistryImage;
import com.optio3.cloud.builder.model.jobs.output.RegistryImageReleaseStatus;
import com.optio3.cloud.builder.model.jobs.output.RegistryTaggedImage;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedAgentCreation;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedAgentTermination;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;

public class DeploymentAgentUpgradeDescriptor
{
    public TypedRecordIdentity<DeploymentHostRecord> host;
    public ZonedDateTime                             lastHeartbeat;
    public String                                    displayName;
    public int                                       processed;
    public String                                    positiveReasonForSkipping;
    public String                                    negativeReasonForSkipping;

    public static DeploymentAgentUpgradeDescriptor process(SessionHolder sessionHolder,
                                                           DeploymentGlobalDescriptor globalDescriptor,
                                                           DeploymentHost host,
                                                           DeploymentAgentUpgradeAction action) throws
                                                                                                Exception
    {
        DeploymentHostStatusDescriptor desc = new DeploymentHostStatusDescriptor(host, false);

        DeploymentAgentUpgradeDescriptor res = new DeploymentAgentUpgradeDescriptor();
        res.host          = desc.ri;
        res.lastHeartbeat = host.lastHeartbeat;
        res.displayName   = host.computeDisplayName();

        if (!desc.instanceType.hasAgent)
        {
            res.negativeReasonForSkipping = "Host doesn't run agents";
            return res;
        }

        RegistryTaggedImage taggedImage = null;

        if (action.usingReleaseCandidate)
        {
            taggedImage = globalDescriptor.findCompatibleImage(RegistryImageReleaseStatus.ReleaseCandidate, DeploymentRole.deployer, desc.architecture);
        }

        if (action.usingRelease)
        {
            taggedImage = globalDescriptor.findCompatibleImage(RegistryImageReleaseStatus.Release, DeploymentRole.deployer, desc.architecture);
        }

        if (action.start || action.activate)
        {
            if (taggedImage == null)
            {
                res.negativeReasonForSkipping = String.format("No image for %s", action);
                return res;
            }

            RegistryImage image = globalDescriptor.images.get(taggedImage.image.sysId);

            if (action.start)
            {
                if (action.onlyOperational && desc.operationalStatus != DeploymentOperationalStatus.operational)
                {
                    res.positiveReasonForSkipping = "Not operational";
                    return res;
                }

                switch (desc.status)
                {
                    case Ready:
                        break;

                    case Terminating:
                    case Terminated:
                        res.negativeReasonForSkipping = "Host retired";
                        return res;

                    default:
                        res.negativeReasonForSkipping = "Host not ready yet";
                        return res;
                }

                switch (desc.operationalStatus)
                {
                    case factoryFloor:
                    case provisioned:
                        res.negativeReasonForSkipping = "Host not ready yet";
                        return res;

                    case storageCorruption:
                        res.negativeReasonForSkipping = "Host has corrupted storage";
                        return res;

                    case maintenance:
                        res.negativeReasonForSkipping = "Host in maintenance";
                        return res;

                    case retired:
                        res.negativeReasonForSkipping = "Host retired";
                        return res;
                }

                for (DeploymentAgent agent : host.rawAgents)
                {
                    switch (agent.status)
                    {
                        case Initialized:
                        case Booting:
                        case Booted:
                        case Terminating:
                        case Cancelling:
                            res.negativeReasonForSkipping = String.format("Agent in state %s", agent.status);
                            return res;
                    }
                }

                if (action.usingReleaseCandidate)
                {
                    if (desc.hasFlag(DeploymentHostStatusDescriptorFlag.AgentsRunningReleaseCandidate))
                    {
                        for (DeploymentAgent agent : host.rawAgents)
                        {
                            if (agent.findImage() == image)
                            {
                                res.positiveReasonForSkipping = "Already running Release Candidate";
                                return res;
                            }
                        }
                    }

                    final RecordLocked<DeploymentHostRecord> lock_host = sessionHolder.getEntityWithLock(DeploymentHostRecord.class, host.sysId, 2, TimeUnit.MINUTES);
                    final DeploymentHostRecord               rec_host  = lock_host.get();

                    DeploymentOperationalStatus status = rec_host.getOperationalStatus();
                    if (status.acceptsNewTasks())
                    {
                        final RegistryTaggedImageRecord rec_taggedImage = sessionHolder.getEntity(RegistryTaggedImageRecord.class, taggedImage.sysId);
                        if (DelayedAgentCreation.queue(lock_host, rec_taggedImage, false))
                        {
                            res.processed++;
                        }
                        else
                        {
                            res.positiveReasonForSkipping = "Already upgrading to Release Candidate";
                        }
                    }
                    else
                    {
                        res.negativeReasonForSkipping = String.format("State '%s' can't accept new tasks", status);
                    }
                }
                else
                {
                    if (desc.hasFlag(DeploymentHostStatusDescriptorFlag.AgentsRunningRelease))
                    {
                        for (DeploymentAgent agent : host.rawAgents)
                        {
                            if (agent.findImage() == image)
                            {
                                res.positiveReasonForSkipping = "Already running Release";
                                return res;
                            }
                        }
                    }

                    final RecordLocked<DeploymentHostRecord> lock_host = sessionHolder.getEntityWithLock(DeploymentHostRecord.class, host.sysId, 2, TimeUnit.MINUTES);
                    final DeploymentHostRecord               rec_host  = lock_host.get();

                    DeploymentOperationalStatus status = rec_host.getOperationalStatus();
                    if (status.acceptsNewTasks())
                    {
                        final RegistryTaggedImageRecord rec_taggedImage = sessionHolder.getEntity(RegistryTaggedImageRecord.class, taggedImage.sysId);
                        if (DelayedAgentCreation.queue(lock_host, rec_taggedImage, false))
                        {
                            res.processed++;
                        }
                        else
                        {
                            res.positiveReasonForSkipping = "Already upgrading to Release";
                        }
                    }
                    else
                    {
                        res.negativeReasonForSkipping = String.format("State '%s' can't accept new tasks", status);
                    }
                }

                return res;
            }

            if (action.activate)
            {
                for (DeploymentAgent agent : host.rawAgents)
                {
                    if (agent.active && !agent.isDefaultAgent() && agent.findImage() == image)
                    {
                        res.positiveReasonForSkipping = action.usingReleaseCandidate ? "Already running with Release Candidate" : "Already running with Release";
                        return res;
                    }
                }

                for (DeploymentAgent agent : host.rawAgents)
                {
                    if (!agent.active && agent.findImage() == image)
                    {
                        final DeploymentHostRecord  rec_host  = sessionHolder.getEntity(DeploymentHostRecord.class, host.sysId);
                        final DeploymentAgentRecord rec_agent = sessionHolder.getEntity(DeploymentAgentRecord.class, agent.sysId);
                        rec_host.activateAgent(rec_agent);
                        res.processed++;
                        break;
                    }
                }

                return res;
            }
        }

        if (action.terminate)
        {
            for (DeploymentAgent agent : host.rawAgents)
            {
                if (!agent.active && agent.status == DeploymentStatus.Ready)
                {
                    final DeploymentAgentRecord rec_agent = sessionHolder.getEntity(DeploymentAgentRecord.class, agent.sysId);
                    if (DelayedAgentTermination.queue(sessionHolder, rec_agent))
                    {
                        res.processed++;
                    }
                }
            }

            return res;
        }

        if (action.delete)
        {
            for (DeploymentAgent agent : host.rawAgents)
            {
                if (agent.status == DeploymentStatus.Terminated)
                {
                    final DeploymentAgentRecord rec_agent = sessionHolder.getEntity(DeploymentAgentRecord.class, agent.sysId);
                    sessionHolder.deleteEntity(rec_agent);
                    res.processed++;
                }
            }

            return res;
        }

        res.negativeReasonForSkipping = String.format("Unexpected action '%s'", action);
        return res;
    }
}
