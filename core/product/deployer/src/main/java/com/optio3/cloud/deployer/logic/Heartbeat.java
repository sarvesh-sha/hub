/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.deployer.logic;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.File;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.cloud.client.deployer.model.ContainerStatus;
import com.optio3.cloud.client.deployer.model.DeployerCellularInfo;
import com.optio3.cloud.client.deployer.model.DeployerShutdownConfiguration;
import com.optio3.cloud.client.deployer.model.DeploymentAgentFeature;
import com.optio3.cloud.client.deployer.model.DeploymentAgentStatus;
import com.optio3.cloud.client.deployer.model.ImageStatus;
import com.optio3.cloud.client.deployer.proxy.DeployerStatusApi;
import com.optio3.cloud.deployer.DeployerApplication;
import com.optio3.cloud.deployer.DeployerConfiguration;
import com.optio3.cloud.deployer.remoting.impl.DeployerDockerApiImpl;
import com.optio3.cloud.messagebus.WellKnownDestination;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.cloud.messagebus.channel.RpcWorker;
import com.optio3.infra.NetworkHelper;
import com.optio3.infra.docker.DockerHelper;
import com.optio3.infra.docker.model.ContainerInspection;
import com.optio3.infra.docker.model.ContainerSummary;
import com.optio3.infra.docker.model.ImageSummary;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.interop.CpuUtilization;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public class Heartbeat extends RpcWorker.BaseHeartbeat<DeployerApplication, DeployerConfiguration>
{
    public static boolean enableCpuInformation = true; // Flip to false when profiling, JNA seems to affect JProfiler.

    private final FirmwareHelper                 m_firmwareHelper    = FirmwareHelper.get();
    private final Set<String>                    m_supportedFeatures = Sets.newHashSet();
    private       MonotonousTime                 m_frequentCheckinsUntil;
    private       CpuUtilization.Sample          m_previousSample;
    private       CpuUtilization.HighLoadMonitor m_highLoadMonitor;

    public Heartbeat(DeployerApplication app,
                     RpcWorker rpcWorker,
                     Runnable callbackBefore,
                     Runnable callbackAfter)
    {
        super(app, DeployerApplication.LoggerInstance, rpcWorker, callbackBefore, callbackAfter);

        if (enableCpuInformation)
        {
            m_previousSample  = CpuUtilization.takeSample();
            m_highLoadMonitor = new CpuUtilization.HighLoadMonitor(DeployerApplication.LoggerInstance, 60, 20, 10, 60);
        }

        DeployerConfiguration cfg = m_app.getServiceNonNull(DeployerConfiguration.class);
        if (cfg.supportedFeatures != null)
        {
            for (DeploymentAgentFeature supportedFeature : cfg.supportedFeatures)
            {
                addFeature(supportedFeature);
            }
        }
        else
        {
            if (m_firmwareHelper.supportsShutdownOnLowVoltage())
            {
                addFeature(DeploymentAgentFeature.ShutdownOnLowVoltage);
            }

            addFeature(DeploymentAgentFeature.ImagePullProgressEx2);
            addFeature(DeploymentAgentFeature.FlushAndRestart);

            addFeature(DeploymentAgentFeature.DockerBatch);
            addFeature(DeploymentAgentFeature.DockerBatchForContainerLaunch);
            addFeature(DeploymentAgentFeature.DockerBatchForContainerTerminate);
            addFeature(DeploymentAgentFeature.DockerBatchForVolumeCreate);
            addFeature(DeploymentAgentFeature.DockerBatchForVolumeDelete);

            addFeature(DeploymentAgentFeature.CopyFileChunk);
        }
    }

    private void addFeature(DeploymentAgentFeature feature)
    {
        m_supportedFeatures.add(feature.name());
    }

    @Override
    protected CompletableFuture<Duration> sendCheckinInner(boolean force) throws
                                                                          Exception
    {
        DeployerConfiguration cfg = m_app.getServiceNonNull(DeployerConfiguration.class);
        List<ContainerStatus> tasks;
        List<ImageStatus>     images;

        if (cfg.sendTasksInformationWithHeartbeat)
        {
            tasks  = fetchTasks();
            images = fetchImages();
        }
        else
        {
            tasks  = null;
            images = null;
        }

        DeploymentAgentStatus status = new DeploymentAgentStatus();

        if (force || m_app.shouldUpdateTasks(tasks))
        {
            // Only send if there were changes.
            status.tasks = tasks;
        }

        if (force || m_app.shouldUpdateImages(images))
        {
            // Only send if there were changes.
            status.images = images;
        }

        status.supportedFeatures = m_supportedFeatures;
        status.hostId            = cfg.hostId;
        status.instanceId        = cfg.instanceId;
        status.dockerId          = DockerHelper.getSelfDockerId();

        DeployerCellularInfo cellular = new DeployerCellularInfo();
        cellular.update(cfg.IMSI, cfg.IMEI, cfg.ICCID);

        if (cellular.modemIMSI != null || cellular.modemIMEI != null || cellular.getModemICCID() != null)
        {
            status.cellular = cellular;
        }

        Runtime runtime = Runtime.getRuntime();
        status.availableProcessors = runtime.availableProcessors();
        status.freeMemory          = runtime.freeMemory();
        status.totalMemory         = runtime.totalMemory();
        status.maxMemory           = runtime.maxMemory();

        try
        {
            File f = new File("/");
            status.diskTotal = f.getTotalSpace();
            status.diskFree  = f.getUsableSpace();
        }
        catch (Exception e)
        {
            // Ignore failures.
        }

        CpuUtilization.Sample cpuSample = null;

        if (enableCpuInformation)
        {
            cpuSample = CpuUtilization.takeSample();
            CpuUtilization.DeltaSample cpuDelta = new CpuUtilization.DeltaSample(m_previousSample, cpuSample);
            status.cpuUsageSystem = cpuDelta.systemPercent;
            status.cpuUsageUser   = cpuDelta.userPercent;

            m_highLoadMonitor.process((int) status.cpuUsageUser);
        }

        status.batteryVoltage = m_firmwareHelper.readBatteryVoltage();
        status.cpuTemperature = m_firmwareHelper.readTemperature();

        if (m_firmwareHelper.supportsShutdownOnLowVoltage())
        {
            FirmwareHelper.ShutdownConfiguration shutdownCfg = m_firmwareHelper.getShutdownConfiguration();
            if (shutdownCfg != null)
            {
                status.shutdownConfiguration                     = new DeployerShutdownConfiguration();
                status.shutdownConfiguration.turnOffVoltage      = shutdownCfg.turnOffVoltage;
                status.shutdownConfiguration.turnOnVoltage       = shutdownCfg.turnOnVoltage;
                status.shutdownConfiguration.turnOffDelaySeconds = shutdownCfg.turnOffDelaySeconds;
                status.shutdownConfiguration.turnOnDelaySeconds  = shutdownCfg.turnOnDelaySeconds;
            }
        }

        for (NetworkHelper.InterfaceAddressDetails itfDetails : NetworkHelper.listNetworkAddresses(false, false, false, false, null))
        {
            status.networkInterfaces.put(itfDetails.networkInterface.getName(), itfDetails.cidr.toString());
        }

        status.architecture = FirmwareHelper.architecture();

        RpcClient         client = await(m_app.getRpcClient(10, TimeUnit.SECONDS));
        DeployerStatusApi proxy  = client.createProxy(WellKnownDestination.Service.getId(), null, DeployerStatusApi.class, 10, TimeUnit.SECONDS);

        status.localTime = TimeUtils.now();

        await(proxy.checkin(status), 10, TimeUnit.SECONDS);

        m_previousSample = cpuSample;

        m_app.touchHeartbeat();
        m_app.touchWatchdog();

        m_app.updateTasks(tasks);
        m_app.updateImages(images);

        //
        // After the first successful checking, keep sending frequent checkins for an hour.
        // This helps to keep the connection open.
        //
        if (m_frequentCheckinsUntil == null)
        {
            m_frequentCheckinsUntil = TimeUtils.computeTimeoutExpiration(1, TimeUnit.HOURS);
        }

        if (!TimeUtils.isTimeoutExpired(m_frequentCheckinsUntil))
        {
            return wrapAsync(Duration.ofSeconds(60));
        }

        return wrapAsync(Duration.ofMinutes(20));
    }

    private static List<ContainerStatus> fetchTasks()
    {
        for (int retries = 0; retries < 3; retries++)
        {
            try (DockerHelper helper = new DockerHelper(null))
            {
                List<ContainerStatus> tasks = Lists.newArrayList();

                for (ContainerSummary cont : helper.listContainers(true, null))
                {
                    ContainerInspection ci = helper.inspectContainerNoThrow(cont.id);
                    if (ci != null)
                    {
                        ContainerStatus sub = DeployerDockerApiImpl.copyDetails(ci);
                        tasks.add(sub);
                    }
                }

                tasks.sort(Comparator.comparing((task) -> task.id));

                return tasks;
            }
            catch (Throwable t)
            {
                // Sometimes, Docker times out when getting the list of containers. Ignore this failure.
            }
        }

        // Don't propagate the failure, just send no tasks to the builder.
        return null;
    }

    private static List<ImageStatus> fetchImages()
    {
        for (int retries = 0; retries < 3; retries++)
        {
            try (DockerHelper helper = new DockerHelper(null))
            {
                List<ImageStatus> res = Lists.newArrayList();

                for (ImageSummary img : helper.listImages(false, null, false))
                {
                    ImageStatus is = new ImageStatus();

                    is.id       = img.id;
                    is.parentId = img.parentId;

                    is.size    = img.size;
                    is.created = TimeUtils.fromSecondsToLocalTime(img.created);

                    is.repoTags = img.repoTags;

                    is.labels = img.labels;

                    res.add(is);
                }

                return res;
            }
            catch (Throwable t)
            {
                // Sometimes, Docker times out when getting the list of containers. Ignore this failure.
            }
        }

        // Don't propagate the failure, just send no images to the builder.
        return null;
    }
}
