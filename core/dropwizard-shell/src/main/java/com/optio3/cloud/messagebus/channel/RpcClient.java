/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.channel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.messagebus.MessageBusChannelSubscriber;
import com.optio3.cloud.messagebus.payload.MbData_Message;
import com.optio3.cloud.messagebus.payload.rpc.RpcMessage;
import com.optio3.cloud.messagebus.payload.rpc.RpcMessageReply;

public class RpcClient extends MessageBusChannelSubscriber<RpcMessage, RpcMessageReply>
{

    private final RpcController m_controller;

    //--//

    public RpcClient(RpcContext rpcContext)
    {
        super(RpcChannel.class);

        m_controller = new RpcController(rpcContext, this);
    }

    //--//

    @Override
    protected CompletableFuture<Void> receivedMessage(MbData_Message data,
                                                      RpcMessage obj) throws
                                                                      Exception
    {
        return m_controller.processRpcRequest(data, obj);
    }

    //--//

    public CompletableFuture<Boolean> waitForDestination(String destination,
                                                         int timeout,
                                                         TimeUnit unit)
    {
        return m_controller.waitForDestination(destination, timeout, unit);
    }

    public <P> P createProxy(String destinationHost,
                             String destinationInstance,
                             Class<P> itf,
                             int timeout,
                             TimeUnit timeoutUnit)
    {
        return m_controller.createProxy(destinationHost, destinationInstance, itf, timeout, timeoutUnit);
    }

    public void registerInstance(String instanceId,
                                 Object target)
    {
        m_controller.registerInstance(instanceId, target);
    }

    public void unregisterInstance(String instanceId)
    {
        m_controller.unregisterInstance(instanceId);
    }
}
