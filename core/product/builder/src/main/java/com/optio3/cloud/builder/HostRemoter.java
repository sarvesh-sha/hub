/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder;

import java.util.concurrent.TimeUnit;

import com.optio3.cloud.builder.persistence.worker.HostBoundResource;
import com.optio3.cloud.messagebus.WellKnownDestination;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.service.IServiceProvider;

public class HostRemoter implements IServiceProvider
{
    private final IServiceProvider m_serviceProvider;
    private final RpcClient        m_client;

    public HostRemoter(IServiceProvider serviceProvider,
                       RpcClient client)
    {
        m_serviceProvider = serviceProvider;
        m_client = client;
    }

    public <P> P createRemotableProxy(HostBoundResource context,
                                      Class<P> itf)
    {
        // For now, just create a local proxy.
        return m_client.createProxy(WellKnownDestination.Service.getId(), null, itf, 3600, TimeUnit.SECONDS);
    }

    @Override
    public <S> S getService(Class<S> serviceClass)
    {
        return m_serviceProvider.getService(serviceClass);
    }
}
