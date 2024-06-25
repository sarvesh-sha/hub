/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.remoting;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class ProxyForRemoteInvocation implements InvocationHandler
{
    private final static Object[] s_emptyArgs = new Object[0];

    private final CallMarshaller m_marshaller;
    private final CallDispatcher m_dispatcher;
    private final Type           m_itf;

    public ProxyForRemoteInvocation(CallMarshaller marshaller,
                                    CallDispatcher dispatcher,
                                    Type itf)
    {
        m_marshaller = marshaller;
        m_dispatcher = dispatcher;
        m_itf = itf;
    }

    @Override
    public Object invoke(Object proxy,
                         Method method,
                         Object[] args) throws
                                        Throwable
    {
        if (args == null)
        {
            args = s_emptyArgs;
        }

        Class<?> declaringClass = method.getDeclaringClass();

        // Call methods on Object class directly.
        if (Object.class == declaringClass)
        {
            return method.invoke(this, args);
        }

        RemoteCallDescriptor rc = m_marshaller.encode(m_itf, method, args);

        return m_dispatcher.send(rc, args);
    }
}
