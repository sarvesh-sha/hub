/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.recurring;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wasComputationCancelled;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3RecurringProcessor;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.deployment.DeploymentAgent;
import com.optio3.cloud.builder.model.deployment.DeploymentGlobalDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentHost;
import com.optio3.cloud.builder.model.deployment.DeploymentHostImage;
import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentTask;
import com.optio3.cloud.builder.model.jobs.output.RegistryImageReleaseStatus;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentInstance;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedBootOptionsPush;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedImagePruning;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedImagePull;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedWaypointUpdate;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.logic.BackgroundActivityScheduler;
import com.optio3.cloud.logic.RecurringActivityHandler;
import com.optio3.cloud.persistence.DbEvent;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.infra.waypoint.BootConfig;
import com.optio3.logging.Logger;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@Optio3RecurringProcessor
public class RecurringAgentCheck extends RecurringActivityHandler implements RecurringActivityHandler.ForTable
{
    public static final Logger LoggerInstance = BackgroundActivityScheduler.LoggerInstance.createSubLogger(RecurringAgentCheck.class);

    enum ConfigVariable implements IConfigVariable
    {
        HostSysId("HOST_SYSID"),
        HostId("HOST_ID"),
        AgentId("AGENT_ID"),
        LastHeartbeat("LAST_HEARTBEAT"),
        SupplyVoltage("BATTERY_VOLTAGE"),
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

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator       = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_gotHeartbeat = s_configValidator.newTemplate(RecurringAgentCheck.class, "emails/agent/got_heartbeat.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_noHeartbeat  = s_configValidator.newTemplate(RecurringAgentCheck.class, "emails/agent/no_heartbeat.txt", "${", "}");

    //--//

    private       ZonedDateTime              m_nextRun;
    private final Map<String, ZonedDateTime> m_hostTimestamp = Maps.newHashMap();

    //--//

    @Override
    public Class<?> getEntityClass()
    {
        return DeploymentAgentRecord.class;
    }

    @Override
    public boolean shouldTrigger(DbEvent event)
    {
        return true;
    }

    //--//

    @Override
    public Duration startupDelay()
    {
        return Duration.of(10, ChronoUnit.MINUTES);
    }

    @Override
    public CompletableFuture<ZonedDateTime> process(SessionProvider sessionProvider) throws
                                                                                     Exception
    {
        ZonedDateTime now = TimeUtils.now();

        if (TimeUtils.isBeforeOrNull(m_nextRun, now))
        {
            DeploymentGlobalDescriptor.Settings settings = new DeploymentGlobalDescriptor.Settings();
            settings.loadDeployments = true;

            DeploymentGlobalDescriptor globalDescriptor = sessionProvider.computeInReadOnlySession((sessionHolder) -> DeploymentGlobalDescriptor.get(sessionHolder, settings));

            Stopwatch stTotal   = Stopwatch.createStarted();
            int       total     = 0;
            int       processed = 0;

            for (DeploymentHost host : globalDescriptor.hosts.values())
            {
                total++;

                ZonedDateTime timestamp = null;

                timestamp = TimeUtils.updateIfAfter(timestamp, host.updatedOn);
                timestamp = TimeUtils.updateIfAfter(timestamp, host.lastHeartbeat);

                for (DeploymentAgent agent : host.rawAgents)
                {
                    timestamp = TimeUtils.updateIfAfter(timestamp, agent.updatedOn);
                    timestamp = TimeUtils.updateIfAfter(timestamp, agent.lastHeartbeat);
                }

                for (DeploymentTask task : host.rawTasks)
                {
                    timestamp = TimeUtils.updateIfAfter(timestamp, task.updatedOn);
                }

                ZonedDateTime lastTimestamp = m_hostTimestamp.get(host.sysId);
                if (!TimeUtils.wasUpdatedRecently(lastTimestamp, 1, TimeUnit.HOURS) || lastTimestamp.isBefore(timestamp))
                {
                    processed++;

                    m_hostTimestamp.put(host.sysId, now);

                    LoggerInstance.debugVerbose("Processing host '%s'", host.computeDisplayName());

                    Stopwatch st = Stopwatch.createStarted();

                    try (SessionHolder holder = sessionProvider.newSessionWithTransaction())
                    {
                        processHost(holder, now, host);

                        holder.commit();
                    }
                    catch (Throwable t)
                    {
                        LoggerInstance.error("Failed to process agents on host '%s', due to %s", host.computeDisplayName(), t);
                    }

                    LoggerInstance.debugVerbose("Processed host '%s' in %,dmsec", host.computeDisplayName(), st.elapsed(TimeUnit.MILLISECONDS));

                    // Yield processor.
                    await(sleep(1, TimeUnit.MILLISECONDS));
                }

                if (wasComputationCancelled())
                {
                    break;
                }
            }

            LoggerInstance.debug("Processed %,d hosts out of %,d in %,dmsec", processed, total, stTotal.elapsed(TimeUnit.MILLISECONDS));

            m_nextRun = TimeUtils.future(10, TimeUnit.MINUTES);
        }

        return wrapAsync(m_nextRun);
    }

    @Override
    public void shutdown()
    {
        // Nothing to do.
    }

    //--//

    private void processHost(SessionHolder sessionHolder,
                             ZonedDateTime now,
                             DeploymentHost host) throws
                                                  Exception
    {
        RecordLocked<DeploymentHostRecord> lock_host = sessionHolder.getEntityWithLock(DeploymentHostRecord.class, host.sysId, 2, TimeUnit.MINUTES);
        DeploymentHostRecord               rec_host  = lock_host.get();

        BuilderConfiguration cfg = sessionHolder.getServiceNonNull(BuilderConfiguration.class);
        rec_host.refreshCharges(cfg, now, 2); // No point in updating more than every couple of hours...

        AtomicBoolean schedulePruning          = new AtomicBoolean();
        AtomicBoolean scheduleFrontendShutdown = new AtomicBoolean();

        //--//

        {
            MetadataMap metadata = rec_host.getMetadata();

            processAgentsHeartbeat(sessionHolder, now, metadata, rec_host, host.rawAgents, cfg.developerSettings.disableImagePruning ? null : schedulePruning, scheduleFrontendShutdown);

            rec_host.updateOnlineOfflineState(sessionHolder, LoggerInstance, metadata);

            rec_host.setMetadata(metadata);
        }

        processStaleTaskImages(sessionHolder, rec_host, host.rawTasks);

        processStaleBootstrapImages(sessionHolder, lock_host);

        //--//

        //
        // Need to schedule all tasks after writing back metadata, or the changes will be lost.
        //

        if (schedulePruning.get())
        {
            DelayedImagePruning.queue(lock_host, 30);
        }

        if (scheduleFrontendShutdown.get())
        {
            var optValue = new BootConfig.OptionAndValue();
            optValue.key   = BootConfig.Options.DisableFrontend;
            optValue.value = "true";
            DelayedBootOptionsPush.queue(lock_host, optValue);

            optValue = new BootConfig.OptionAndValue();
            optValue.key   = BootConfig.Options.DisableWifi;
            optValue.value = "true";
            DelayedBootOptionsPush.queue(lock_host, optValue);
        }
    }

    private void processAgentsHeartbeat(SessionHolder sessionHolder,
                                        ZonedDateTime now,
                                        MetadataMap metadata,
                                        DeploymentHostRecord rec_host,
                                        DeploymentAgent[] agents,
                                        AtomicBoolean schedulePruning,
                                        AtomicBoolean scheduleFrontendShutdown)
    {
        DeploymentInstance instanceType = rec_host.getInstanceType();
        if (instanceType != null && !instanceType.hasAgent)
        {
            // This host doesn't have an agent that regularly checks in, skip the rest of the logic.
            return;
        }

        DeploymentOperationalStatus operationalStatus = rec_host.getOperationalStatus();
        boolean                     isHostOperational = operationalStatus == DeploymentOperationalStatus.operational;
        boolean                     noRecentHeartbeat = true;

        DeploymentHostRecord.AgentFailures failures = DeploymentHostRecord.WellKnownMetadata.agentFailures.get(metadata);
        if (failures == null)
        {
            failures = new DeploymentHostRecord.AgentFailures();
        }

        for (DeploymentAgent agent : agents)
        {
            ZonedDateTime lastHeartbeat = agent.lastHeartbeat;
            if (lastHeartbeat == null)
            {
                continue;
            }

            //
            // If no heartbeat received in a month, change state from 'operational' to 'lost connectivity'
            //
            if (TimeUtils.wasUpdatedRecently(lastHeartbeat, 30, TimeUnit.DAYS))
            {
                noRecentHeartbeat = false;
            }

            boolean isAgentReady = agent.status == DeploymentStatus.Ready;

            ZonedDateTime warningThreshold = now.minus(rec_host.getWarningThreshold(), ChronoUnit.MINUTES);
            ZonedDateTime successThreshold = now.minus(30, ChronoUnit.MINUTES);

            if (lastHeartbeat.isBefore(warningThreshold))
            {
                // Only send emails for Operational hosts.
                if (isHostOperational && isAgentReady && !failures.lookup.containsKey(agent.sysId))
                {
                    failures.lookup.put(agent.sysId, lastHeartbeat);

                    sendEmail(sessionHolder, agent, rec_host, BuilderApplication.EmailFlavor.Warning, "Agent failure", s_template_noHeartbeat);
                }
            }
            else if (lastHeartbeat.isAfter(successThreshold))
            {
                ZonedDateTime lastEmail = failures.lookup.get(agent.sysId);
                if (lastEmail != null)
                {
                    failures.lookup.remove(agent.sysId);

                    Duration d = Duration.between(lastEmail, now);
                    String   subject;

                    long val = d.toDays();
                    if (val > 0)
                    {
                        subject = String.format("Agent Back Online after %d days", val);
                    }
                    else
                    {
                        val = d.toHours();
                        if (val > 0)
                        {
                            subject = String.format("Agent  Back Online after %d hours", val);
                        }
                        else
                        {
                            subject = "Agent Back Online";
                        }
                    }

                    sendEmail(sessionHolder, agent, rec_host, BuilderApplication.EmailFlavor.Info, subject, s_template_gotHeartbeat);
                }

                if (schedulePruning != null && operationalStatus.acceptsNewTasks())
                {
                    final ZonedDateTime nextPrune = DeploymentHostRecord.WellKnownMetadata.nextImagesPrune.get(metadata);

                    if (TimeUtils.isBeforeOrNull(nextPrune, now))
                    {
                        DeploymentHostRecord.WellKnownMetadata.nextImagesPrune.put(metadata, now.plus(5, ChronoUnit.DAYS));

                        schedulePruning.set(true);
                    }
                }

                ZonedDateTime timeout = DeploymentHostRecord.WellKnownMetadata.frontendTimeout.get(metadata);
                if (timeout != null && timeout.isBefore(now))
                {
                    DeploymentHostRecord.WellKnownMetadata.frontendTimeout.remove(metadata);
                    scheduleFrontendShutdown.set(true);
                }
            }
        }

        if (noRecentHeartbeat && isHostOperational)
        {
            rec_host.setOperationalStatus(DeploymentOperationalStatus.lostConnectivity);
        }

        setAgentFailures(metadata, failures);
    }

    private void processStaleBootstrapImages(SessionHolder sessionHolder,
                                             RecordLocked<DeploymentHostRecord> lock_host) throws
                                                                                           Exception
    {
        DeploymentHostRecord rec_host = lock_host.get();

        switch (rec_host.getOperationalStatus())
        {
            case idle:
            case operational:
                DockerImageArchitecture architecture = rec_host.getArchitecture();
                if (architecture.isArm32())
                {
                    boolean mismatchDeployerBootstrap = true;
                    boolean missingWaypointBootstrap  = true;
                    boolean hasImageList              = false;

                    for (DeploymentHostImage image : rec_host.getImages())
                    {
                        hasImageList = true;

                        String tag = image.tag;

                        if (DeploymentHostImage.isBoostrap(tag))
                        {
                            String aliasTag = DeploymentHostImage.mapFromLegacyVersion(tag);
                            if (aliasTag != null)
                            {
                                tag = aliasTag;
                            }

                            if (StringUtils.equals(DeploymentHostImage.WAYPOINT_BOOTSTRAP_ARM, tag))
                            {
                                missingWaypointBootstrap = false;
                            }

                            if (StringUtils.equals(DeploymentHostImage.DEPLOYER_BOOTSTRAP_ARM, tag))
                            {
                                RegistryTaggedImageRecord rec_taggedImage = RegistryTaggedImageRecord.findMatch(sessionHolder,
                                                                                                                DeploymentRole.deployer,
                                                                                                                architecture,
                                                                                                                RegistryImageReleaseStatus.Release);
                                if (rec_taggedImage != null)
                                {
                                    String expectedImageSha = rec_taggedImage.getImageSha();
                                    String actualImageSha   = image.id;

                                    if (StringUtils.equals(expectedImageSha, actualImageSha))
                                    {
                                        mismatchDeployerBootstrap = false;
                                    }
                                }
                            }
                        }
                    }

                    if (hasImageList)
                    {
                        if (mismatchDeployerBootstrap)
                        {
                            RegistryTaggedImageRecord rec_taggedImage = RegistryTaggedImageRecord.findMatch(sessionHolder, DeploymentRole.deployer, architecture, RegistryImageReleaseStatus.Release);

                            if (rec_taggedImage != null)
                            {
                                if (DelayedImagePull.queue(lock_host, rec_taggedImage, null))
                                {
                                    LoggerInstance.warn("Detected stale Deployer bootstrap image on host '%s', queued pull...", rec_host.getDisplayName());
                                }
                            }
                        }

                        if (missingWaypointBootstrap && rec_host.getOperationalStatus() == DeploymentOperationalStatus.idle)
                        {
                            RegistryTaggedImageRecord rec_taggedImage = RegistryTaggedImageRecord.findMatch(sessionHolder, DeploymentRole.waypoint, architecture, RegistryImageReleaseStatus.Release);
                            if (rec_taggedImage != null)
                            {
                                if (DelayedWaypointUpdate.queue(lock_host, rec_taggedImage))
                                {
                                    LoggerInstance.warn("Detected missing Waypoint image on host '%s', queued pull...", rec_host.getDisplayName());
                                }
                            }
                        }
                    }
                    else
                    {
                        LoggerInstance.debug("No image list on host '%s'...", rec_host.getDisplayName());
                    }
                }
                break;
        }
    }

    private void processStaleTaskImages(SessionHolder sessionHolder,
                                        DeploymentHostRecord rec_host,
                                        DeploymentTask[] tasks)
    {
        if (!TimeUtils.wasUpdatedRecently(rec_host.getLastHeartbeat(), 30, TimeUnit.DAYS))
        {
            for (DeploymentTask task : tasks)
            {
                if (task.imageReference != null)
                {
                    LoggerInstance.warn("Detected stale task '%s' on host '%s', resetting image reference...", task.name, rec_host.getDisplayName());

                    DeploymentTaskRecord rec_task = sessionHolder.getEntity(DeploymentTaskRecord.class, task.sysId);
                    rec_task.setImageReference(null);
                }
            }
        }
    }

    private void sendEmail(SessionHolder sessionHolder,
                           DeploymentAgent agent,
                           DeploymentHostRecord rec_host,
                           BuilderApplication.EmailFlavor flavor,
                           String emailSubject,
                           ConfigVariables.Template<ConfigVariable> emailTemplate)
    {
        if (rec_host != null)
        {
            switch (rec_host.getOperationalStatus())
            {
                case operational:
                    BuilderApplication app = sessionHolder.getServiceNonNull(BuilderApplication.class);

                    ConfigVariables<ConfigVariable> parameters = emailTemplate.allocate();

                    String supplyVoltage;

                    if (agent.details.batteryVoltage > 0)
                    {
                        supplyVoltage = String.format("%.1fV", agent.details.batteryVoltage);
                    }
                    else
                    {
                        supplyVoltage = "N/A";
                    }

                    parameters.setValue(ConfigVariable.HostSysId, rec_host.getSysId());
                    parameters.setValue(ConfigVariable.HostId, rec_host.getDisplayName());
                    parameters.setValue(ConfigVariable.AgentId, agent.instanceId);
                    parameters.setValue(ConfigVariable.LastHeartbeat, agent.lastHeartbeat);
                    parameters.setValue(ConfigVariable.SupplyVoltage, supplyVoltage);
                    parameters.setValue(ConfigVariable.Timestamp, TimeUtils.now());

                    app.sendEmailNotification(flavor, rec_host.prepareEmailSubject(emailSubject), parameters);
                    break;
            }
        }
    }

    private static DeploymentHostRecord.AgentFailures getAgentFailures(MetadataMap metadata)
    {
        DeploymentHostRecord.AgentFailures failures = DeploymentHostRecord.WellKnownMetadata.agentFailures.get(metadata);
        if (failures == null)
        {
            failures = new DeploymentHostRecord.AgentFailures();
        }

        return failures;
    }

    private static void setAgentFailures(MetadataMap metadata,
                                         DeploymentHostRecord.AgentFailures failures)
    {
        if (failures.lookup.isEmpty())
        {
            DeploymentHostRecord.WellKnownMetadata.agentFailures.remove(metadata);
        }
        else
        {
            DeploymentHostRecord.WellKnownMetadata.agentFailures.put(metadata, failures);
        }
    }
}
