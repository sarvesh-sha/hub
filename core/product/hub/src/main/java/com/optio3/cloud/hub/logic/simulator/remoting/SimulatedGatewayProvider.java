/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.simulator.remoting;

import java.lang.reflect.Type;

import javax.inject.Singleton;

import com.optio3.cloud.hub.logic.simulator.SimulatedGateway;
import org.glassfish.jersey.internal.inject.Injectee;
import org.glassfish.jersey.internal.inject.InjectionResolver;

@Singleton
public class SimulatedGatewayProvider implements InjectionResolver<Optio3SimulatedGateway>
{
    private static final ThreadLocal<SimulatedGateway> s_gateway = new ThreadLocal<>();

    public static void set(SimulatedGateway gateway)
    {
        s_gateway.set(gateway);
    }

    //--//

    @Override
    public Object resolve(Injectee injectee)
    {
        Type t = injectee.getRequiredType();

        if (t == SimulatedGateway.class)
        {
            SimulatedGateway gateway = s_gateway.get();
            if (gateway == null)
            {
                throw new RuntimeException("Invalid context for @Optio3SimulatedGateway");
            }

            return gateway;
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
    public Class<Optio3SimulatedGateway> getAnnotation()
    {
        return Optio3SimulatedGateway.class;
    }
}
