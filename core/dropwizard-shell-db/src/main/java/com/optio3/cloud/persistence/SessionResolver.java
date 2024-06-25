/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.lang.reflect.Type;

import javax.inject.Singleton;

import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.serialization.Reflection;
import com.optio3.service.IServiceProvider;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.internal.inject.Injectee;
import org.glassfish.jersey.internal.inject.InjectionResolver;

@Singleton
public class SessionResolver implements InjectionResolver<Optio3Dao>
{
    private final IServiceProvider m_serviceProvider;

    public SessionResolver(IServiceProvider serviceProvider)
    {
        m_serviceProvider = serviceProvider;
    }

    @Override
    public Object resolve(Injectee injectee)
    {
        Type t = injectee.getRequiredType();

        if (Reflection.isSubclassOf(SessionProvider.class, t))
        {
            Optio3Dao anno = injectee.getParent()
                                     .getAnnotation(Optio3Dao.class);

            String dataSource = anno.value();
            if (StringUtils.isEmpty(dataSource))
            {
                dataSource = null;
            }

            // Always throttle access to DB from REST requests.
            return new SessionProvider(m_serviceProvider, dataSource, Optio3DbRateLimiter.Normal);
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
    public Class<Optio3Dao> getAnnotation()
    {
        return Optio3Dao.class;
    }
}
