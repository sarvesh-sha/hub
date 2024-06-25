/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.remoting;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import org.glassfish.jersey.internal.inject.InjectionManager;

public class LocalCall
{
    public final Type     proxy;
    public final Method   method;
    public final Object[] args;

    public final Type returnType;

    public LocalCall(Type proxy,
                     Method method,
                     Type returnType,
                     Object... args)
    {
        requireNonNull(proxy);
        requireNonNull(method);
        requireNonNull(args);

        int expected = method.getParameterTypes().length;
        if (expected != args.length)
        {
            throw Exceptions.newRuntimeException("Wrong number of parameters (%d, expecting %d) when remoting call to %s", args.length, expected, method);
        }

        this.proxy = proxy;
        this.method = method;
        this.returnType = returnType;
        this.args = args;
    }

    public Object instantiateTarget(InjectionManager injectionManager)
    {
        return injectionManager.createAndInitialize(Reflection.getRawType(proxy));
    }

    public Object invoke(Object targetObject) throws
                                              Throwable
    {
        try
        {
            return method.invoke(targetObject, args);
        }
        catch (InvocationTargetException e)
        {
            throw e.getTargetException();
        }
    }
}
