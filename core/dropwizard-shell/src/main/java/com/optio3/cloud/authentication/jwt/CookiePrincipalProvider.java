/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.authentication.jwt;

import java.lang.reflect.Type;

import javax.inject.Singleton;

import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.serialization.Reflection;
import com.optio3.service.IServiceProvider;
import org.glassfish.jersey.internal.inject.Injectee;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InjectionResolver;

@Singleton
public class CookiePrincipalProvider implements InjectionResolver<Optio3Principal>
{
    private final IServiceProvider m_serviceProvider;

    public CookiePrincipalProvider(IServiceProvider serviceProvider)
    {
        m_serviceProvider = serviceProvider;
    }

    @Override
    public Object resolve(Injectee injectee)
    {
        Type t = injectee.getRequiredType();

        if (Reflection.isSubclassOf(CookiePrincipalAccessor.class, t))
        {
            InjectionManager injectionManager = m_serviceProvider.getServiceNonNull(InjectionManager.class);

            return injectionManager.createAndInitialize(CookiePrincipalAccessor.class);
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
        return true;
    }

    @Override
    public Class<Optio3Principal> getAnnotation()
    {
        return Optio3Principal.class;
    }
}
