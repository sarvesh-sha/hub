/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

import com.google.common.collect.Lists;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.exception.DetailedApplicationException;
import io.dropwizard.auth.AuthFilter;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;

@SuppressWarnings("serial")
abstract class WebSocketWrapper extends WebSocketServlet
{
    public static final int MaxMessageSize = 1 * 1024 * 1024;

    //
    // Unfortunately, we have to use a Thread-local property to pass the security context from the wrapper to the socket implementation.
    //
    private static final ThreadLocal<CookiePrincipal> s_principal = new ThreadLocal<>();

    private final long                                    m_timeout;
    private final AuthFilter<?, ?>                        m_authFilter;
    private final Optio3RequestLogFactory.ResourceTracker m_tracker;

    WebSocketWrapper(long timeout,
                     AuthFilter<?, ?> authFilter,
                     Class<?> servletClass,
                     String servletPath,
                     ConcurrentMap<String, RequestStatistics> requestStats)
    {
        m_timeout = timeout;
        m_authFilter = authFilter;

        m_tracker = new Optio3RequestLogFactory.ResourceTracker(servletClass, servletPath, requestStats);
    }

    @Override
    public void configure(WebSocketServletFactory factory)
    {
        final WebSocketPolicy policy = factory.getPolicy();
        policy.setIdleTimeout(m_timeout);
        policy.setMaxTextMessageSize(MaxMessageSize);
        policy.setMaxBinaryMessageSize(MaxMessageSize);

        // register the class as the WebSocket to create on Upgrade
        factory.setCreator(new WebSocketCreator()
        {
            @Override
            public Object createWebSocket(ServletUpgradeRequest req,
                                          ServletUpgradeResponse resp)
            {
                return createNewInstance();
            }
        });
    }

    protected abstract Object createNewInstance();

    @Override
    protected void service(HttpServletRequest request,
                           HttpServletResponse response) throws
                                                         ServletException,
                                                         IOException
    {
        CookiePrincipal principal;

        if (m_authFilter != null)
        {
            try
            {
                principal = performAuthentication(request, response);
            }
            catch (final IllegalArgumentException iae)
            {
                final Response.Status badRequest = Response.Status.BAD_REQUEST;
                response.sendError(badRequest.getStatusCode(), badRequest.getReasonPhrase());
                return;
            }
            catch (DetailedApplicationException ex)
            {
                response.sendError(Status.PRECONDITION_FAILED.getStatusCode(), ex.getMessage());
                return;
            }
        }
        else
        {
            principal = CookiePrincipal.createEmpty();
        }

        s_principal.set(principal);

        try
        {
            m_tracker.track(request);
            super.service(request, response);
        }
        finally
        {
            s_principal.set(null);
        }
    }

    //--//

    public static @NotNull CookiePrincipal getPrincipal()
    {
        CookiePrincipal principal = s_principal.get();
        return principal != null ? principal : CookiePrincipal.createEmpty();
    }

    private @NotNull CookiePrincipal performAuthentication(HttpServletRequest request,
                                                           HttpServletResponse response) throws
                                                                                         IOException
    {
        URI requestUri;
        URI baseUri;

        final UriBuilder absoluteUriBuilder = UriBuilder.fromUri(request.getRequestURL()
                                                                        .toString());

        requestUri = absoluteUriBuilder.build();
        baseUri = absoluteUriBuilder.replacePath(request.getContextPath())
                                    .path("/")
                                    .build();

        final ContainerRequest requestContext = new ContainerRequest(baseUri, requestUri, request.getMethod(), getSecurityContext(request), getPropertiesDelegate(request));

        addRequestHeaders(request, requestContext);

        m_authFilter.filter(requestContext);

        SecurityContext secCtx = requestContext.getSecurityContext();
        if (secCtx != null)
        {
            Principal pri = secCtx.getUserPrincipal();
            if (pri instanceof CookiePrincipal)
            {
                return (CookiePrincipal) pri;
            }
        }

        return CookiePrincipal.createEmpty();
    }

    private SecurityContext getSecurityContext(HttpServletRequest request)
    {
        return new SecurityContext()
        {
            @Override
            public Principal getUserPrincipal()
            {
                return request.getUserPrincipal();
            }

            @Override
            public boolean isUserInRole(final String role)
            {
                return request.isUserInRole(role);
            }

            @Override
            public boolean isSecure()
            {
                return request.isSecure();
            }

            @Override
            public String getAuthenticationScheme()
            {
                return request.getAuthType();
            }
        };
    }

    private static PropertiesDelegate getPropertiesDelegate(HttpServletRequest request)
    {
        return new PropertiesDelegate()
        {
            @Override
            public Object getProperty(String name)
            {
                return request.getAttribute(name);
            }

            @Override
            public Collection<String> getPropertyNames()
            {
                List<String> res = Lists.newArrayList();

                Enumeration<String> names = request.getAttributeNames();
                while (names.hasMoreElements())
                {
                    res.add(names.nextElement());
                }

                return res;
            }

            @Override
            public void setProperty(String name,
                                    Object object)
            {
                request.setAttribute(name, object);
            }

            @Override
            public void removeProperty(String name)
            {
                request.removeAttribute(name);
            }
        };
    }

    private static void addRequestHeaders(final HttpServletRequest request,
                                          final ContainerRequest requestContext)
    {
        final Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements())
        {
            final String name = names.nextElement();

            final Enumeration<String> values = request.getHeaders(name);
            while (values.hasMoreElements())
            {
                final String value = values.nextElement();
                if (value != null)
                {
                    requestContext.header(name, value);
                }
            }
        }
    }
}
