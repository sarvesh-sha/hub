/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.authentication.jwt;

import java.security.Key;
import java.util.Optional;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

import com.optio3.cloud.annotation.Optio3NoAuthenticationNeeded;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import io.jsonwebtoken.JwtException;
import org.glassfish.jersey.server.model.AnnotatedMethod;

public class CookiePrincipalAuthenticator implements Authenticator<Object, CookiePrincipal>
{
    private final Key                         m_key;
    private final CookiePrincipalRoleResolver m_resolver;

    CookiePrincipalAuthenticator(Key key,
                                 CookiePrincipalRoleResolver resolver)
    {
        m_key = key;
        m_resolver = resolver;
    }

    @Override
    public Optional<CookiePrincipal> authenticate(Object credentials)
    {
        String token = Reflection.as(credentials, String.class);
        if (token != null)
        {
            try
            {
                CookiePrincipal principal = CookiePrincipal.decode(m_resolver, m_key, token);

                if (m_resolver.stillValid(principal))
                {
                    return Optional.of(principal);
                }
            }
            catch (JwtException e)
            {
            }
        }

        BasicCredentials basic = Reflection.as(credentials, BasicCredentials.class);
        if (basic != null)
        {
            String username = basic.getUsername();

            CookiePrincipal principal = CookiePrincipal.newInstance(m_resolver, username);

            if (m_resolver.authenticate(principal, basic.getPassword()))
            {
                principal.setPersistent(true);

                return Optional.of(principal);
            }
        }

        return Optional.empty();
    }

    //--//

    static class CookieAuthDynamicFeature extends AuthDynamicFeature
    {
        private final ContainerRequestFilter m_authFilter;

        public CookieAuthDynamicFeature(ContainerRequestFilter authFilter)
        {
            super(authFilter);

            m_authFilter = authFilter;
        }

        @Override
        public void configure(ResourceInfo resourceInfo,
                              FeatureContext context)
        {
            final AnnotatedMethod am = new AnnotatedMethod(resourceInfo.getResourceMethod());

            //
            // If class or method has the @Optio3NoAuthenticationNeeded annotation, we don't register the auth filter.
            //
            final boolean annotationOnClass = resourceInfo.getResourceClass()
                                                          .isAnnotationPresent(Optio3NoAuthenticationNeeded.class);
            final boolean annotationOnMethod = am.isAnnotationPresent(Optio3NoAuthenticationNeeded.class);

            if (annotationOnClass || annotationOnMethod)
            {
                super.configure(resourceInfo, context);
            }
            else
            {
                if (resourceInfo.getResourceMethod()
                                .getAnnotation(OPTIONS.class) != null)
                {
                    throw Exceptions.newIllegalArgumentException("Resource methods cannot be using the OPTIONS method and have authentication, it affects CORS: %s", resourceInfo);
                }

                context.register(m_authFilter);
            }
        }
    }
}
