/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet;

import java.util.function.BiConsumer;

import com.optio3.protocol.bacnet.model.pdu.application.ServiceCommon;
import com.optio3.serialization.Reflection;

public class ServiceListenerHandle<T extends ServiceCommon> implements AutoCloseable
{
    private final BACnetManager                 m_manager;
    private final Class<T>                      m_clz;
    private final BiConsumer<T, ServiceContext> m_callback;

    ServiceListenerHandle(BACnetManager manager,
                          Class<T> clz,
                          BiConsumer<T, ServiceContext> callback)
    {
        m_manager  = manager;
        m_clz      = clz;
        m_callback = callback;
    }

    @Override
    public void close()
    {
        m_manager.removeListener(m_clz, this);
    }

    void invoke(ServiceCommon msg,
                ServiceContext sc)
    {
        T msg2 = Reflection.as(msg, m_clz);
        if (msg2 != null)
        {
            m_callback.accept(msg2, sc);
        }
    }
}
