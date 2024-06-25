/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.deployer;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.optio3.cloud.AbstractApplication;
import com.optio3.cloud.JsonDatagram;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.client.deployer.model.BatchToken;
import com.optio3.cloud.client.deployer.model.ContainerStatus;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.client.deployer.model.ImagePullToken;
import com.optio3.cloud.client.deployer.model.ImageStatus;
import com.optio3.cloud.client.deployer.model.ShellToken;
import com.optio3.cloud.deployer.logic.BatchSession;
import com.optio3.cloud.deployer.logic.BatchSessionTracker;
import com.optio3.cloud.deployer.logic.Heartbeat;
import com.optio3.cloud.deployer.logic.ImagePullSession;
import com.optio3.cloud.deployer.logic.ImagePullSessionTracker;
import com.optio3.cloud.deployer.logic.ShellSession;
import com.optio3.cloud.deployer.logic.ShellSessionTracker;
import com.optio3.cloud.messagebus.MessageBusClient;
import com.optio3.cloud.messagebus.MessageBusClientDatagram;
import com.optio3.cloud.messagebus.MessageBusClientWebSocket;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.cloud.messagebus.channel.RpcContext;
import com.optio3.cloud.messagebus.channel.RpcWorker;
import com.optio3.cloud.messagebus.payload.MbControl_UpgradeToUDP;
import com.optio3.cloud.messagebus.payload.MbControl_UpgradeToUDP_Reply;
import com.optio3.cloud.remoting.CallMarshaller;
import com.optio3.infra.NetworkHelper;
import com.optio3.infra.docker.DockerImageDownloader;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.logging.Logger;
import com.optio3.util.ConfigurationPersistenceHelper;
import com.optio3.util.FileSystem;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import com.sun.jna.Platform;
import io.dropwizard.setup.Bootstrap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.internal.inject.InjectionManager;

public class DeployerApplication extends AbstractApplication<DeployerConfiguration>
{
    public static class PersistedState
    {
        public MbControl_UpgradeToUDP_Reply udpUpgrade;
        public List<InetAddress>            addresses;

        public final Map<String, ContainerStatus> lastTasks  = Maps.newHashMap();
        public final Map<String, ImageStatus>     lastImages = Maps.newHashMap();
    }

    static class PersistedStateHolder
    {
        private final ConfigurationPersistenceHelper m_helper;
        private final File                           m_location;

        PersistedState state;

        public PersistedStateHolder(String scratchDirectory,
                                    String instanceId)
        {
            if (scratchDirectory != null)
            {
                Path configRoot = Paths.get(scratchDirectory, "Status");
                m_helper = new ConfigurationPersistenceHelper(configRoot.toFile(), instanceId);
            }
            else
            {
                m_helper = new ConfigurationPersistenceHelper((String) null, null);
            }

            m_location = m_helper.getFile("application-status");
            state      = m_helper.deserializeFromFileNoThrow(m_location, PersistedState.class);
        }

        public PersistedState ensureState()
        {
            if (state == null)
            {
                state = new PersistedState();
            }

            return state;
        }

        public void flush()
        {
            if (m_location != null)
            {
                try
                {
                    if (state != null)
                    {
                        m_helper.serializeToFile(m_location, state);
                    }
                    else
                    {
                        m_location.delete();
                    }
                }
                catch (Throwable t)
                {
                    // Ignore failures.
                }
            }
        }

        //--//

        public void configureUDP(MbControl_UpgradeToUDP_Reply res,
                                 List<InetAddress> addresses)
        {
            var state = ensureState();
            state.udpUpgrade = res;
            state.addresses  = addresses;

            flush();
        }

        public void invalidateUDP()
        {
            var state = ensureState();
            state.udpUpgrade = null;
            state.addresses  = null;

            flush();
        }

        //--//

        public boolean shouldUpdateTasks(List<ContainerStatus> tasks)
        {
            var state = ensureState();

            if (tasks.size() != state.lastTasks.size())
            {
                return true;
            }

            for (ContainerStatus task : tasks)
            {
                ContainerStatus oldTask = state.lastTasks.get(task.id);
                if (oldTask == null)
                {
                    // Added container, resend.
                    return true;
                }

                if (oldTask.running != task.running)
                {
                    // Change in running state, resend.
                    return true;
                }

                if (oldTask.restartCount < task.restartCount)
                {
                    // Container restarted, resend.
                    return true;
                }

                if (oldTask.mountPoints.size() < task.mountPoints.size())
                {
                    // This is strange, but sometimes Docker returns a different set of mountpoints.
                    // Only resend if we get extra mountpoints...
                    return true;
                }
            }

            return false;
        }

        public void updateTasks(List<ContainerStatus> tasks)
        {
            if (shouldUpdateTasks(tasks))
            {
                state.lastTasks.clear();
                for (ContainerStatus task : tasks)
                {
                    state.lastTasks.put(task.id, task);
                }

                flush();
            }
        }

        //--//

        public boolean shouldUpdateImages(List<ImageStatus> images)
        {
            if (images != null)
            {
                var state = ensureState();

                if (images.size() != state.lastImages.size())
                {
                    return true;
                }

                for (ImageStatus image : images)
                {
                    ImageStatus oldImage = state.lastImages.get(image.id);
                    if (oldImage == null || !Objects.equals(oldImage.repoTags, image.repoTags))
                    {
                        // Added image, resend.
                        return true;
                    }
                }
            }

            return false;
        }

        public void updateImages(List<ImageStatus> images)
        {
            if (shouldUpdateImages(images))
            {
                state.lastImages.clear();
                for (ImageStatus image : images)
                {
                    state.lastImages.put(image.id, image);
                }

                flush();
            }
        }
    }

    //--//

    public static final Logger LoggerInstance = new Logger(DeployerApplication.class);

    private static final int c_automaticRestartDelay = 48;

    //--//

    private final Supplier<DockerImageDownloader> m_dockerImageDownloader = Suppliers.memoize(() -> new DockerImageDownloader(Duration.of(1, ChronoUnit.DAYS)));
    private final ShellSessionTracker             m_shellSessions         = new ShellSessionTracker();
    private final ImagePullSessionTracker         m_imagePullSessions     = new ImagePullSessionTracker();
    private final BatchSessionTracker             m_batchSessions         = new BatchSessionTracker();

    private PersistedStateHolder m_persistedStateHolder;

    private RpcWorker      m_rpcWorker;
    private MonotonousTime m_nextDestinationCheck;
    private MonotonousTime m_automaticRestart;

    //--//

    public static void main(String[] args) throws
                                           Exception
    {
        new DeployerApplication().run(args);
    }

    public DeployerApplication() throws
                                 IOException
    {
        enableVariableSubstition = true;

        registerService(DockerImageDownloader.class, m_dockerImageDownloader::get);
    }

    @Override
    public String getName()
    {
        return "Optio3 Deployer";
    }

    @Override
    protected void initialize()
    {
        Bootstrap<?> bootstrap = getServiceNonNull(Bootstrap.class);
        bootstrap.addCommand(new DeployerCommand(this));
    }

    @Override
    protected boolean enablePeeringProtocol()
    {
        // We act as a client, no need for peering.
        return false;
    }

    @Override
    protected void run() throws
                         IOException
    {
        discoverRemotableEndpoints("com.optio3.cloud.deployer.remoting.impl.");

        DeployerConfiguration cfg = getServiceNonNull(DeployerConfiguration.class);

        m_persistedStateHolder = new PersistedStateHolder(cfg.scratchDirectory, cfg.instanceId);

        Path root = cfg.getAgentFilesRoot();
        FileSystem.deleteDirectory(root);
        FileSystem.createDirectory(root);
    }

    @Override
    public void cleanupOnShutdown(long timeout,
                                  TimeUnit unit)
    {
        stopLoop();
    }

    //--//

    void startLoop()
    {
        m_automaticRestart = TimeUtils.computeTimeoutExpiration(c_automaticRestartDelay, TimeUnit.HOURS);

        if (m_rpcWorker != null)
        {
            throw new RuntimeException("Already started");
        }

        RpcContext rpcContext = new RpcContext()
        {
            @Override
            public CallMarshaller getCallMarshaller()
            {
                return getServiceNonNull(CallMarshaller.class);
            }

            @Override
            public InjectionManager getInjectionManager()
            {
                return getServiceNonNull(InjectionManager.class);
            }
        };

        m_rpcWorker = new RpcWorker(rpcContext)
        {
            private JsonDatagram.SessionConfiguration m_sessionForUDP;

            @Override
            protected void setRpcContext(RpcClient rpcClient,
                                         BaseHeartbeat hb)
            {
                DeployerApplication.this.setRpcClient(rpcClient);
                DeployerApplication.this.setRpcHeartbeat(hb);
            }

            @Override
            protected boolean isCellularConnection()
            {
                return NetworkHelper.isCellularConnection();
            }

            @Override
            protected MessageBusClient allocateSocket()
            {
                if (m_sessionForUDP != null)
                {
                    if (!m_sessionForUDP.isValid())
                    {
                        m_sessionForUDP = null;
                        m_persistedStateHolder.invalidateUDP();
                    }
                    else if (m_sessionForUDP.hasActivity())
                    {
                        return new MessageBusClientDatagram(m_sessionForUDP);
                    }
                }

                DeployerConfiguration cfg = getServiceNonNull(DeployerConfiguration.class);

                return new MessageBusClientWebSocket(cfg.dnsHints, getConnectionUrl(), WellKnownRole.Machine, cfg.hostId)
                {
                    @Override
                    public boolean shouldUpgrade()
                    {
                        var state = m_persistedStateHolder.ensureState();
                        return state.udpUpgrade != null;
                    }

                    @Override
                    public boolean prepareUpgrade(RpcWorker rpcWorker)
                    {
                        if (m_sessionForUDP == null)
                        {
                            var state = m_persistedStateHolder.ensureState();

                            m_sessionForUDP       = new JsonDatagram.SessionConfiguration(state.udpUpgrade, true);
                            m_sessionForUDP.hosts = state.addresses;
                        }

                        if (!m_sessionForUDP.hasActivity())
                        {
                            // Every N hours, try connecting through WebSocket.
                            m_sessionForUDP.markActivity();
                            return false;
                        }

                        return rpcWorker.prepareUpgrade(m_sessionForUDP);
                    }

                    @Override
                    protected MbControl_UpgradeToUDP completeUpgradeRequest(MbControl_UpgradeToUDP req)
                    {
                        DockerImageArchitecture arch = FirmwareHelper.architecture();
                        if (arch == null)
                        {
                            LoggerInstance.info("Unrecognized platform %s, using regular HTTPS transport...", Platform.ARCH);
                            return null;
                        }

                        req.isIntel       = arch.isIntel();
                        req.isARM         = arch.isArm();
                        req.registerWidth = arch.getRegisterWidth();

                        DeployerConfiguration cfg = getServiceNonNull(DeployerConfiguration.class);
                        req.hostId = cfg.hostId;

                        if (req.isARM && !NetworkHelper.isCellularConnection())
                        {
                            // Not on a cellular connection, use HTTPS transport.
                            LoggerInstance.info("Not on Cellular, using regular HTTPS transport...");
                            return null;
                        }

                        return req;
                    }

                    @Override
                    protected void completeUpgradeResponse(MbControl_UpgradeToUDP_Reply res,
                                                           List<InetAddress> addresses)
                    {
                        m_persistedStateHolder.configureUDP(res, addresses);
                    }
                };
            }

            @Override
            protected String getConnectionUrl()
            {
                DeployerConfiguration cfg = getServiceNonNull(DeployerConfiguration.class);

                return cfg.connectionUrl;
            }

            @Override
            protected BaseHeartbeat<DeployerApplication, DeployerConfiguration> createHeartbeat()
            {
                return new Heartbeat(DeployerApplication.this, this, () ->
                {
                    m_shellSessions.purgeStaleEntries();
                    m_imagePullSessions.purgeStaleEntries();
                }, null);
            }

            @Override
            protected void onSuccess()
            {
                touchHeartbeat();
                touchWatchdog();
            }

            @Override
            protected void onFailure()
            {
                touchHeartbeat();

                if (TimeUtils.isTimeoutExpired(m_automaticRestart))
                {
                    LoggerInstance.info("Could not contact Builder for %d hours, restarting...", c_automaticRestartDelay);
                    Runtime.getRuntime()
                           .exit(10);
                }

                if (TimeUtils.isTimeoutExpired(m_nextDestinationCheck))
                {
                    if (checkDestinations("https://www.google.com", "https://www.bing.com"))
                    {
                        // We still have internet connectivity, not a local issue.
                        touchWatchdog();
                    }
                    else
                    {
                        m_nextDestinationCheck = TimeUtils.computeTimeoutExpiration(15, TimeUnit.MINUTES);
                    }
                }
            }

            @Override
            public boolean prepareUpgrade(JsonDatagram.SessionConfiguration session)
            {
                m_sessionForUDP = session;
                return true;
            }

            //--//

            private boolean checkDestinations(String... targets)
            {
                for (String target : targets)
                {
                    if (checkDestination(target))
                    {
                        return true;
                    }
                }

                return false;
            }

            private boolean checkDestination(String target)
            {
                try
                {
                    URL               url           = new URL(target);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                    urlConnection.setConnectTimeout(2 * 1000);
                    urlConnection.setReadTimeout(2 * 1000);
                    urlConnection.setRequestMethod("HEAD");

                    try
                    {
                        String type = urlConnection.getContentType();
                        if (StringUtils.isNotEmpty(type))
                        {
                            return true;
                        }
                    }
                    finally
                    {
                        IOUtils.close(urlConnection);
                    }
                }
                catch (Exception e)
                {
                    // Failure means no connection.
                }

                return false;
            }
        };

        m_rpcWorker.startLoop();
    }

    void stopLoop()
    {
        if (m_rpcWorker != null)
        {
            m_rpcWorker.stopLoop();
            m_rpcWorker = null;
        }

        try
        {
            m_dockerImageDownloader.get()
                                   .close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    //--//

    public boolean shouldUpdateTasks(List<ContainerStatus> tasks)
    {
        return m_persistedStateHolder.shouldUpdateTasks(tasks);
    }

    public void updateTasks(List<ContainerStatus> tasks)
    {
        m_persistedStateHolder.updateTasks(tasks);
    }

    public boolean shouldUpdateImages(List<ImageStatus> images)
    {
        return m_persistedStateHolder.shouldUpdateImages(images);
    }

    public void updateImages(List<ImageStatus> images)
    {
        m_persistedStateHolder.updateImages(images);
    }

    public void touchWatchdog()
    {
        DeployerConfiguration cfg = getServiceNonNull(DeployerConfiguration.class);
        touchFile(cfg.watchdogFile);

        m_nextDestinationCheck = TimeUtils.computeTimeoutExpiration(30, TimeUnit.MINUTES);
        m_automaticRestart     = TimeUtils.computeTimeoutExpiration(c_automaticRestartDelay, TimeUnit.HOURS);
    }

    public void touchHeartbeat()
    {
        DeployerConfiguration cfg = getServiceNonNull(DeployerConfiguration.class);
        touchFile(cfg.heartbeatFile);
    }

    private static void touchFile(String file)
    {
        if (file != null)
        {
            try
            {
                FileUtils.touch(new File(file));
            }
            catch (Exception e)
            {
                // Ignore failure.
            }
        }
    }

    //--//

    public List<ShellToken> listShellSessions()
    {
        return m_shellSessions.list();
    }

    public ShellToken registerShellSession(ShellSession session)
    {
        return m_shellSessions.register(session);
    }

    public ShellSession getShellSession(ShellToken token)
    {
        return m_shellSessions.get(token);
    }

    public void unregisterShellSession(ShellToken token)
    {
        m_shellSessions.unregister(token);
    }

    //--//

    public ImagePullToken findImagePullSession(String image)
    {
        for (ImagePullToken token : m_imagePullSessions.list())
        {
            ImagePullSession session = m_imagePullSessions.get(token);
            if (StringUtils.equals(session.getImage(), image))
            {
                return token;
            }
        }

        return null;
    }

    public ImagePullToken registerImagePullSession(ImagePullSession session)
    {
        return m_imagePullSessions.register(session);
    }

    public ImagePullSession getImagePullSession(ImagePullToken token)
    {
        return m_imagePullSessions.get(token);
    }

    public void unregisterImagePullSession(ImagePullToken token)
    {
        m_imagePullSessions.unregister(token);
    }

    //--//

    public BatchToken registerBatchSession(BatchSession session)
    {
        return m_batchSessions.register(session);
    }

    public BatchSession getBatchSession(BatchToken token)
    {
        return m_batchSessions.get(token);
    }

    public void unregisterBatchSession(BatchToken token)
    {
        m_batchSessions.unregister(token);
    }
}
