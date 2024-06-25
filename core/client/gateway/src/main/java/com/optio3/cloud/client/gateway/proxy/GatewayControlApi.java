/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.proxy;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.optio3.cloud.client.Optio3RemotableProxy;
import com.optio3.cloud.client.gateway.model.GatewayOperationStatus;
import com.optio3.cloud.client.gateway.model.GatewayOperationToken;
import com.optio3.logging.LoggerConfiguration;

@Optio3RemotableProxy
public interface GatewayControlApi
{
    CompletableFuture<Void> flushHeartbeat() throws
                                             Exception;

    CompletableFuture<List<String>> dumpThreads(boolean includeMemInfo) throws
                                                                        Exception;

    //--//

    CompletableFuture<GatewayOperationStatus> checkOperation(GatewayOperationToken token) throws
                                                                                          Exception;

    CompletableFuture<Void> cancelOperation(GatewayOperationToken token) throws
                                                                         Exception;

    //--//

    CompletableFuture<List<LoggerConfiguration>> getLoggers() throws
                                                              Exception;

    CompletableFuture<LoggerConfiguration> configLogger(LoggerConfiguration cfg) throws
                                                                                 Exception;
}
