/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.channel;

import java.lang.reflect.Type;

import javax.inject.Singleton;

import com.optio3.cloud.annotation.Optio3RemoteOrigin;
import org.glassfish.jersey.internal.inject.Injectee;
import org.glassfish.jersey.internal.inject.InjectionResolver;

/**
 * A dependency injector for the Identity of a remote call.
 *
 * To use, add this to your RPC implementation, and you'll be able to get the RPC ID of the caller:
 *
 * <pre>
 * &#64;Optio3RemoteOrigin
 * private RpcOrigin m_origin;
 * </pre>
 */
@Singleton
public class RemoteOriginProvider implements InjectionResolver<Optio3RemoteOrigin>
{
    private static final ThreadLocal<RpcOrigin> s_origin = new ThreadLocal<>();

    public static void set(RpcOrigin origin)
    {
        s_origin.set(origin);
    }

    //--//

    @Override
    public Object resolve(Injectee injectee)
    {
        Type t = injectee.getRequiredType();

        if (t == RpcOrigin.class)
        {
            RpcOrigin rpcOrigin = s_origin.get();
            if (rpcOrigin == null)
            {
                throw new RuntimeException("Invalid context for @Optio3RemoteOrigin");
            }

            return rpcOrigin;
        }

        return null;
    }

    @Override
    public boolean isConstructorParameterIndicator()
    {
        return false;
    }

    @Override
    public boolean isMethodParameterIndicator()
    {
        return false;
    }

    @Override
    public Class<Optio3RemoteOrigin> getAnnotation()
    {
        return Optio3RemoteOrigin.class;
    }
}
