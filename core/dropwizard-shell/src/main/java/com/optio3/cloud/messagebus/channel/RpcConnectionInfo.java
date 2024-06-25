/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.channel;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.AbstractApplication;
import com.optio3.service.IServiceProvider;
import com.optio3.util.Exceptions;

public class RpcConnectionInfo
{
    public String hostDisplayName;
    public String hostId;
    public String instanceId;
    public String rpcId;

    public CompletableFuture<RpcClient> checkIfOnline(IServiceProvider serviceProvider,
                                                      int timeout,
                                                      TimeUnit unit) throws
                                                                     Exception
    {
        if (rpcId != null)
        {
            AbstractApplication<?> app    = serviceProvider.getServiceNonNull(AbstractApplication.class);
            RpcClient              client = await(app.getRpcClient(timeout, unit));

            if (await(client.waitForDestination(rpcId, timeout, unit)))
            {
                return wrapAsync(client);
            }
        }

        return wrapAsync(null);
    }

    public <T> CompletableFuture<T> getProxyOrNull(IServiceProvider serviceProvider,
                                                   Class<T> clz,
                                                   int proxyTimeoutInSeconds) throws
                                                                              Exception
    {
        try
        {
            T proxy = await(getProxy(serviceProvider, clz, proxyTimeoutInSeconds));
            return wrapAsync(proxy);
        }
        catch (Throwable t)
        {
            return wrapAsync(null);
        }
    }

    public <T> CompletableFuture<T> getProxy(IServiceProvider serviceProvider,
                                             Class<T> clz,
                                             int proxyTimeoutInSeconds) throws
                                                                        Exception
    {
        if (rpcId == null)
        {
            throw Exceptions.newTimeoutException("No RPC ID for '%s / %s'", hostDisplayName, instanceId);
        }

        RpcClient client = await(checkIfOnline(serviceProvider, 20, TimeUnit.SECONDS));
        if (client == null)
        {
            throw Exceptions.newTimeoutException("Can't connect to endpoint '%s / %s' with instance '%s'", hostDisplayName, instanceId, clz.getName());
        }

        return wrapAsync(client.createProxy(rpcId, null, clz, proxyTimeoutInSeconds, TimeUnit.SECONDS));
    }
}
