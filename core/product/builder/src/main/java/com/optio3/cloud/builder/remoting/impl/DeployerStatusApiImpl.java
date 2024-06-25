/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.remoting.impl;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.AsyncBackground;
import com.optio3.asyncawait.AsyncDelay;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3RemotableEndpoint;
import com.optio3.cloud.annotation.Optio3RemoteOrigin;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.model.deployment.DeploymentAgentDetails;
import com.optio3.cloud.builder.model.deployment.DeploymentHostDetails;
import com.optio3.cloud.builder.model.deployment.DeploymentHostOnlineSession;
import com.optio3.cloud.builder.model.deployment.DeploymentHostOnlineSessions;
import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.orchestration.tasks.bookkeeping.TaskForAlertThresholds;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForDelayedOperations;
import com.optio3.cloud.builder.orchestration.tasks.recurring.RecurringAgentCheck;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DelayedOperations;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.builder.persistence.deployment.EmbeddedMountpoint;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedAgentBatteryConfiguration;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedAgentTermination;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedTaskRestartSingle;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedTaskTermination;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryImageRecord;
import com.optio3.cloud.client.deployer.model.ContainerStatus;
import com.optio3.cloud.client.deployer.model.DeployerShutdownConfiguration;
import com.optio3.cloud.client.deployer.model.DeploymentAgentFeature;
import com.optio3.cloud.client.deployer.model.DeploymentAgentStatus;
import com.optio3.cloud.client.deployer.model.DockerCompressedLayerDescription;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.client.deployer.model.DockerImageDescription;
import com.optio3.cloud.client.deployer.model.DockerLayerChunk;
import com.optio3.cloud.client.deployer.model.DockerLayerChunks;
import com.optio3.cloud.client.deployer.model.DockerLayerChunksWithMetadata;
import com.optio3.cloud.client.deployer.model.DockerLayerDescription;
import com.optio3.cloud.client.deployer.model.DockerPackageDescription;
import com.optio3.cloud.client.deployer.model.MountPointStatus;
import com.optio3.cloud.client.deployer.proxy.DeployerStatusApi;
import com.optio3.cloud.messagebus.channel.RpcOrigin;
import com.optio3.cloud.messagebus.transport.StableIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.infra.WellKnownDockerImageLabel;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;
import com.optio3.infra.docker.DockerImageDownloader;
import com.optio3.infra.docker.DockerImageIdentifier;
import com.optio3.logging.ILogger;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;
import com.optio3.util.ConfigVariables;
import com.optio3.util.Encryption;
import com.optio3.util.IConfigVariable;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.AsyncFunctionWithException;
import org.apache.commons.lang3.StringUtils;

@Optio3RemotableEndpoint(itf = DeployerStatusApi.class)
public class DeployerStatusApiImpl implements DeployerStatusApi
{
    public static final Logger LoggerInstance = new Logger(DeployerStatusApi.class);

    //--//

    enum ConfigVariable implements IConfigVariable
    {
        HostSysId("HOST_SYSID"),
        HostId("HOST_ID"),
        Timestamp("TIME");

        private final String m_variable;

        ConfigVariable(String variable)
        {
            m_variable = variable;
        }

        public String getVariable()
        {
            return m_variable;
        }
    }

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator      = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_online      = s_configValidator.newTemplate(RecurringAgentCheck.class, "emails/agent/online.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_restartLoop = s_configValidator.newTemplate(RecurringAgentCheck.class, "emails/agent/task_restart_loop.txt", "${", "}");

    //--//

    @Inject
    private BuilderApplication m_app;

    @Optio3RemoteOrigin
    private RpcOrigin m_origin;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @Override
    public CompletableFuture<Void> checkin(DeploymentAgentStatus status) throws
                                                                         Exception
    {
        // Workaround for generic architecture.
        if (status.architecture == DockerImageArchitecture.ARM)
        {
            status.architecture = DockerImageArchitecture.ARMv7;
        }

        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<DeploymentHostRecord>  helper_host  = sessionHolder.createHelper(DeploymentHostRecord.class);
            RecordHelper<DeploymentAgentRecord> helper_agent = sessionHolder.createHelper(DeploymentAgentRecord.class);
            final ZonedDateTime                 checkinTime  = TimeUtils.now();
            final boolean                       hasCellular  = status.cellular != null;

            RecordLocked<DeploymentHostRecord> lock_host;
            DeploymentHostRecord               rec_host;

            TypedRecordIdentity<DeploymentHostRecord> ri_host = DeploymentHostRecord.findByHostId(helper_host, status.hostId);
            if (ri_host == null)
            {
                helper_host.lockTableUntilEndOfTransaction(10, TimeUnit.SECONDS);

                String uniqueName = DeploymentHostRecord.findUniqueName(sessionHolder, status.hostId, status.architecture, hasCellular);

                rec_host = DeploymentHostRecord.buildNewHost(status.hostId, uniqueName, status.architecture, null, null, null);
                rec_host.setStatus(DeploymentStatus.Ready);
                rec_host.setOperationalStatus(DeploymentOperationalStatus.idle);
                rec_host.setWarningThreshold(hasCellular ? 900 : 60);
                lock_host = helper_host.persist(rec_host);

                String                          subject    = rec_host.prepareEmailSubject("New Host Online");
                ConfigVariables<ConfigVariable> parameters = prepareEmailBody(s_template_online, rec_host);

                m_app.sendEmailNotification(BuilderApplication.EmailFlavor.Provisioning, subject, parameters);
            }
            else
            {
                lock_host = helper_host.getWithLock(ri_host.sysId, 20, TimeUnit.SECONDS);
                rec_host  = lock_host.get();

                ZonedDateTime lastHeartbeat = rec_host.getLastHeartbeat();

                rec_host.setUpdatedOn(checkinTime);
                rec_host.setArchitecture(status.architecture);

                if (rec_host.getHostName() == null)
                {
                    String uniqueName = DeploymentHostRecord.findUniqueName(sessionHolder, rec_host.getHostId(), status.architecture, hasCellular);

                    rec_host.setHostName(uniqueName);
                    rec_host.setWarningThreshold(hasCellular ? 900 : 60);
                }

                if (!TimeUtils.wasUpdatedRecently(lastHeartbeat, 4 * rec_host.getWarningThreshold(), TimeUnit.MINUTES))
                {
                    ConfigVariables<ConfigVariable> parameters = prepareEmailBody(s_template_online, rec_host);

                    if (lastHeartbeat == null)
                    {
                        m_app.sendEmailNotification(BuilderApplication.EmailFlavor.Provisioning, rec_host.prepareEmailSubject("New Host Online"), parameters);
                    }
                    else
                    {
                        Duration d = Duration.between(lastHeartbeat, checkinTime);
                        String   subject;

                        long val = d.toDays();
                        if (val > 0)
                        {
                            subject = String.format("Host Back Online after %d days", val);
                        }
                        else
                        {
                            val = d.toHours();
                            if (val > 0)
                            {
                                subject = String.format("Host Back Online after %d hours", val);
                            }
                            else
                            {
                                subject = "Host Back Online";
                            }
                        }

                        m_app.sendEmailNotification(BuilderApplication.EmailFlavor.Alert, rec_host.prepareEmailSubject(subject), parameters);
                    }
                }
            }

            var loggerInstance = DeploymentHostRecord.buildContextualLogger(LoggerInstance, sessionHolder.createLocator(rec_host));

            if (loggerInstance.isEnabled(Severity.Debug))
            {
                loggerInstance.debug("Checkin by host %s\n%s", rec_host.getDisplayName(), ObjectMappers.prettyPrintAsJson(status));
            }

            DeploymentOperationalStatus operationalStatus = rec_host.getOperationalStatus();
            switch (operationalStatus)
            {
                case factoryFloor:
                case provisioned:
                    rec_host.setOperationalStatus(DeploymentOperationalStatus.idle);

                    operationalStatus = DeploymentOperationalStatus.idle;
                    break;

                case lostConnectivity:
                {
                    DeploymentHostOnlineSessions sessions      = rec_host.getOnlineSessions();
                    DeploymentHostOnlineSession  sessionActive = sessions.accessLastSession(false);
                    if (sessionActive != null && !TimeUtils.wasUpdatedRecently(sessionActive.start, 1, TimeUnit.HOURS))
                    {
                        // Switch to operational if online for more than an hour.
                        rec_host.setOperationalStatus(DeploymentOperationalStatus.operational);

                        operationalStatus = DeploymentOperationalStatus.operational;
                    }
                    break;
                }
            }

            DeploymentAgentRecord rec_agent = rec_host.findAgent(status.instanceId);
            if (rec_agent == null)
            {
                rec_agent = DeploymentAgentRecord.newInstance(rec_host);
                rec_agent.setInstanceId(status.instanceId);

                sessionHolder.persistEntity(rec_agent);
            }
            else
            {
                helper_agent.optimisticallyUpgradeToLocked(rec_agent, 20, TimeUnit.SECONDS);
            }

            if (!rec_agent.isActive() && rec_host.findActiveAgent() == null)
            {
                rec_host.activateAgent(rec_agent);
            }

            String rpcId = m_origin.getRpcId();
            if (!StringUtils.equals(rec_agent.getRpcId(), rpcId))
            {
                loggerInstance.debug("Deployer '%s' on host '%s' reconnected with RPC Id '%s'", rec_agent.getInstanceId(), rec_host.getDisplayName(), rpcId);
                rec_agent.setRpcId(rpcId);
            }

            rec_agent.setDockerId(status.dockerId);
            rec_agent.setStatus(DeploymentStatus.Ready);
            rec_agent.setLastHeartbeat(checkinTime);
            rec_host.setLastHeartbeat(checkinTime);

            DeploymentAgentDetails details = new DeploymentAgentDetails();
            details.supportedFeatures     = status.supportedFeatures;
            details.architecture          = status.architecture;
            details.availableProcessors   = status.availableProcessors;
            details.freeMemory            = status.freeMemory;
            details.totalMemory           = status.totalMemory;
            details.maxMemory             = status.maxMemory;
            details.cpuUsageUser          = status.cpuUsageUser;
            details.cpuUsageSystem        = status.cpuUsageSystem;
            details.diskTotal             = status.diskTotal;
            details.diskFree              = status.diskFree;
            details.batteryVoltage        = status.batteryVoltage;
            details.cpuTemperature        = status.cpuTemperature;
            details.shutdownConfiguration = status.shutdownConfiguration;

            details.networkInterfaces = status.networkInterfaces;
            rec_agent.setDetails(details);

            DeploymentHostDetails hostDetails = rec_host.getDetails();

            if (hasCellular)
            {
                if (hostDetails == null)
                {
                    hostDetails = new DeploymentHostDetails();
                }

                hostDetails.update(status.cellular);

                rec_host.setDetails(hostDetails);
            }

            if (hostDetails != null && hostDetails.shouldUpdateCellular())
            {
                executeLinkToCellular(m_sessionProvider, sessionHolder.createLocator(rec_host), 1, TimeUnit.SECONDS);
            }

            //--//

            if (status.tasks != null)
            {
                updateTasks(sessionHolder, status.tasks, checkinTime, rec_host);
            }

            if (status.images != null)
            {
                rec_host.setImages(status.images);
            }

            if (status.localTime != null)
            {
                long builderTimeSec = checkinTime.toEpochSecond();
                long agentTimeSec   = status.localTime.toEpochSecond();

                long diff = Math.abs(builderTimeSec - agentTimeSec);
                if (diff > 5 * 60)
                {
                    loggerInstance.debug("Adjusting time on host '%s', due to drift of %d seconds...", rec_host.getHostId(), diff);

                    // Don't wait...
                    executeTimeSync(loggerInstance, m_sessionProvider, sessionHolder.createLocator(rec_host), diff, 100, TimeUnit.MILLISECONDS);
                }
            }

            //--//

            m_origin.setContext(rec_agent.getSysId(), status.instanceId);

            StableIdentity stableIdentity = m_origin.getIdentity(rec_host.getSysId());
            if (stableIdentity != null)
            {
                if (rec_host.getMetadata(DeploymentHostRecord.WellKnownMetadata.logRPC))
                {
                    stableIdentity.logger = loggerInstance;
                }
                else
                {
                    stableIdentity.logger = null;
                }

                stableIdentity.displayName = rec_host.getDisplayName();

                if (rec_agent.isActive())
                {
                    stableIdentity.rpcId = rpcId;
                }
            }

            //--//

            List<DeploymentAgentRecord> agentShutdownList = handleNotifications(loggerInstance, sessionHolder, rec_host, rec_agent, checkinTime);

            if (operationalStatus.acceptsNewTasks())
            {
                manageTasksInIncorrectState(loggerInstance, lock_host);
            }
            else
            {
                if (rec_host.getOperationalStatus() == DeploymentOperationalStatus.storageCorruption)
                {
                    for (DeploymentTaskRecord rec_task : rec_host.getTasks())
                    {
                        if (rec_task.getRole() != DeploymentRole.deployer)
                        {
                            try
                            {
                                if (DelayedTaskTermination.queue(sessionHolder, rec_task, true))
                                {
                                    loggerInstance.info("Detected %s task for host '%s' with corrupted storage, queueing termination...", rec_task.getRole(), rec_host.getDisplayName());
                                }
                            }
                            catch (Throwable t)
                            {
                                // Ignore failures, we'll retry on the next checkin.
                            }
                        }
                    }
                }
            }

            for (DeploymentAgentRecord rec_duplicateAgent : agentShutdownList)
            {
                if (DelayedAgentTermination.queue(sessionHolder, rec_duplicateAgent))
                {
                    loggerInstance.info("Detected duplicate agent '%s' for host '%s', queueing termination...", rec_duplicateAgent.getInstanceId(), rec_host.getDisplayName());
                }
            }

            if (rec_agent.isActive() && rec_agent.canSupport(DeploymentAgentFeature.ShutdownOnLowVoltage))
            {
                DeployerShutdownConfiguration cfgHost  = rec_host.getBatteryThresholds();
                DeployerShutdownConfiguration cfgAgent = status.shutdownConfiguration;

                if (!DeployerShutdownConfiguration.equals(cfgHost, cfgAgent))
                {
                    loggerInstance.info("Detected stale battery threshold on agent '%s' for host '%s', queueing update...", rec_agent.getInstanceId(), rec_host.getDisplayName());

                    if (cfgAgent != null)
                    {
                        loggerInstance.debug("Agent: %s", ObjectMappers.prettyPrintAsJson(cfgAgent));
                    }

                    if (cfgHost != null)
                    {
                        loggerInstance.debug("Host: %s", ObjectMappers.prettyPrintAsJson(cfgHost));
                    }

                    DelayedAgentBatteryConfiguration.queue(lock_host);
                }
            }

            if (DelayedOperations.isValid(rec_host.getDelayedOperations(sessionHolder, false)))
            {
                TaskForDelayedOperations.scheduleTask(lock_host);
            }

            if (rec_host.shouldUpdateTaskThresholds())
            {
                TaskForAlertThresholds.scheduleTask(sessionHolder, rec_host.getCustomerService(), rec_host);
            }

            sessionHolder.commit();

            return wrapAsync(null);
        }
    }

    private List<DeploymentAgentRecord> handleNotifications(ILogger loggerInstance,
                                                            SessionHolder sessionHolder,
                                                            DeploymentHostRecord rec_host,
                                                            DeploymentAgentRecord rec_agent,
                                                            ZonedDateTime checkinTime)
    {
        //
        // First agent to check-in automatically becomes the active one.
        //
        DeploymentAgentRecord rec_agent_active = rec_host.findActiveAgent();
        if (rec_agent_active == null)
        {
            rec_host.activateAgent(rec_agent);
            rec_agent_active = rec_agent;
        }

        //--//

        List<DeploymentAgentRecord> agentShutdownList = Lists.newArrayList();

        MetadataMap metadata = rec_host.getMetadata();

        //
        // Make sure the active agent is still responsive. Otherwise, switch to current agent.
        //
        {
            ZonedDateTime deadlineForSwitch = DeploymentHostRecord.WellKnownMetadata.activeAgentUnresponsive.get(metadata);

            if (rec_agent_active.gotHeartbeatRecently(1, TimeUnit.HOURS))
            {
                // Active agent synced recently, reset timer.
                deadlineForSwitch = null;
            }
            else
            {
                if (deadlineForSwitch != null)
                {
                    if (deadlineForSwitch.isBefore(checkinTime))
                    {
                        // Active agent did not sync, switch to different agent.
                        rec_host.activateAgent(rec_agent);
                        deadlineForSwitch = null;
                    }
                }
                else
                {
                    // Active agent did not sync, start switch timer.
                    deadlineForSwitch = checkinTime.plus(1, ChronoUnit.HOURS);
                }
            }

            DeploymentHostRecord.WellKnownMetadata.activeAgentUnresponsive.put(metadata, deadlineForSwitch);
        }

        //
        // Look for duplicate agents on host.
        //
        if (rec_agent.isActive())
        {
            DeploymentTaskRecord rec_task = rec_agent.findTask();
            if (rec_task != null)
            {
                ZonedDateTime deadlineForDuplicateAgents = DeploymentHostRecord.WellKnownMetadata.duplicateAgentsShutdown.get(metadata);
                boolean       gotDuplicate               = false;

                for (DeploymentAgentRecord rec_agent2 : rec_host.getAgents())
                {
                    if (rec_agent2 == rec_agent || rec_agent2.getStatus() != DeploymentStatus.Ready)
                    {
                        continue;
                    }

                    DeploymentTaskRecord rec_task2 = rec_agent2.findTask();
                    if (rec_task2 != null && StringUtils.equals(rec_task.getImage(), rec_task2.getImage()))
                    {
                        // Two agents with the same image.
                        gotDuplicate = true;

                        if (deadlineForDuplicateAgents == null)
                        {
                            deadlineForDuplicateAgents = checkinTime.plus(4, ChronoUnit.HOURS);
                        }
                        else if (deadlineForDuplicateAgents.isBefore(checkinTime))
                        {
                            agentShutdownList.add(rec_agent2);
                        }
                    }
                }

                if (gotDuplicate)
                {
                    DeploymentHostRecord.WellKnownMetadata.duplicateAgentsShutdown.put(metadata, deadlineForDuplicateAgents);
                }
                else
                {
                    DeploymentHostRecord.WellKnownMetadata.duplicateAgentsShutdown.remove(metadata);
                }
            }
        }

        //
        // Process notifications.
        //
        {
            DeploymentHostRecord.UsersNotification notify = DeploymentHostRecord.WellKnownMetadata.notifyWhenOnline.get(metadata);
            if (notify != null)
            {
                DeploymentHostRecord.WellKnownMetadata.notifyWhenOnline.remove(metadata);

                String                          subject    = rec_host.prepareEmailSubject("Host Online Notification");
                ConfigVariables<ConfigVariable> parameters = prepareEmailBody(s_template_online, rec_host);

                for (String user : notify.users)
                {
                    m_app.sendEmailNotification(user, subject, parameters);
                }
            }
        }

        rec_host.updateOnlineOfflineState(sessionHolder, loggerInstance, metadata);

        rec_host.setMetadata(metadata);

        return agentShutdownList;
    }

    private ConfigVariables<ConfigVariable> prepareEmailBody(ConfigVariables.Template<ConfigVariable> template,
                                                             DeploymentHostRecord rec_host)
    {
        ConfigVariables<ConfigVariable> parameters = template.allocate();

        parameters.setValue(ConfigVariable.HostSysId, rec_host.getSysId());
        parameters.setValue(ConfigVariable.HostId, rec_host.getDisplayName());
        parameters.setValue(ConfigVariable.Timestamp, TimeUtils.now());

        return parameters;
    }

    @Override
    public CompletableFuture<String> getDockerImageSha(String img) throws
                                                                   Exception
    {
        return findPackage(img, (pkg) -> wrapAsync(pkg.imageSha));
    }

    @Override
    public CompletableFuture<DockerPackageDescription> describeDockerTarball(String img) throws
                                                                                         Exception
    {
        return findPackage(img, (pkg) ->
        {
            if (pkg == null)
            {
                return wrapAsync(null);
            }

            DockerPackageDescription pkgDesc = new DockerPackageDescription();
            pkgDesc.metadata     = pkg.manifestRaw;
            pkgDesc.repositories = pkg.repositoriesRaw;

            for (DockerImageDownloader.ImageDetails imageDetails : pkg.images.values())
            {
                DockerImageDescription imgDesc = new DockerImageDescription();
                imgDesc.fileName = imageDetails.fileName;
                imgDesc.details  = imageDetails.raw;

                for (String diffId : imageDetails.diffIdToLayer.keySet())
                {
                    DockerImageDownloader.Layer l = imageDetails.diffIdToLayer.get(diffId);
                    imgDesc.diffIdsToLayers.put(diffId, l.id);
                }

                pkgDesc.images.add(imgDesc);
            }

            return wrapAsync(pkgDesc);
        });
    }

    @Override
    public CompletableFuture<DockerLayerDescription> describeDockerLayerTarball(String img,
                                                                                String layerId) throws
                                                                                                Exception
    {
        return findLayer(img, layerId, (l) ->
        {
            DockerLayerDescription layerDesc = new DockerLayerDescription();
            layerDesc.version = l.version;
            layerDesc.json    = l.json;
            layerDesc.size    = l.getSize();

            return wrapAsync(layerDesc);
        });
    }

    @Override
    public CompletableFuture<DockerCompressedLayerDescription> describeDockerCompressedLayerTarball(String img,
                                                                                                    String layerId) throws
                                                                                                                    Exception
    {
        return findLayer(img, layerId, (l) ->
        {
            DockerCompressedLayerDescription layerDesc = new DockerCompressedLayerDescription();
            layerDesc.version        = l.version;
            layerDesc.json           = l.json;
            layerDesc.size           = l.getSize();
            layerDesc.sizeCompressed = l.getCompressedSize();

            return wrapAsync(layerDesc);
        });
    }

    @Override
    public CompletableFuture<List<DockerLayerChunk>> describeDockerLayerChunks(String img,
                                                                               String layerId) throws
                                                                                               Exception
    {
        return findLayer(img, layerId, (l) ->
        {
            return wrapAsync(CollectionUtils.transformToList(l.getChunks(), (chunk) ->
            {
                DockerLayerChunk legacyChunk = new DockerLayerChunk();
                legacyChunk.hash = chunk.hash.toString();
                legacyChunk.size = chunk.size;
                return legacyChunk;
            }));
        });
    }

    @Override
    public CompletableFuture<DockerLayerChunks> describeDockerLayerCompactChunks(String img,
                                                                                 String layerId) throws
                                                                                                 Exception
    {
        return findLayer(img, layerId, (l) ->
        {
            return wrapAsync(DockerLayerChunks.build(l.getChunks()));
        });
    }

    @Override
    public CompletableFuture<DockerLayerChunksWithMetadata> describeDockerLayerChunksWithMetadata(String img,
                                                                                                  String layerId) throws
                                                                                                                  Exception
    {
        return findLayer(img, layerId, (l) ->
        {
            return wrapAsync(DockerLayerChunksWithMetadata.build(l.getChunksWithMetadata()));
        });
    }

    @Override
    public CompletableFuture<DockerLayerChunks> describeDockerLayerSubchunks(String img,
                                                                             String layerId,
                                                                             Encryption.Sha1Hash hash) throws
                                                                                                       Exception
    {
        return findLayer(img, layerId, (l) ->
        {
            return wrapAsync(DockerLayerChunks.build(l.getSubchunks(hash)));
        });
    }

    @Override
    public CompletableFuture<byte[]> fetchLayerTarball(String img,
                                                       String layerId,
                                                       long offset,
                                                       int size) throws
                                                                 Exception
    {
        return findLayer(img, layerId, (l) ->
        {
            return wrapAsync(l.readChunk(offset, size, false));
        });
    }

    @Override
    public CompletableFuture<byte[]> fetchCompressedLayerTarball(String img,
                                                                 String layerId,
                                                                 long offset,
                                                                 int size) throws
                                                                           Exception
    {
        return findLayer(img, layerId, (l) ->
        {
            return wrapAsync(l.readChunk(offset, size, true));
        });
    }

    @Override
    public CompletableFuture<byte[]> fetchLayerChunk(String img,
                                                     String hash,
                                                     long offset,
                                                     int size) throws
                                                               Exception
    {
        return fetchLayerChunk(img, new Encryption.Sha1Hash(hash), offset, size);
    }

    @Override
    public CompletableFuture<byte[]> fetchLayerChunk(String img,
                                                     Encryption.Sha1Hash hash,
                                                     long offset,
                                                     int size) throws
                                                               Exception
    {
        return findPackage(img, (pkg) ->
        {
            return wrapAsync(pkg.readFileChunk(hash, offset, size));
        });
    }

    @Override
    public CompletableFuture<List<byte[]>> fetchLayerChunks(String img,
                                                            List<Encryption.Sha1Hash> hashes) throws
                                                                                              Exception
    {
        return findPackage(img, (pkg) ->
        {
            List<byte[]> results = Lists.newArrayList();

            for (Encryption.Sha1Hash hash : hashes)
            {
                results.add(pkg.readFileChunk(hash));
            }

            return wrapAsync(results);
        });
    }

    //--//

    @AsyncBackground
    private static CompletableFuture<Void> executeTimeSync(ILogger loggerInstance,
                                                           SessionProvider sessionProvider,
                                                           RecordLocator<DeploymentHostRecord> loc_host,
                                                           long diff,
                                                           @AsyncDelay long delay,
                                                           @AsyncDelay TimeUnit delayUnit) throws
                                                                                           Exception
    {
        final DeployLogicForAgent agentLogic = sessionProvider.computeInReadOnlySession((sessionHolder) ->
                                                                                        {
                                                                                            DeploymentHostRecord rec_host = sessionHolder.fromLocator(loc_host);
                                                                                            return new DeployLogicForAgent(sessionHolder, rec_host);
                                                                                        });

        ZonedDateTime builderTime = TimeUtils.now();

        ZonedDateTime newTime = await(agentLogic.setSystemTime(builderTime));
        if (newTime != null)
        {
            if (diff > 900)
            {
                loggerInstance.warn("Adjusted time on host '%s', due to drift of %d seconds: %s", agentLogic.host_displayName, diff, newTime);
            }
            else
            {
                loggerInstance.debug("Adjusted time on host '%s', due to drift of %d seconds: %s", agentLogic.host_displayName, diff, newTime);
            }
        }

        return AsyncRuntime.NullResult;
    }

    @AsyncBackground
    private static CompletableFuture<Void> executeLinkToCellular(SessionProvider sessionProvider,
                                                                 RecordLocator<DeploymentHostRecord> loc_host,
                                                                 @AsyncDelay long delay,
                                                                 @AsyncDelay TimeUnit delayUnit)
    {
        try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
        {
            BuilderConfiguration cfg = sessionHolder.getServiceNonNull(BuilderConfiguration.class);

            RecordLocked<DeploymentHostRecord> lock_host   = sessionHolder.fromLocatorWithLock(loc_host, 2, TimeUnit.MINUTES);
            DeploymentHostRecord               rec_host    = lock_host.get();
            DeploymentHostDetails              hostDetails = rec_host.getDetails();

            if (hostDetails != null && hostDetails.cellular != null)
            {
                String modemICCID = hostDetails.cellular.getModemICCID();
                String modemIMSI  = hostDetails.cellular.modemIMSI;

                rec_host.tryLinkingToCellular(cfg, modemICCID, modemIMSI, null);

                sessionHolder.commit();
            }
        }

        return AsyncRuntime.NullResult;
    }

    //--//

    private void updateTasks(SessionHolder sessionHolder,
                             List<ContainerStatus> agentTasks,
                             ZonedDateTime now,
                             DeploymentHostRecord rec_host)
    {
        List<DeploymentTaskRecord> existingTasks  = rec_host.getTasks();
        Set<DeploymentTaskRecord>  removableTasks = Sets.newHashSet();

        //
        // First, mark missing tasks as terminated.
        //
        for (DeploymentTaskRecord rec_task : existingTasks)
        {
            if (findNewTask(agentTasks, rec_task) == null)
            {
                boolean hadContainer = rec_task.getDockerId() != null;

                rec_task.setDockerId(null);
                rec_task.setStatus(DeploymentStatus.Terminated);

                // If the task doesn't have any associated log, we can safely remove it.
                if (hadContainer && rec_task.getLastOffset() == 0)
                {
                    removableTasks.add(rec_task);
                }
            }
        }

        //
        // Then, add new/update existing tasks.
        //
        for (ContainerStatus task : agentTasks)
        {
            DeploymentTaskRecord rec_task = findExistingTask(existingTasks, task);
            if (rec_task == null)
            {
                rec_task = DeploymentTaskRecord.newInstance(rec_host);
                rec_task.setImage(task.image);
                sessionHolder.persistEntity(rec_task);
            }
            else if (!task.running && rec_task.getStatus() == DeploymentStatus.Stopped)
            {
                // Don't update a non-running task.
                continue;
            }

            rec_task.setDockerId(task.id);
            rec_task.setImage(task.image);
            rec_task.setName(task.name);
            rec_task.setStatus(task.running ? DeploymentStatus.Ready : DeploymentStatus.Stopped);
            rec_task.setLastHeartbeat(now);
            rec_task.setRestartCount(task.restartCount);

            rec_task.setImageReference(RegistryImageRecord.findBySha(sessionHolder.createHelper(RegistryImageRecord.class), task.image));

            rec_task.updateLabels(task.labels);

            Map<String, EmbeddedMountpoint> newMounts = Maps.newHashMap();
            for (MountPointStatus mount : task.mountPoints)
            {
                EmbeddedMountpoint mount2 = new EmbeddedMountpoint();
                mount2.setType(mount.type);
                mount2.setName(mount.name);
                mount2.setReadWrite(mount.readWrite);
                mount2.setSource(mount.source);
                mount2.setDestination(mount.destination);

                mount2.setDriver(mount.driver);
                mount2.setMode(mount.mode);
                mount2.setPropagation(mount.propagation);
                newMounts.put(mount.destination, mount2);
            }

            rec_task.updateMounts(newMounts);

            if (task.restartCount > 4 && !TimeUtils.wasUpdatedRecently(rec_task.getMetadata(DeploymentTaskRecord.WellKnownMetadata.restartWarning), 1, TimeUnit.DAYS))
            {
                ConfigVariables<ConfigVariable> parameters = prepareEmailBody(s_template_restartLoop, rec_host);

                m_app.sendEmailNotification(BuilderApplication.EmailFlavor.Warning, rec_host.prepareEmailSubject("Task In Restart Loop"), parameters);

                rec_task.putMetadata(DeploymentTaskRecord.WellKnownMetadata.restartWarning, TimeUtils.now());
            }
        }

        //
        // Purge dead tasks after one hour.
        //
        ZonedDateTime cleanupTime = now.minus(1, ChronoUnit.HOURS);
        for (DeploymentTaskRecord rec_task : existingTasks)
        {
            if (rec_task.getDockerId() == null)
            {
                if (cleanupTime.isAfter(rec_task.getUpdatedOn()))
                {
                    removableTasks.add(rec_task);
                }
            }
        }

        for (DeploymentTaskRecord rec_task : removableTasks)
        {
            rec_task.setDockerId(null);

            sessionHolder.deleteEntity(rec_task);
        }
    }

    private void manageTasksInIncorrectState(ILogger loggerInstance,
                                             RecordLocked<DeploymentHostRecord> lock_host)
    {
        SessionHolder        sessionHolder = lock_host.getSessionHolder();
        DeploymentHostRecord rec_host      = lock_host.get();

        List<DeploymentTaskRecord> existingTasks = rec_host.getTasks();

        //
        // Restart stopped tasks, if not already processing a state change.
        //
        boolean busy = rec_host.hasDelayedOperations();

        final CustomerServiceRecord rec_svc = rec_host.getCustomerService();
        if (rec_svc != null)
        {
            busy |= rec_svc.getCurrentActivityIfNotDone() != null;
        }

        if (!busy)
        {
            Multimap<DeploymentRole, DeploymentTaskRecord> activeTasks = HashMultimap.create();

            for (DeploymentTaskRecord rec_task : existingTasks)
            {
                if (rec_task.getDockerId() != null && rec_task.getStatus() == DeploymentStatus.Ready)
                {
                    activeTasks.put(rec_task.getRole(), rec_task);
                }
            }

            for (DeploymentTaskRecord rec_task : existingTasks)
            {
                if (rec_task.getDockerId() != null && rec_task.getStatus() == DeploymentStatus.Stopped)
                {
                    if (!activeTasks.containsKey(rec_task.getRole()))
                    {
                        try
                        {
                            DelayedTaskRestartSingle.queue(lock_host, rec_task, false);
                        }
                        catch (Throwable t)
                        {
                            // Ignore failures, we'll retry on the next checkin.
                        }
                    }
                }
            }

            //
            // For roles with multiple running tasks, keep only the newest.
            //
            removeDuplicates(loggerInstance,
                             sessionHolder,
                             lock_host,
                             activeTasks,
                             DeploymentRole.gateway,
                             DeploymentRole.reporter,
                             DeploymentRole.waypoint,
                             DeploymentRole.provisioner);
        }
    }

    private void removeDuplicates(ILogger loggerInstance,
                                  SessionHolder sessionHolder,
                                  RecordLocked<DeploymentHostRecord> lock_host,
                                  Multimap<DeploymentRole, DeploymentTaskRecord> activeTasks,
                                  DeploymentRole... roles)
    {
        for (DeploymentRole role : roles)
        {
            Collection<DeploymentTaskRecord> tasks = activeTasks.get(role);
            if (tasks.size() > 1)
            {
                DeploymentTaskRecord rec_newestTask = null;
                ZonedDateTime        timestamp      = null;

                for (DeploymentTaskRecord rec_task : tasks)
                {
                    ZonedDateTime createdOn = rec_task.getCreatedOn();

                    if (timestamp == null || createdOn.isAfter(timestamp))
                    {
                        timestamp      = createdOn;
                        rec_newestTask = rec_task;
                    }
                }

                //
                // First, queue terminations.
                //
                for (DeploymentTaskRecord rec_task : tasks)
                {
                    if (rec_task != rec_newestTask)
                    {
                        try
                        {
                            if (DelayedTaskTermination.queue(sessionHolder, rec_task, true))
                            {
                                DeploymentHostRecord rec_host = lock_host.get();
                                loggerInstance.info("Detected duplicate task '%s/%s' for host '%s', queueing termination...", rec_task.getRole(), rec_task.getDockerId(), rec_host.getDisplayName());
                            }
                        }
                        catch (Throwable t)
                        {
                            // Ignore failures, we'll retry on the next checkin.
                        }
                    }
                }

                //
                // Then, queue restart, so that it gets a fresh state.
                //
                try
                {
                    DelayedTaskRestartSingle.queue(lock_host, rec_newestTask, true);
                }
                catch (Throwable t)
                {
                    // Ignore failures, we'll retry on the next checkin.
                }
            }
        }
    }

    private static ContainerStatus findNewTask(List<ContainerStatus> newTasks,
                                               DeploymentTaskRecord oldTask)
    {
        for (ContainerStatus newTask : newTasks)
        {
            if (match(newTask, oldTask))
            {
                return newTask;
            }
        }

        return null;
    }

    private static DeploymentTaskRecord findExistingTask(List<DeploymentTaskRecord> existingTasks,
                                                         ContainerStatus newTask)
    {
        for (DeploymentTaskRecord rec_oldTask : existingTasks)
        {
            if (match(newTask, rec_oldTask))
            {
                return rec_oldTask;
            }
        }

        return null;
    }

    private static boolean match(ContainerStatus newTask,
                                 DeploymentTaskRecord oldTask)
    {
        if (StringUtils.equals(newTask.id, oldTask.getDockerId()))
        {
            return true;
        }

        Map<String, String> currentLabels = oldTask.getLabels();
        if (!matchLabel(newTask.labels, currentLabels, WellKnownDockerImageLabel.DeploymentPurpose))
        {
            return false;
        }

        if (!matchLabel(newTask.labels, currentLabels, WellKnownDockerImageLabel.DeploymentInstanceId))
        {
            return false;
        }

        return true;
    }

    private static boolean matchLabel(Map<String, String> newTaskLabels,
                                      Map<String, String> oldTaskLabels,
                                      WellKnownDockerImageLabel label)
    {
        String newLabelValue = label.getValue(newTaskLabels);
        String oldLabelValue = label.getValue(oldTaskLabels);

        return newLabelValue != null && newLabelValue.equals(oldLabelValue);
    }

    //--//

    private <T> CompletableFuture<T> findLayer(String img,
                                               String layerId,
                                               AsyncFunctionWithException<DockerImageDownloader.Layer, T> callback) throws
                                                                                                                    Exception
    {
        return findPackage(img, (pkg) ->
        {
            DockerImageDownloader.Layer l = pkg.findLayer(layerId);
            if (l == null)
            {
                return wrapAsync(null);
            }

            return callback.apply(l);
        });
    }

    private <T> CompletableFuture<T> findPackage(String img,
                                                 AsyncFunctionWithException<DockerImageDownloader.PackageOfImages, T> callback) throws
                                                                                                                                Exception
    {
        BuilderConfiguration cfg  = m_app.getServiceNonNull(BuilderConfiguration.class);
        UserInfo             user = null;

        if (cfg.credentials != null)
        {
            user = cfg.credentials.findFirstAutomationUser(WellKnownSites.dockerRegistry(), RoleType.Subscriber);
        }

        DockerImageDownloader dl = m_app.getServiceNonNull(DockerImageDownloader.class);
        try (DockerImageDownloader.Reservation reservation = await(dl.acquire()))
        {
            try
            {
                DockerImageDownloader.PackageOfImages pkg = reservation.analyze(user, new DockerImageIdentifier(img));
                if (pkg != null)
                {
                    T res = await(callback.apply(pkg));
                    return wrapAsync(res);
                }
            }
            catch (NotFoundException e)
            {
                // Not found, just return null.
            }

            return wrapAsync(null);
        }
    }
}
