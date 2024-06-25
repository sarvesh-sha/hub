/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway.logic;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayState;
import com.optio3.cloud.messagebus.MessageBusStatistics;
import com.optio3.concurrency.Executors;
import com.optio3.infra.NetworkHelper;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.interop.CpuUtilization;
import com.optio3.interop.mediaaccess.EthernetAccess;
import com.optio3.logging.ILogger;
import com.optio3.protocol.common.ServiceWorker;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.GatewayDescriptor;
import com.optio3.protocol.model.GatewayPerfDescriptor;
import com.optio3.protocol.model.GatewayPerformanceCounters;
import com.optio3.protocol.model.transport.IpTransportAddress;
import com.optio3.util.MonotonousTime;
import com.optio3.util.StackTraceAnalyzer;
import com.optio3.util.TimeUtils;
import com.sun.jna.Platform;

public abstract class SamplePerfCounters implements AutoCloseable
{
    public static boolean enableCpuInformation = true; // Flip to false when profiling, JNA seems to affect JProfiler.

    private final ILogger m_logger;
    private final String  m_instanceId;
    private final int     m_samplingFrequencySeconds;

    private final Set<BaseAssetDescriptor>                     m_objectsSeen    = Sets.newHashSet();
    private final Set<InetAddress>                             m_localAddresses = Sets.newHashSet();
    private final Map<InetAddress, GatewayPerformanceCounters> m_transports     = Maps.newHashMap();
    private       GatewayPerformanceCounters                   m_globalCounters = new GatewayPerformanceCounters();
    private       long                                         m_previousNumberOfUploadedEntities;
    private       long                                         m_previousNumberOfUploadedEntitiesRetries;
    private final ServiceWorker                                m_pcapWorker;
    private       EthernetAccess                               m_pcap;
    private       CpuUtilization.Sample                        m_previousSample;
    private       CpuUtilization.HighLoadMonitor               m_highLoadMonitor;
    private       boolean                                      m_shutdown;

    protected SamplePerfCounters(ILogger logger,
                                 String instanceId,
                                 int samplingFrequencyInSeconds)
    {
        m_logger                   = logger;
        m_instanceId               = instanceId;
        m_samplingFrequencySeconds = samplingFrequencyInSeconds;

        if (enableCpuInformation)
        {
            m_previousSample  = CpuUtilization.takeSample();
            m_highLoadMonitor = new CpuUtilization.HighLoadMonitor(logger, 60, 20, 10, 120);
            queueNextSampling();
        }

        m_pcapWorker = new ServiceWorker(logger, "PCAP", 0, 1000)
        {
            private MonotonousTime m_nextNetworkScan;

            @Override
            protected void shutdown()
            {
                closeTransport();
            }

            @Override
            protected void worker()
            {
                while (canContinue())
                {
                    EthernetAccess pcap = m_pcap;
                    if (pcap == null)
                    {
                        try
                        {
                            if (Platform.isMac())
                            {
                                for (EthernetAccess.InterfaceDescriptor device : EthernetAccess.findAllDevices())
                                {
                                    for (EthernetAccess.InterfaceAddress address : device.addresses)
                                    {
                                        if (address.addr.family == EthernetAccess.AddressFamily.IpV4)
                                        {
                                            m_pcap = new EthernetAccess(device.name, 1000);
                                            break;
                                        }
                                    }
                                }

                                if (m_pcap == null)
                                {
                                    m_pcap = new EthernetAccess(EthernetAccess.lookupDev(), 1000);
                                }
                            }
                            else
                            {
                                m_pcap = new EthernetAccess(null, 1000);
                            }

                            m_pcap.setFilter("ip");

                            reportErrorResolution("Reconnected to PCAP!");
                        }
                        catch (Throwable t)
                        {
                            reportFailure("Failed to start PCAP", t);

                            workerSleep(10000);
                        }
                    }
                    else
                    {
                        try
                        {
                            NetworkHelper.decodeRawPackets(pcap, 100, (ip) ->
                            {
                                if (!updateCounters(ip) && TimeUtils.isTimeoutExpired(m_nextNetworkScan))
                                {
                                    m_localAddresses.clear();

                                    for (NetworkHelper.InterfaceAddressDetails itf : NetworkHelper.listNetworkAddresses(false, false, true, true, null))
                                    {
                                        InetAddress localAddress = itf.localAddress;
                                        if (localAddress instanceof Inet4Address)
                                        {
                                            m_localAddresses.add(localAddress);
                                        }
                                    }

                                    m_nextNetworkScan = TimeUtils.computeTimeoutExpiration(10, TimeUnit.SECONDS);

                                    updateCounters(ip);
                                }
                            });
                        }
                        catch (Exception e)
                        {
                            if (!canContinue())
                            {
                                // The manager has been stopped, exit.
                                return;
                            }

                            closeTransport();

                            reportDebug("Received error: %s", e);

                            workerSleep(10000);
                        }
                    }
                }
            }

            private synchronized void closeTransport()
            {
                if (m_pcap != null)
                {
                    try
                    {
                        m_pcap.close();
                    }
                    catch (Throwable t)
                    {
                        // Ignore failures.
                    }

                    m_pcap = null;
                }
            }
        };

        m_pcapWorker.start();
    }

    protected abstract int getNumberOfConnections();

    protected abstract int getNumberOfPendingEntities();

    protected abstract long getNumberOfUploadedEntities();

    protected abstract long getNumberOfUploadedEntitiesRetries();

    protected abstract MessageBusStatistics sampleMessageBusStatistics();

    protected abstract GatewayState.ResultHolder getRoot(long timeEpochSeconds);

    protected abstract void notifyLowBattery() throws
                                               Exception;

    @Override
    public void close()
    {
        m_shutdown = true;
        m_pcapWorker.close();
    }

    private void queueNextSampling()
    {
        if (!m_shutdown)
        {
            long now        = TimeUtils.nowEpochSeconds();
            long nextSample = TimeUtils.adjustGranularity(now + m_samplingFrequencySeconds, m_samplingFrequencySeconds);

            long nowMilliUtc      = TimeUtils.nowMilliUtc();
            long diffMilliseconds = Math.max(0, (nextSample * 1_000 - nowMilliUtc));

            Executors.scheduleOnDefaultPool(() -> executeSampling(nextSample), diffMilliseconds, TimeUnit.MILLISECONDS);
        }
    }

    private void executeSampling(long samplingSlot)
    {
        try
        {
            long now = TimeUtils.nowEpochSeconds();
            if (samplingSlot + 5 < now)
            {
                m_logger.warn("Perf sampling skipped slot: %s, instead of %s",
                              TimeUtils.DEFAULT_FORMATTER_NO_MILLI.format(TimeUtils.fromSecondsToLocalTime(now)),
                              TimeUtils.DEFAULT_FORMATTER_NO_MILLI.format(TimeUtils.fromSecondsToLocalTime(samplingSlot)));
            }

            Runtime runtime = Runtime.getRuntime();

            // Swap global counters.
            GatewayPerformanceCounters pc = m_globalCounters;
            m_globalCounters = new GatewayPerformanceCounters();

            pc.freeMemory  = runtime.freeMemory();
            pc.totalMemory = runtime.totalMemory();
            pc.inUseMemory = pc.totalMemory - pc.freeMemory;
            pc.threads     = StackTraceAnalyzer.numberOfThreads();

            CpuUtilization.Sample      cpuSample = CpuUtilization.takeSample();
            CpuUtilization.DeltaSample cpuDelta  = new CpuUtilization.DeltaSample(m_previousSample, cpuSample);
            pc.cpuUsageSystem = cpuDelta.systemPercent;
            pc.cpuUsageUser   = cpuDelta.userPercent;

            m_highLoadMonitor.process((int) pc.cpuUsageUser);

            try
            {
                final FirmwareHelper firmwareHelper = FirmwareHelper.get();
                pc.cpuTemperature = firmwareHelper.readTemperature();
                pc.inputVoltage   = firmwareHelper.readBatteryVoltage();

                //
                // If the voltage drops too close to the low battery value, flush entities every minute.
                //
                FirmwareHelper.ShutdownConfiguration shutdownCfg = firmwareHelper.getShutdownConfiguration();
                if (shutdownCfg != null && shutdownCfg.turnOffVoltage > 1)
                {
                    float flushThreshold = shutdownCfg.turnOffVoltage + 0.5f;
                    if (pc.inputVoltage < flushThreshold)
                    {
                        notifyLowBattery();
                    }
                }
            }
            catch (Throwable t)
            {
                // Ignore failures for voltage
            }

            long numberOfUploadedEntities        = getNumberOfUploadedEntities();
            long numberOfUploadedEntitiesRetries = getNumberOfUploadedEntitiesRetries();

            pc.entitiesUploaded        = (int) (numberOfUploadedEntities - m_previousNumberOfUploadedEntities);
            pc.entitiesUploadedRetries = (int) (numberOfUploadedEntitiesRetries - m_previousNumberOfUploadedEntitiesRetries);
            pc.pendingQueueLength      = getNumberOfPendingEntities();
            pc.numberOfConnections     = getNumberOfConnections();

            MessageBusStatistics stats = sampleMessageBusStatistics();
            if (stats != null)
            {
                pc.mbPacketTx            = stats.packetTx;
                pc.mbPacketTxBytes       = stats.packetTxBytes;
                pc.mbPacketTxBytesResent = stats.packetTxBytesResent;
                pc.mbPacketRx            = stats.packetRx;
                pc.mbPacketRxBytes       = stats.packetRxBytes;
                pc.mbPacketRxBytesResent = stats.packetRxBytesResent;
            }

            collectPerHostStatistics(samplingSlot, 32 * 1024);

            GatewayPerfDescriptor desc = new GatewayPerfDescriptor();
            sampleStatistics(samplingSlot, desc, pc);

            m_previousSample                          = cpuSample;
            m_previousNumberOfUploadedEntities        = numberOfUploadedEntities;
            m_previousNumberOfUploadedEntitiesRetries = numberOfUploadedEntitiesRetries;
        }
        catch (Throwable t)
        {
            // Ignore failures.
        }

        queueNextSampling();
    }

    private synchronized boolean updateCounters(NetworkHelper.IpHeader ip)
    {
        InetAddress src = ip.getSourceAddress();
        InetAddress dst = ip.getDestinationAddress();

        if (src.isLoopbackAddress() || dst.isLoopbackAddress())
        {
            // Ignore loopback interfaces.
            return true;
        }

        int totalLength = ip.totalLength;

        if (m_localAddresses.contains(src))
        {
            m_globalCounters.packetTx++;
            m_globalCounters.packetTxBytes += totalLength;

            GatewayPerformanceCounters counters = m_transports.computeIfAbsent(dst, (key) -> new GatewayPerformanceCounters());
            counters.packetTx++;
            counters.packetTxBytes += totalLength;
            counters.staleTimeout = TimeUtils.computeTimeoutExpiration(4, TimeUnit.HOURS);

            if (ip.protocol == 17) // UDP protocol
            {
                m_globalCounters.packetTxBytesUDP += totalLength;
                counters.packetTxBytesUDP += totalLength;
            }

            return true;
        }

        if (m_localAddresses.contains(dst))
        {
            m_globalCounters.packetRx++;
            m_globalCounters.packetRxBytes += totalLength;

            GatewayPerformanceCounters counters = m_transports.computeIfAbsent(src, (key) -> new GatewayPerformanceCounters());
            counters.packetRx++;
            counters.packetRxBytes += totalLength;
            counters.staleTimeout = TimeUtils.computeTimeoutExpiration(4, TimeUnit.HOURS);

            if (ip.protocol == 17) // UDP protocol
            {
                m_globalCounters.packetRxBytesUDP += totalLength;
                counters.packetRxBytesUDP += totalLength;
            }

            return true;
        }

        return false;
    }

    private synchronized void collectPerHostStatistics(long samplingSlot,
                                                       int threshold)
    {
        final Set<Map.Entry<InetAddress, GatewayPerformanceCounters>> entries = m_transports.entrySet();
        for (Iterator<Map.Entry<InetAddress, GatewayPerformanceCounters>> it = entries.iterator(); it.hasNext(); )
        {
            Map.Entry<InetAddress, GatewayPerformanceCounters> entry   = it.next();
            final IpTransportAddress                           address = new IpTransportAddress(entry.getKey());
            final GatewayPerformanceCounters                   pc      = entry.getValue();

            if (TimeUtils.isTimeoutExpired(pc.staleTimeout))
            {
                // Stale entry, get rid of it.
                it.remove();
                continue;
            }

            // Only report when there's enough traffic.
            if (pc.packetRxBytes > threshold || pc.packetTxBytes > threshold)
            {
                pc.shouldReport = true;
            }

            if (pc.shouldReport)
            {
                GatewayPerfDescriptor desc = new GatewayPerfDescriptor();
                desc.transportAddress = address.getHost();

                sampleStatistics(samplingSlot, desc, pc);

                // Reset values after reporting them.
                pc.packetRx         = 0;
                pc.packetRxBytes    = 0;
                pc.packetRxBytesUDP = 0;
                pc.packetTx         = 0;
                pc.packetTxBytes    = 0;
                pc.packetTxBytesUDP = 0;
            }
        }
    }

    private void sampleStatistics(long samplingSlot,
                                  BaseAssetDescriptor desc,
                                  GatewayPerformanceCounters stats)
    {
        try
        {
            boolean                   firstPass       = m_objectsSeen.add(desc);
            GatewayState.ResultHolder holder_root     = getRoot(samplingSlot);
            GatewayState.ResultHolder holder_network  = holder_root.prepareResult(GatewayDiscoveryEntitySelector.Gateway, m_instanceId, false);
            GatewayState.ResultHolder holder_protocol = holder_network.prepareResult(GatewayDiscoveryEntitySelector.Protocol, GatewayDiscoveryEntity.Protocol_Perf, false);

            GatewayDescriptor deviceDesc = new GatewayDescriptor();
            deviceDesc.sysId = m_instanceId;

            GatewayState.ResultHolder holder_device = holder_protocol.prepareResult(GatewayDiscoveryEntitySelector.Perf_Device, deviceDesc, firstPass);
            GatewayState.ResultHolder holder_object = holder_device.prepareResult(GatewayDiscoveryEntitySelector.Perf_Object, desc.toString(), firstPass);

            if (firstPass)
            {
                //
                // If this is the first time we see this counter,
                // queue some fake contents for the Hub.
                // That way the record for the counter will be created.
                //
                holder_device.queueContents("<trigger>");
                holder_object.queueContents("<trigger>");
            }

            GatewayState.ResultHolder holder_sample = holder_object.prepareResult(GatewayDiscoveryEntitySelector.Perf_ObjectSample, (String) null, true);
            holder_sample.queueContents(stats.serializeToJson());
        }
        catch (JsonProcessingException e)
        {
            // Ignore failures.
        }
    }
}
