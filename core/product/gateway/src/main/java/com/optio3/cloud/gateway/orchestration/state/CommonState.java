/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway.orchestration.state;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayNetwork;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayOperationTracker;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayState;
import com.optio3.concurrency.Executors;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.TransportPerformanceCounters;
import com.optio3.util.function.BiConsumerWithException;
import com.optio3.util.function.ConsumerWithException;

abstract class CommonState implements AutoCloseable
{
    private static class ProgressStatusImpl extends GatewayState.ProgressStatus
    {
        private final Stopwatch     m_timer   = Stopwatch.createStarted();
        private final AtomicInteger m_holders = new AtomicInteger();
    }

    class ProgressStatusHolder implements AutoCloseable
    {
        private final String                 m_key;
        private final int                    m_frequency;
        private final ProgressReportCallback m_callback;

        private final ProgressStatusImpl m_target;

        ProgressStatusHolder(String key,
                             int frequency,
                             ProgressReportCallback callback)
        {
            m_key       = key;
            m_frequency = frequency;
            m_callback  = callback;

            boolean            first = false;
            ProgressStatusImpl ps;

            if (key != null)
            {
                synchronized (m_pendingProgress)
                {
                    ps = m_pendingProgress.get(key);
                    if (ps == null)
                    {
                        ps = new ProgressStatusImpl();
                        m_pendingProgress.put(key, ps);

                        first = true;
                    }
                }
            }
            else
            {
                ps    = new ProgressStatusImpl();
                first = true;
            }

            ps.m_holders.incrementAndGet();

            m_target = ps;

            if (first)
            {
                queueNextReport();
            }
        }

        @Override
        public void close()
        {
            if (m_target.m_holders.decrementAndGet() > 0)
            {
                return;
            }

            synchronized (m_pendingProgress)
            {
                if (m_key != null)
                {
                    m_pendingProgress.remove(m_key);
                }
            }

            sendReport(true);
        }

        GatewayState.ProgressStatus get()
        {
            return m_target;
        }

        private void sendReport(boolean last)
        {
            try
            {
                m_callback.report(m_target, last, (int) m_target.m_timer.elapsed(TimeUnit.SECONDS));
            }
            catch (Throwable t)
            {
                // Ignore report failures.
            }
        }

        private void queueNextReport()
        {
            Executors.scheduleOnDefaultPool(() ->
                                            {
                                                if (m_target.m_holders.get() > 0)
                                                {
                                                    sendReport(false);
                                                    queueNextReport();
                                                }
                                            }, m_frequency, TimeUnit.SECONDS);
        }
    }

    @FunctionalInterface
    interface ProgressReportCallback
    {
        void report(GatewayState.ProgressStatus ps,
                    boolean last,
                    int elapsedSeconds) throws
                                        Exception;
    }

    //--//

    protected final GatewayState   m_gatewayState;
    protected final GatewayNetwork m_configuration;

    private final Map<String, ProgressStatusImpl> m_pendingProgress = Maps.newHashMap();

    //--//

    protected CommonState(GatewayState gatewayState,
                          GatewayNetwork configuration)
    {
        m_gatewayState  = gatewayState;
        m_configuration = configuration;
    }

    protected void reportSamplingDone(int sequenceNumber,
                                      String suffix,
                                      long samplingSlot,
                                      int period,
                                      GatewayState.ProgressStatus stats)
    {
        m_gatewayState.reportSamplingDone(sequenceNumber, suffix, samplingSlot, period, stats);
    }

    //--//

    abstract CompletableFuture<Boolean> start() throws
                                                Exception;

    abstract CompletableFuture<Void> stop() throws
                                            Exception;

    abstract void enumerateNetworkStatistics(BiConsumerWithException<BaseAssetDescriptor, TransportPerformanceCounters> callback) throws
                                                                                                                                  Exception;

    abstract CompletableFuture<Boolean> reload(ConsumerWithException<ConsumerWithException<InputStream>> stateProcessor) throws
                                                                                                                         Exception;

    abstract CompletableFuture<Boolean> discover(GatewayOperationTracker.State operationContext,
                                                 GatewayState.ResultHolder holder_protocol,
                                                 int broadcastIntervals,
                                                 int rebroadcastCount) throws
                                                                       Exception;

    //--//

    abstract CompletableFuture<Boolean> listObjects(GatewayOperationTracker.State operationContext,
                                                    GatewayState.ResultHolder holder_protocol,
                                                    GatewayDiscoveryEntity en_protocol) throws
                                                                                        Exception;

    abstract CompletableFuture<Boolean> readAllValues(GatewayOperationTracker.State operationContext,
                                                      GatewayState.ResultHolder holder_protocol,
                                                      GatewayDiscoveryEntity en_protocol) throws
                                                                                          Exception;

    abstract CompletableFuture<Boolean> writeValues(GatewayOperationTracker.State operationContext,
                                                    GatewayState.ResultHolder holder_protocol,
                                                    GatewayDiscoveryEntity en_protocol) throws
                                                                                        Exception;

    //--//

    abstract CompletableFuture<Void> startSamplingConfiguration(GatewayOperationTracker.State operationContext) throws
                                                                                                                Exception;

    abstract CompletableFuture<Void> updateSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                                 GatewayDiscoveryEntity en_protocol) throws
                                                                                                     Exception;

    abstract CompletableFuture<Void> completeSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                                   String configId) throws
                                                                                    Exception;
}