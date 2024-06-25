/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.proxy;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.optio3.cloud.client.Optio3RemotableProxy;
import com.optio3.cloud.client.gateway.model.LogEntry;
import com.optio3.cloud.client.gateway.model.prober.ProberNetworkStatus;
import com.optio3.cloud.client.gateway.model.prober.ProberOperation;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationStatus;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationToken;
import com.optio3.util.function.FunctionWithException;

@Optio3RemotableProxy
public interface GatewayProberControlApi
{
    CompletableFuture<ProberNetworkStatus> checkNetwork() throws
                                                          Exception;

    CompletableFuture<ProberOperationToken> executeOperation(ProberOperation input) throws
                                                                                    Exception;

    CompletableFuture<ProberOperationStatus> checkOperation(ProberOperationToken token,
                                                            FunctionWithException<List<LogEntry>, CompletableFuture<Void>> output) throws
                                                                                                                                   Exception;

    CompletableFuture<Void> cancelOperation(ProberOperationToken token);

    CompletableFuture<ProberOperation.BaseResults> getOperationResults(ProberOperationToken token);
}
