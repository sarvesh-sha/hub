/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgentOnHost;
import com.optio3.cloud.builder.model.deployment.DeploymentHostConfig;
import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.StatusCheckResult;
import com.optio3.infra.WellKnownDockerImageLabel;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public class TaskForHostCreation extends BaseHostDeployTask implements BackgroundActivityHandler.ICleanupOnFailure
{
    public RecordLocator<RegistryTaggedImageRecord> loc_image;
    public Integer                                  diskSize;
    public boolean                                  allowSNS;
    public boolean                                  allowEmail;

    //--//

    public static ActivityWithHost scheduleTask(RecordLocked<CustomerServiceRecord> lock_targetService,
                                                DeploymentHostConfig config,
                                                Duration timeout) throws
                                                                  Exception
    {
        Exceptions.requireNotNull(lock_targetService, InvalidArgumentException.class, "No Service for Host");
        Exceptions.requireNotNull(config.roles, InvalidArgumentException.class, "No Role for Host");

        SessionHolder         sessionHolder = lock_targetService.getSessionHolder();
        CustomerServiceRecord rec_svc       = lock_targetService.get();

        ActivityWithHost res = new ActivityWithHost();

        if (config.shouldDeployAgent())
        {
            RegistryTaggedImageRecord rec_taggedImage = sessionHolder.getEntity(RegistryTaggedImageRecord.class, config.imageId);

            Exceptions.requireNotNull(rec_taggedImage.findLabelOrDefault(WellKnownDockerImageLabel.ConfigTemplate, null),
                                      InvalidArgumentException.class,
                                      "No config template in image %s",
                                      rec_taggedImage.getTag());

            Exceptions.requireTrue(rec_svc.getInstanceType().deployerArch == rec_taggedImage.getArchitecture(), InvalidArgumentException.class, "No Service for Host");

            res.lock_host = DeployLogicForAgentOnHost.initializeNewHost(lock_targetService,
                                                                        rec_svc.getInstanceAccount(),
                                                                        rec_svc.getInstanceType(),
                                                                        rec_svc.getInstanceRegion(),
                                                                        config.roles);

            BackgroundActivityRecord activity = BaseDeployTask.scheduleActivity(res.lock_host, rec_svc, config.roles, null, TaskForHostCreation.class, (t) ->
            {
                DeploymentHostRecord rec_host = res.lock_host.get();
                t.loggerInstance.info("Provisioning host '%s'", rec_host.getHostId());

                t.initializeTimeout(timeout);

                t.loc_image = sessionHolder.createLocator(rec_taggedImage);
                t.diskSize  = rec_svc.getDiskSize();

                for (DeploymentRole role : config.roles)
                {
                    switch (role)
                    {
                        case hub:
                            t.allowSNS = true;
                            t.allowEmail = true;
                            break;

                        default: // TODO: BUGBUG: We need to allow all hosts to have access to SNS and Email, because permissions are per-customer, instead of per-role. Need to fix AgentDeployerForAWS.
                            t.allowSNS = true;
                            t.allowEmail = true;
                            break;
                    }
                }
            });

            res.activity = sessionHolder.createLocator(activity);
        }
        else
        {
            res.lock_host = DeployLogicForAgentOnHost.initializeNewHost(lock_targetService,
                                                                        rec_svc.getInstanceAccount(),
                                                                        config.instanceType,
                                                                        rec_svc.getInstanceRegion(),
                                                                        config.roles);

            DeploymentHostRecord rec_host = res.lock_host.get();
            rec_host.setStatus(DeploymentStatus.Ready);

            if (config.instanceType == DeploymentInstance.AZURE_EDGE)
            {
                rec_host.setOperationalStatus(DeploymentOperationalStatus.operational);
                rec_host.setWarningThreshold(7 * 24 * 60); // One week
            }
        }

        return res;
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
        return "Create Host";
    }

    @BackgroundActivityMethod()
    public CompletableFuture<Void> process() throws
                                             Exception
    {
        DeployLogicForAgentOnHost agentLogic = getLogicForAgentOnHost();

        switch (agentLogic.host_status)
        {
            case Initialized:
            {
                loggerInstance.info("Booting host '%s'", agentLogic.host_displayName);

                instanceId = agentLogic.bootHost(loc_image, diskSize, allowSNS, allowEmail);

                setHostStatus(DeploymentStatus.Booting);
                return rescheduleDelayed(15, TimeUnit.SECONDS);
            }

            case Booting:
            {
                if (agentLogic.checkHostStartup() != StatusCheckResult.Positive)
                {
                    return rescheduleDelayed(15, TimeUnit.SECONDS);
                }

                loggerInstance.info("Booted host '%s'", agentLogic.host_displayName);
                setHostStatus(DeploymentStatus.Booted);
                return rescheduleDelayed(0, TimeUnit.SECONDS);
            }

            case Booted:
            {
                boolean done = withLocatorReadonly(getTargetHostLocator(), (sessionHolder, rec_host) ->
                {
                    DeploymentAgentRecord rec_activeAgent = CollectionUtils.findFirst(rec_host.getAgents(), (agent) -> agent.getRpcId() != null);
                    return rec_activeAgent != null;
                });

                if (!done)
                {
                    return rescheduleDelayed(5, TimeUnit.SECONDS);
                }

                loggerInstance.info("Host '%s' ready for commands", agentLogic.host_displayName);
                setHostStatus(DeploymentStatus.Ready);
                return markAsCompleted();
            }

            case Cancelling:
                tryToTerminate();

                setHostStatus(DeploymentStatus.Cancelled);
                return markAsCompleted();

            default:
                return markAsCompleted();
        }
    }

    @Override
    public void cleanupOnFailure(Throwable t) throws
                                              Exception
    {
        super.cleanupOnFailure(t);

        tryToTerminate();
    }

    //--//

    private CompletableFuture<Void> tryToTerminate() throws
                                                     Exception
    {
        DeployLogicForAgentOnHost agentLogic = getLogicForAgentOnHostOrNull();
        if (agentLogic != null)
        {
            try
            {
                loggerInstance.info("Cleaning up host '%s'", agentLogic.host_displayName);

                setHostStatus(DeploymentStatus.Terminating);

                agentLogic.terminateHost();

                MonotonousTime cleanupTimeout = TimeUtils.computeTimeoutExpiration(10, TimeUnit.MINUTES);
                while (true)
                {
                    if (TimeUtils.isTimeoutExpired(cleanupTimeout))
                    {
                        loggerInstance.error("Host '%s' failed to shutdown in a timely fashion", agentLogic.host_displayName);
                        return AsyncRuntime.NullResult;
                    }

                    if (agentLogic.checkHostShutdown() == StatusCheckResult.Positive)
                    {
                        break;
                    }

                    await(sleep(10, TimeUnit.SECONDS));
                }

                loggerInstance.info("Terminated host '%s'", agentLogic.host_displayName);
            }
            catch (Throwable t)
            {
                loggerInstance.error("Caught exception on cleanup: %s", t);
            }
            finally
            {
                setHostStatus(DeploymentStatus.Terminated);
            }
        }

        return AsyncRuntime.NullResult;
    }
}
