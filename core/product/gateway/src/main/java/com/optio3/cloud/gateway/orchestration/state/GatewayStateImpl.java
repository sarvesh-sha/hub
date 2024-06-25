/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway.orchestration.state;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.AsyncBackground;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.JsonWebSocket;
import com.optio3.cloud.client.gateway.model.GatewayAutoDiscovery;
import com.optio3.cloud.client.gateway.model.GatewayNetwork;
import com.optio3.cloud.client.gateway.model.LogEntry;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayOperationTracker;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayState;
import com.optio3.cloud.client.gateway.orchestration.state.NetworkState;
import com.optio3.cloud.client.gateway.proxy.GatewayStatusApi;
import com.optio3.cloud.gateway.GatewayApplication;
import com.optio3.cloud.gateway.GatewayConfiguration;
import com.optio3.cloud.messagebus.WellKnownDestination;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.concurrency.AsyncWaitMultiple;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.logging.Logger;
import com.optio3.logging.LoggerFactory;
import com.optio3.logging.LoggerResource;
import com.optio3.logging.Severity;
import com.optio3.protocol.ipn.IpnManager;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.protocol.model.ipn.objects.IpnLocation;
import com.optio3.protocol.model.ipn.objects.argohytos.stealthpower.BaseArgoHytosModel;
import com.optio3.protocol.model.ipn.objects.bluesky.BaseBlueSkyObjectModel;
import com.optio3.protocol.model.ipn.objects.epsolar.BaseEpSolarModel;
import com.optio3.protocol.model.ipn.objects.hendrickson.Hendrickson_Watchman;
import com.optio3.protocol.model.ipn.objects.holykell.HolykellModel;
import com.optio3.protocol.model.ipn.objects.montage.BaseBluetoothGatewayObjectModel;
import com.optio3.protocol.model.ipn.objects.morningstar.BaseTriStarModel;
import com.optio3.protocol.model.ipn.objects.nitephoenix.BaseNitePhoenixModel;
import com.optio3.protocol.model.ipn.objects.palfinger.BasePalfingerModel;
import com.optio3.protocol.model.ipn.objects.stealthpower.BaseStealthPowerModel;
import com.optio3.protocol.model.ipn.objects.victron.BaseVictronModel;
import com.optio3.protocol.model.obdii.ObdiiObjectModel;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.ConfigurationPersistenceHelper;

public class GatewayStateImpl extends GatewayState
{
    private static class ProgressStatusAggregator
    {
        final Set<String>               m_uniqueSuffix    = Sets.newHashSet();
        final Map<Long, ProgressStatus> m_perSamplingSlot = Maps.newHashMap();

        long m_lastReportedSamplingSlot;
        int  m_count;
    }

    //--//

    public static final Logger LoggerInstance = new Logger(GatewayStateImpl.class, true);

    public final GatewayApplication app;

    private final Map<Integer, ProgressStatusAggregator> m_progressStatusMap = Maps.newHashMap();

    private final LinkedList<LogEntry> m_logEntries = new LinkedList<>();

    public GatewayStateImpl(GatewayApplication app,
                            ConfigurationPersistenceHelper helper,
                            int batchPeriod,
                            int flushToDiskDelay)
    {
        super(helper, batchPeriod, flushToDiskDelay, 12000, 1000, 2 * 1024 * 1024);

        this.app = app;

        app.registerService(GatewayState.class, () -> this);
    }

    //--//

    public void publishLogEntry(Object context,
                                ZonedDateTime timestamp,
                                Severity level,
                                String thread,
                                String selector,
                                String msg) throws
                                            Exception
    {
        if (context == this)
        {
            return;
        }

        LogEntry en = new LogEntry();
        en.timestamp = timestamp;
        en.level     = level;
        en.thread    = thread;
        en.selector  = selector;
        en.line      = msg;

        synchronized (m_logEntries)
        {
            final int maxPendingEntries = 10000;

            if (m_logEntries.size() > maxPendingEntries)
            {
                // Avoid running out of memory...
                m_logEntries.removeFirst();
            }

            m_logEntries.add(en);
            if (m_logEntries.size() == 1)
            {
                flushWorkerForLog();
            }
        }
    }

    @AsyncBackground
    private CompletableFuture<Void> flushWorkerForLog() throws
                                                        Exception
    {
        boolean sleepAfterError = false;

        while (true)
        {
            if (sleepAfterError)
            {
                await(sleep(10, TimeUnit.SECONDS));
            }

            synchronized (m_logEntries)
            {
                if (m_logEntries.size() == 0)
                {
                    break;
                }
            }

            try
            {
                await(flushWorkerForLogInner());

                sleepAfterError = false;
            }
            catch (TimeoutException te)
            {
                sleepAfterError = true;
            }
            catch (Throwable t)
            {
                if (sleepAfterError)
                {
                    LoggerInstance.error("Flushing log failed due to exception: %s", t);
                }
                sleepAfterError = true;
            }
        }

        return wrapAsync(null);
    }

    private CompletableFuture<Void> flushWorkerForLogInner() throws
                                                             Exception
    {
        List<LogEntry> candidates = Lists.newArrayList();

        while (true)
        {
            candidates.clear();

            int queued = 0;

            synchronized (m_logEntries)
            {
                int threshold = 50;

                for (LogEntry entry : m_logEntries)
                {
                    candidates.add(entry);
                    queued++;

                    if (--threshold == 0)
                    {
                        break;
                    }
                }
            }

            if (candidates.size() == 0)
            {
                break;
            }

            GatewayConfiguration cfg = app.getServiceNonNull(GatewayConfiguration.class);

            GatewayStatusApi proxy = await(getProxy(GatewayStatusApi.class, 1, TimeUnit.MINUTES));
            await(proxy.publishLog(cfg.instanceId, candidates));

            synchronized (m_logEntries)
            {
                for (int i = 0; i < queued && !m_logEntries.isEmpty(); i++)
                {
                    m_logEntries.removeFirst();
                }
            }
        }

        return wrapAsync(null);
    }

    //--//

    @Override
    protected NetworkState allocateNetworkState(GatewayNetwork network)
    {
        return new NetworkStateImpl(this, network);
    }

    @Override
    protected <T> CompletableFuture<T> getProxy(Class<T> clz,
                                                int timeout,
                                                TimeUnit timeoutUnit) throws
                                                                      Exception
    {
        RpcClient client = await(app.getRpcClient(10, TimeUnit.SECONDS));
        T         proxy  = client.createProxy(WellKnownDestination.Service.getId(), null, clz, timeout, timeoutUnit);

        return wrapAsync(proxy);
    }

    @Override
    protected ObjectMapper getObjectMapper()
    {
        return JsonWebSocket.getObjectMapper();
    }

    @Override
    protected boolean isCellularConnection()
    {
        return app.isCellularConnection();
    }

    @Override
    public void reportSamplingDone(int sequenceNumber,
                                   String suffix,
                                   long samplingSlot,
                                   int period,
                                   ProgressStatus stats)
    {
        ProgressStatusAggregator val;

        synchronized (m_progressStatusMap)
        {
            val = m_progressStatusMap.computeIfAbsent(period, (key) -> new ProgressStatusAggregator());
        }

        synchronized (val)
        {
            // Reports every hour.
            if (val.m_lastReportedSamplingSlot + 3600 <= samplingSlot)
            {
                ProgressStatus aggregatedStats = new ProgressStatus();
                int            count           = 0;

                for (ProgressStatus value : val.m_perSamplingSlot.values())
                {
                    aggregatedStats.accumulate(value);
                    count++;
                }

                if (count > 0)
                {
                    if (aggregatedStats.hasAnyFailure())
                    {
                        LoggerInstanceForReport.info(
                                "period %4d seconds: total of %,d samplings on %,d networks, average of %,d unreachable devices, %,d objects, %,d properties, %,d failures => %,d timeouts, %,d deadlines, %,d unknown objs/props",
                                period,
                                val.m_count,
                                val.m_uniqueSuffix.size(),
                                aggregatedStats.countDevicesUnreachable / count,
                                aggregatedStats.countObjects / count,
                                aggregatedStats.countProperties / count,
                                aggregatedStats.countPropertiesBad / count,
                                aggregatedStats.countTimeouts / count,
                                aggregatedStats.countDeadlines / count,
                                aggregatedStats.countUnknowns / count);
                    }
                    else
                    {
                        LoggerInstanceForReport.info("period %4d seconds: total of %,d samplings on %,d networks, average of %,d unreachable devices, %,d objects, %,d properties",
                                                     period,
                                                     val.m_count,
                                                     val.m_uniqueSuffix.size(),
                                                     aggregatedStats.countDevicesUnreachable / count,
                                                     aggregatedStats.countObjects / count,
                                                     aggregatedStats.countProperties / count);
                    }

                    val.m_uniqueSuffix.clear();
                    val.m_perSamplingSlot.clear();
                }

                val.m_lastReportedSamplingSlot = samplingSlot;
            }

            val.m_count++;

            ProgressStatus accumulatedStats = val.m_perSamplingSlot.computeIfAbsent(samplingSlot, (key) -> new ProgressStatus());
            accumulatedStats.accumulate(stats);

            val.m_uniqueSuffix.add(suffix);
        }
    }

    //--//

    static class AutoDiscoveryState
    {
        List<GatewayAutoDiscovery> results      = Lists.newArrayList();
        Set<String>                claimedPorts = Sets.newHashSet();

        <T extends IpnObjectModel> CompletableFuture<Boolean> checkSensor(GatewayAutoDiscovery.Flavor flavor,
                                                                          String port,
                                                                          boolean invert,
                                                                          Class<T> clz,
                                                                          ProtocolConfigForIpn cfg)
        {
            return checkSensors(port, invert, (obj) -> clz.isInstance(obj) ? flavor : null, cfg);
        }

        CompletableFuture<Boolean> checkSensors(String port,
                                                boolean invert,
                                                Function<IpnObjectModel, GatewayAutoDiscovery.Flavor> callback,
                                                ProtocolConfigForIpn cfg)
        {
            boolean success = false;
            boolean isPortAvailable;

            synchronized (this)
            {
                isPortAvailable = !claimedPorts.contains(port);
            }

            if (isPortAvailable)
            {
                Multimap<GatewayAutoDiscovery.Flavor, Class<? extends IpnObjectModel>> found = HashMultimap.create();

                FirmwareHelper f = FirmwareHelper.get();
                if (f.mightBePresent(port, invert))
                {
                    try (IpnManager manager = new IpnManager(cfg)
                    {
                        @Override
                        protected void notifyTransport(String port,
                                                       boolean opened,
                                                       boolean closed)
                        {
                        }

                        @Override
                        protected void streamSamples(IpnObjectModel obj) throws
                                                                         Exception
                        {
                            var flavor = callback.apply(obj);
                            if (flavor != null)
                            {
                                final Class<? extends IpnObjectModel> objClass = obj.getClass();

                                found.put(flavor, objClass);
                            }
                        }

                        @Override
                        protected void notifySamples(IpnObjectModel obj,
                                                     String field)
                        {
                        }

                        @Override
                        protected byte[] detectedStealthPowerBootloader(byte bootloadVersion,
                                                                        byte hardwareVersion,
                                                                        byte hardwareRevision)
                        {
                            return null;
                        }

                        @Override
                        protected void completedStealthPowerBootloader(int statusCode)
                        {
                        }
                    })
                    {
                        manager.start();

                        await(manager.getReadySignal(), 2, TimeUnit.MINUTES);

                        await(sleep(2, TimeUnit.SECONDS));
                    }
                    catch (Throwable e)
                    {
                        // Ignore errors.
                    }
                }

                for (GatewayAutoDiscovery.Flavor flavor : found.keySet())
                {
                    var classes = found.get(flavor);
                    if (!classes.isEmpty())
                    {
                        synchronized (this)
                        {
                            GatewayAutoDiscovery res = new GatewayAutoDiscovery();
                            res.flavor = flavor;
                            res.cfg    = ObjectMappers.cloneThroughJson(null, cfg);
                            res.found.addAll(classes);
                            results.add(res);

                            claimedPorts.add(port);
                        }

                        GatewayState.LoggerInstance.info("Detected new sensor: %s", flavor);
                        try (LoggerResource resource = LoggerFactory.indent(">>> "))
                        {
                            GatewayState.LoggerInstance.info("%s", ObjectMappers.prettyPrintAsJson(cfg));
                        }

                        success = true;
                    }
                }
            }

            return wrapAsync(success);
        }
    }

    @Override
    public CompletableFuture<Boolean> performAutoDiscovery(GatewayOperationTracker.State operationContext) throws
                                                                                                           Exception
    {
        await(suspendNetworks());

        GatewayState.LoggerInstance.info("Starting Auto-Configuration...");

        AutoDiscoveryState state  = new AutoDiscoveryState();
        AsyncWaitMultiple  waiter = new AsyncWaitMultiple();

        waiter.add(checkRS485orRS232(state, "/optio3-dev/optio3_RS485"));
        waiter.add(checkRS485(state, "/optio3-dev/optio3_RS485b"));
        waiter.add(checkRS232(state, "/optio3-dev/optio3_RS232"));
        waiter.add(checkRS232(state, "/optio3-dev/optio3_RS232b"));
        waiter.add(checkRS232(state, "/optio3-dev/optio3_RS232ext"));
        waiter.add(checkCANbus(state, 0));
        waiter.add(checkCANbus(state, 1));
        waiter.add(checkOBD(state, "/optio3-dev/optio3_obdii"));
        waiter.add(checkGPS(state, "/optio3-dev/optio3_gps"));

        await(waiter.drain());

        GatewayState.LoggerInstance.info("Completed Auto-Configuration.");

        operationContext.setValue(state.results);

        await(resumeNetworks());

        return wrapAsync(true);
    }

    private CompletableFuture<Void> checkRS485orRS232(AutoDiscoveryState state,
                                                      String port) throws
                                                                   Exception
    {
        await(checkRS485(state, port));

        {
            ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
            cfg.tristarPort = port;
            await(state.checkSensor(GatewayAutoDiscovery.Flavor.MorningStar, cfg.tristarPort, false, BaseTriStarModel.class, cfg));
        }

        return AsyncRuntime.NullResult;
    }

    private CompletableFuture<Void> checkRS485(AutoDiscoveryState state,
                                               String port) throws
                                                            Exception
    {
        boolean foundSomething;

        {
            ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
            cfg.ipnPort    = port;
            foundSomething = await(state.checkSensor(GatewayAutoDiscovery.Flavor.BlueSky, cfg.ipnPort, cfg.ipnInvert, BaseBlueSkyObjectModel.class, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }

            cfg.ipnInvert  = true;
            foundSomething = await(state.checkSensor(GatewayAutoDiscovery.Flavor.BlueSky, cfg.ipnPort, cfg.ipnInvert, BaseBlueSkyObjectModel.class, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }
        }

        {
            ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
            cfg.epsolarPort = port;
            foundSomething  = await(state.checkSensor(GatewayAutoDiscovery.Flavor.EpSolar, cfg.epsolarPort, cfg.epsolarInvert, BaseEpSolarModel.class, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }

            cfg.epsolarInvert = true;
            foundSomething    = await(state.checkSensor(GatewayAutoDiscovery.Flavor.EpSolar, cfg.epsolarPort, cfg.epsolarInvert, BaseEpSolarModel.class, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }
        }

        if (false) // Disabled for now, no customer demand.
        {
            ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
            cfg.holykellPort = port;
            foundSomething   = await(state.checkSensor(GatewayAutoDiscovery.Flavor.Holykell, cfg.holykellPort, cfg.holykellInvert, HolykellModel.class, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }

            cfg.holykellInvert = true;
            foundSomething     = await(state.checkSensor(GatewayAutoDiscovery.Flavor.Holykell, cfg.holykellPort, cfg.holykellInvert, HolykellModel.class, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }
        }

        return wrapAsync(null);
    }

    private CompletableFuture<Void> checkRS232(AutoDiscoveryState state,
                                               String port) throws
                                                            Exception
    {
        boolean foundSomething;

        {
            ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
            cfg.montageBluetoothGatewayPort = port;

            foundSomething = await(state.checkSensor(GatewayAutoDiscovery.Flavor.MontageBluetoothGateway, cfg.montageBluetoothGatewayPort, false, BaseBluetoothGatewayObjectModel.class, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }
        }

        {
            ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
            cfg.argohytosPort = port;

            foundSomething = await(state.checkSensor(GatewayAutoDiscovery.Flavor.ArgoHytos, cfg.argohytosPort, false, BaseArgoHytosModel.class, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }
        }

        if (false) // Disabled for now, no customer demand.
        {
            ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
            cfg.stealthpowerPort = port;

            foundSomething = await(state.checkSensor(GatewayAutoDiscovery.Flavor.StealthPower, cfg.stealthpowerPort, false, BaseStealthPowerModel.class, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }
        }

        {
            ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
            cfg.victronPort = port;

            foundSomething = await(state.checkSensor(GatewayAutoDiscovery.Flavor.Victron, cfg.victronPort, false, BaseVictronModel.class, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }
        }

        return wrapAsync(null);
    }

    private CompletableFuture<Void> checkCANbus(AutoDiscoveryState state,
                                                int id) throws
                                                        Exception
    {
        String  port = String.format("can%d", id);
        boolean foundSomething;

        {
            Function<IpnObjectModel, GatewayAutoDiscovery.Flavor> checker = (obj) ->
            {
                if (obj instanceof BasePalfingerModel)
                {
                    return GatewayAutoDiscovery.Flavor.Palfinger;
                }

                if (obj instanceof BaseNitePhoenixModel)
                {
                    return GatewayAutoDiscovery.Flavor.Bergstrom;
                }

                if (obj instanceof Hendrickson_Watchman)
                {
                    return GatewayAutoDiscovery.Flavor.HendricksonWatchman;
                }
                return null;
            };

            ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
            cfg.canPort = port;

            cfg.canFrequency     = 250000;
            cfg.canNoTermination = false;
            foundSomething       = await(state.checkSensors(cfg.canPort, cfg.canInvert, checker, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }

            cfg.canInvert  = true;
            foundSomething = await(state.checkSensors(cfg.canPort, cfg.canInvert, checker, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }

            cfg.canInvert        = false;
            cfg.canNoTermination = true;
            foundSomething       = await(state.checkSensors(cfg.canPort, cfg.canInvert, checker, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }

            cfg.canInvert  = true;
            foundSomething = await(state.checkSensors(cfg.canPort, cfg.canInvert, checker, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }

            cfg.canFrequency     = 500000;
            cfg.canNoTermination = true;
            cfg.canInvert        = false;
            foundSomething       = await(state.checkSensors(cfg.canPort, cfg.canInvert, checker, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }

            cfg.canInvert  = true;
            foundSomething = await(state.checkSensors(cfg.canPort, cfg.canInvert, checker, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }

            cfg.canInvert        = false;
            cfg.canNoTermination = false;
            foundSomething       = await(state.checkSensors(cfg.canPort, cfg.canInvert, checker, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }

            cfg.canInvert  = true;
            foundSomething = await(state.checkSensors(cfg.canPort, cfg.canInvert, checker, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }
        }

        {
            ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
            cfg.obdiiPort      = port;
            cfg.obdiiFrequency = 250000;
            foundSomething     = await(state.checkSensor(GatewayAutoDiscovery.Flavor.J1939, cfg.obdiiPort, cfg.obdiiInvert, ObdiiObjectModel.class, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }

            cfg.obdiiFrequency = 500000;
            foundSomething     = await(state.checkSensor(GatewayAutoDiscovery.Flavor.J1939, cfg.obdiiPort, cfg.obdiiInvert, ObdiiObjectModel.class, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }
        }

        return AsyncRuntime.NullResult;
    }

    private CompletableFuture<Void> checkOBD(AutoDiscoveryState state,
                                             String port) throws
                                                          Exception
    {
        boolean foundSomething;

        {
            ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
            cfg.obdiiPort      = port;
            cfg.obdiiFrequency = 115200;
            foundSomething     = await(state.checkSensor(GatewayAutoDiscovery.Flavor.OBDII, cfg.obdiiPort, cfg.obdiiInvert, ObdiiObjectModel.class, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }

            cfg.obdiiFrequency = 19200;
            foundSomething     = await(state.checkSensor(GatewayAutoDiscovery.Flavor.OBDII, cfg.obdiiPort, cfg.obdiiInvert, ObdiiObjectModel.class, cfg));
            if (foundSomething)
            {
                return AsyncRuntime.NullResult;
            }
        }

        return wrapAsync(null);
    }

    private CompletableFuture<Void> checkGPS(AutoDiscoveryState state,
                                             String port) throws
                                                          Exception
    {
        {
            ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
            cfg.gpsPort = port;
            await(state.checkSensor(GatewayAutoDiscovery.Flavor.GPS, cfg.gpsPort, false, IpnLocation.class, cfg));
        }

        return wrapAsync(null);
    }
}
