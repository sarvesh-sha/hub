/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.authentication.jwt;

import java.security.Key;
import java.time.Duration;
import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.NewCookie;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

public class CookieAuthBundle<C extends Configuration> implements ConfiguredBundle<C>
{
    private static final String DEFAULT_COOKIE_NAME = "sessionToken";

    private final CookieAuthConfiguration     m_conf;
    private final CookiePrincipalRoleResolver m_resolver;
    private       CookieAuthRequestFilter     m_authRequestFilter;
    private       CookieAuthResponseFilter    m_authResponseFilter;

    public CookieAuthBundle(CookieAuthConfiguration conf,
                            CookiePrincipalRoleResolver resolver)
    {
        if (conf == null)
        {
            conf = new CookieAuthConfiguration();
        }

        m_conf = conf;
        m_resolver = Objects.requireNonNull(resolver);
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap)
    {
        //
        // Prep the object mapper in case somebody needs to serialize a CookiePrincipal.
        //
        SimpleModule model = new SimpleModule().addAbstractTypeMapping(Claims.class, DefaultClaims.class);
        bootstrap.getObjectMapper()
                 .registerModule(model);
    }

    @Override
    public void run(C configuration,
                    Environment environment)
    {
        Key      key                = CookiePrincipal.generateKey(m_conf.getSecretSeed());
        Duration volatileValidity   = Duration.parse(m_conf.getSessionExpiryVolatile());
        Duration persistentValidity = Duration.parse(m_conf.getSessionExpiryPersistent());

        JerseyEnvironment jerseyEnvironment = environment.jersey();

        //--//

        //
        // Create an AuthFilter that uses JWT and BasicCredentials to associate a Principal with the request.
        //
        CookieAuthRequestFilter.Builder filter = new CookieAuthRequestFilter.Builder();
        filter.setCookieName(DEFAULT_COOKIE_NAME);

        filter.setAuthenticator(new CookiePrincipalAuthenticator(key, m_resolver));

        filter.setAuthorizer(CookiePrincipal::isInRole);

        m_authRequestFilter = filter.buildAuthFilter();

        //--//

        //
        // Register handler that associates the AuthFilter with all resources, unless they are marked with @Optio3NoAuthenticationNeeded.
        //
        jerseyEnvironment.register(new CookiePrincipalAuthenticator.CookieAuthDynamicFeature(m_authRequestFilter));

        //
        // Register handler for @RolesAllowed, @DenyAll, @PermitAll.
        //
        jerseyEnvironment.register(RolesAllowedDynamicFeature.class);

        //
        // Register filter that converts a Principal into a JWT cookie.
        //
        m_authResponseFilter = new CookieAuthResponseFilter(DEFAULT_COOKIE_NAME, m_conf.isSecure(), m_conf.isHttpOnly(), key, volatileValidity, persistentValidity);
        jerseyEnvironment.register(m_authResponseFilter);

        //
        // Finally register our AuthFilter, so it gets called to manage security at the request level.
        //
        jerseyEnvironment.register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(m_authRequestFilter).to(CookieAuthRequestFilter.class);
            }
        });
    }

    public CookieAuthRequestFilter getAuthRequestFilter()
    {
        return m_authRequestFilter;
    }

    public @NotNull CookiePrincipal buildPrincipal(String subject)
    {
        return CookiePrincipal.newInstance(m_resolver, subject);
    }

    public NewCookie generateCookie(CookiePrincipal principal)
    {
        return m_authResponseFilter.generateCookie(principal);
    }
}
