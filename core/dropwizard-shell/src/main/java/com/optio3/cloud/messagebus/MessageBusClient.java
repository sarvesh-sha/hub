/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

import java.util.concurrent.CompletableFuture;

import com.optio3.cloud.messagebus.channel.RpcWorker;
import com.optio3.cloud.messagebus.transport.DataTransport;

public interface MessageBusClient
{
    String describeConnection();

    MessageBusStatistics sampleStatistics();

    boolean isConnectionOpen();

    void startConnection() throws
                           Exception;

    CompletableFuture<Void> onDisconnected() throws
                                             Exception;

    void closeConnection();

    //--//

    CompletableFuture<String> getEndpointId() throws
                                              Exception;

    boolean shouldUpgrade();

    boolean prepareUpgrade(RpcWorker rpcWorker);

    //--//

    <TRequest, TReply> CompletableFuture<Boolean> join(MessageBusChannelSubscriber<TRequest, TReply> inboundPath) throws
                                                                                                                  Exception;

    <TRequest> CompletableFuture<Boolean> leave(DataTransport<TRequest> inboundPath) throws
                                                                                     Exception;
}
