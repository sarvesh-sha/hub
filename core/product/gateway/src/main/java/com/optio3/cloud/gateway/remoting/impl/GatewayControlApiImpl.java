/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway.remoting.impl;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.lang.management.ThreadInfo;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import com.optio3.cloud.annotation.Optio3RemotableEndpoint;
import com.optio3.cloud.client.gateway.model.GatewayOperationStatus;
import com.optio3.cloud.client.gateway.model.GatewayOperationToken;
import com.optio3.cloud.client.gateway.proxy.GatewayControlApi;
import com.optio3.cloud.gateway.GatewayApplication;
import com.optio3.cloud.gateway.logic.ProberForBACnet;
import com.optio3.cloud.gateway.logic.ProberForCANbus;
import com.optio3.cloud.gateway.logic.ProberForIpn;
import com.optio3.logging.LoggerConfiguration;
import com.optio3.logging.LoggerFactory;
import com.optio3.logging.Severity;
import com.optio3.protocol.bacnet.BACnetManager;
import com.optio3.protocol.bacnet.DeviceIdentity;
import com.optio3.protocol.can.CanManager;
import com.optio3.protocol.epsolar.EpSolarManager;
import com.optio3.protocol.ipn.IpnManager;
import com.optio3.protocol.modbus.ModbusManager;
import com.optio3.protocol.obdii.J1939Manager;
import com.optio3.protocol.stealthpower.StealthPowerManager;
import com.optio3.protocol.tristar.TriStarManager;
import com.optio3.util.StackTraceAnalyzer;

@Optio3RemotableEndpoint(itf = GatewayControlApi.class)
public class GatewayControlApiImpl extends CommonGatewayApiImpl implements GatewayControlApi
{
    @Inject
    private GatewayApplication m_app;

    @Override
    public CompletableFuture<Void> flushHeartbeat()
    {
        m_app.flushHeartbeat(true);

        return wrapAsync(null);
    }

    @Override
    public CompletableFuture<List<String>> dumpThreads(boolean includeMemInfo)
    {
        Map<StackTraceAnalyzer, List<ThreadInfo>> uniqueStackTraces = StackTraceAnalyzer.allThreadInfos();
        List<String>                              lines             = StackTraceAnalyzer.printThreadInfos(includeMemInfo, uniqueStackTraces);

        return wrapAsync(lines);
    }

    //--//

    @Override
    public CompletableFuture<GatewayOperationStatus> checkOperation(GatewayOperationToken token) throws
                                                                                                 Exception
    {
        return wrapAsync(getTracker().checkOperationStatus(token));
    }

    @Override
    public CompletableFuture<Void> cancelOperation(GatewayOperationToken token)
    {
        getTracker().unregister(token);

        return wrapAsync(null);
    }

    //--//

    @Override
    public CompletableFuture<List<LoggerConfiguration>> getLoggers()
    {
        // Trigger loading of classes.
        BACnetManager.LoggerInstance.isEnabled(Severity.Debug);
        CanManager.LoggerInstance.isEnabled(Severity.Debug);
        DeviceIdentity.LoggerInstance.isEnabled(Severity.Debug);
        EpSolarManager.LoggerInstance.isEnabled(Severity.Debug);
        IpnManager.LoggerInstance.isEnabled(Severity.Debug);
        J1939Manager.LoggerInstance.isEnabled(Severity.Debug);
        ModbusManager.LoggerInstance.isEnabled(Severity.Debug);
        ProberForBACnet.LoggerInstance.isEnabled(Severity.Debug);
        ProberForCANbus.LoggerInstance.isEnabled(Severity.Debug);
        ProberForIpn.LoggerInstance.isEnabled(Severity.Debug);
        StealthPowerManager.LoggerInstance.isEnabled(Severity.Debug);
        TriStarManager.LoggerInstance.isEnabled(Severity.Debug);

        return wrapAsync(LoggerFactory.getLoggersConfiguration());
    }

    @Override
    public CompletableFuture<LoggerConfiguration> configLogger(LoggerConfiguration cfg)
    {
        return wrapAsync(LoggerFactory.setLoggerConfiguration(cfg));
    }
}
