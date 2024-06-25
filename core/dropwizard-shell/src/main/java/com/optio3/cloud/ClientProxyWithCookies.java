/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.optio3.cloud.exception.DetailedApplicationException;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.InvocationHandlerAware;

class ClientProxyWithCookies implements InvocationHandlerAware,
                                        InvocationHandler
{
    private final CookieProvider m_provider;
    private final Client         m_client;
    private final Class<?>       m_cls;

    public ClientProxyWithCookies(CookieProvider provider,
                                  Client client,
                                  Class<?> cls)
    {
        m_provider = provider;
        m_client = client;
        m_cls = cls;
    }

    @Override
    public Object getInvocationHandler()
    {
        return ((InvocationHandlerAware) m_client).getInvocationHandler();
    }

    @Override
    public Object invoke(Object proxy,
                         Method method,
                         Object[] args) throws
                                        Throwable
    {
        Class<?> declaringClass = method.getDeclaringClass();

        if (InvocationHandlerAware.class == declaringClass || Object.class == declaringClass)
        {
            return method.invoke(this, args);
        }

        //
        // The Apache CXF implementation of the REST proxy does not support cookie management.
        // We have to inject and retrieve cookies before/after jumping into the Apache proxy.
        //
        try
        {
            // Inject cookies.
            if (declaringClass == m_cls)
            {
                m_provider.setCookies(m_client);
            }

            Object obj = method.invoke(m_client, args);

            // Retrieve cookies.
            if (declaringClass == m_cls)
            {
                m_provider.updateCookies(m_client);
            }

            return obj;
        }
        catch (InvocationTargetException e)
        {
            Throwable t = e.getTargetException();

            t = DetailedApplicationException.tryAndDecode(t);

            throw t;
        }
    }
}
