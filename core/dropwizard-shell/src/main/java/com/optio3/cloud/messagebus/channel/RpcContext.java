/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.channel;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;
import com.optio3.cloud.remoting.CallMarshaller;
import com.optio3.util.Exceptions;
import org.glassfish.jersey.internal.inject.InjectionManager;

public abstract class RpcContext
{
    private final ConcurrentMap<Object, Object> m_registeredObjects = Maps.newConcurrentMap();

    public abstract CallMarshaller getCallMarshaller();

    public abstract InjectionManager getInjectionManager();

    public Object resolveInstance(String instanceId)
    {
        Object target = m_registeredObjects.get(instanceId);
        if (target == null)
        {
            throw Exceptions.newIllegalArgumentException("Unknown instance ID '%s'", instanceId);
        }

        return target;
    }

    public void registerInstance(String instanceId,
                                 Object target)
    {
        m_registeredObjects.put(instanceId, target);
    }

    public void unregisterInstance(String instanceId)
    {
        m_registeredObjects.remove(instanceId);
    }
}
