/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.simulator.remoting;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.lang.management.ThreadInfo;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.optio3.cloud.annotation.Optio3RemotableEndpoint;
import com.optio3.cloud.client.gateway.model.GatewayOperationStatus;
import com.optio3.cloud.client.gateway.model.GatewayOperationToken;
import com.optio3.cloud.client.gateway.proxy.GatewayControlApi;
import com.optio3.logging.LoggerConfiguration;
import com.optio3.util.StackTraceAnalyzer;

@Optio3RemotableEndpoint(itf = GatewayControlApi.class)
public class SimulatedGatewayControlApiImpl extends CommonSimulatedGatewayApiImpl implements GatewayControlApi
{
    @Override
    public CompletableFuture<Void> flushHeartbeat()
    {
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
        return wrapAsync(Collections.emptyList());
    }

    @Override
    public CompletableFuture<LoggerConfiguration> configLogger(LoggerConfiguration cfg)
    {
        return wrapAsync(cfg);
    }
}