/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway.logic;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Sets;
import com.optio3.cloud.client.gateway.model.GatewayFeature;
import com.optio3.cloud.client.gateway.model.GatewayStatus;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayState;
import com.optio3.cloud.client.gateway.proxy.GatewayStatusApi;
import com.optio3.cloud.gateway.GatewayApplication;
import com.optio3.cloud.gateway.GatewayConfiguration;
import com.optio3.cloud.messagebus.WellKnownDestination;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.cloud.messagebus.channel.RpcWorker;
import com.optio3.infra.NetworkHelper;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.util.TimeUtils;

public class Heartbeat extends RpcWorker.BaseHeartbeat<GatewayApplication, GatewayConfiguration>
{
    private final Set<String>   m_supportedFeatures = Sets.newHashSet();
    private       ZonedDateTime m_nextGc;

    public Heartbeat(GatewayApplication app,
                     RpcWorker rpcWorker,
                     Runnable callbackBefore,
                     Runnable callbackAfter)
    {
        super(app, GatewayApplication.LoggerInstance, rpcWorker, callbackBefore, callbackAfter);

        m_nextGc = computeNextGcTime();

        addFeature(GatewayFeature.SamplingConfigurationId);
    }

    @Override
    protected CompletableFuture<Duration> sendCheckinInner(boolean force) throws
                                                                          Exception
    {
        ZonedDateTime now = TimeUtils.now();
        if (now.isAfter(m_nextGc))
        {
            System.gc();
            m_nextGc = computeNextGcTime();
        }

        GatewayStatus        status = new GatewayStatus();
        GatewayConfiguration cfg    = m_app.getServiceNonNull(GatewayConfiguration.class);

        status.instanceId        = cfg.instanceId;
        status.supportedFeatures = m_supportedFeatures;

        Runtime runtime = Runtime.getRuntime();
        status.availableProcessors = runtime.availableProcessors();
        status.maxMemory           = runtime.maxMemory();
        status.freeMemory          = runtime.freeMemory();
        status.totalMemory         = runtime.totalMemory();

        final FirmwareHelper firmwareHelper = FirmwareHelper.get();
        status.hardwareVersion = firmwareHelper.getHardwareVersion();
        status.firmwareVersion = firmwareHelper.getFirmwareVersion();

        // Don't include interfaces without broadcast, like PPP interface, since their address changes frequently.
        for (NetworkHelper.InterfaceAddressDetails itfDetails : NetworkHelper.listNetworkAddresses(false, false, false, false, null))
        {
            status.networkInterfaces.put(itfDetails.networkInterface.getName(), itfDetails.cidr.toString());
        }

        GatewayState state = getState();
        state.exportNetworks(status.networks);
        status.queueStatus = state.checkQueueStatus();

        RpcClient        client = await(m_app.getRpcClient(20, TimeUnit.SECONDS));
        GatewayStatusApi proxy  = client.createProxy(WellKnownDestination.Service.getId(), null, GatewayStatusApi.class, 20, TimeUnit.SECONDS);

        await(proxy.checkin(status));

        Duration nextChecking = null;
        if (status.queueStatus.numberOfBatches > 0)
        {
            nextChecking = Duration.ofMinutes(5);
        }

        return wrapAsync(nextChecking);
    }

    //--//

    private GatewayState getState()
    {
        return m_app.getServiceNonNull(GatewayState.class);
    }

    private static ZonedDateTime computeNextGcTime()
    {
        ZonedDateTime now  = TimeUtils.now();
        ZonedDateTime when = now.withMinute(56); // Schedule the GC for the last minutes of an hour, it's a quiet time usually.

        if (when.isBefore(now))
        {
            when = when.plus(1, ChronoUnit.HOURS);
        }

        return when;
    }

    private void addFeature(GatewayFeature feature)
    {
        m_supportedFeatures.add(feature.name());
    }
}
