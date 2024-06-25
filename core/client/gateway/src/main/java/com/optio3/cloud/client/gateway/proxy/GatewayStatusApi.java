/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.proxy;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.optio3.cloud.client.Optio3RemotableProxy;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayStatus;
import com.optio3.cloud.client.gateway.model.LogEntry;

@Optio3RemotableProxy
public interface GatewayStatusApi
{
    CompletableFuture<Void> checkin(GatewayStatus status) throws
                                                          Exception;

    CompletableFuture<Void> publishResults(List<GatewayDiscoveryEntity> entities) throws
                                                                                  Exception;

    CompletableFuture<Void> publishLog(String instanceId,
                                       List<LogEntry> entries) throws
                                                               Exception;
}
