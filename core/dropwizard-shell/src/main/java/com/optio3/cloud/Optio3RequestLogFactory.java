/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import io.dropwizard.request.logging.RequestLogFactory;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;

@JsonTypeName("optio3")
public class Optio3RequestLogFactory implements RequestLogFactory<RequestLog>
{
    public static final Logger LoggerInstance = new Logger(RequestLog.class);

    private static final String c_attribute_StateTracker = "OPTIO3_STATE_TRACKER";

    //--//

    private static final ThreadLocal<RequestSettings> s_settings = new ThreadLocal<>();

    static class RequestSettings
    {
        boolean dontLog;
        boolean logOnlyOnFailure;

        ResourceTracker         resourceTracker;
        ContainerRequestContext requestContext;
        CookiePrincipal         requestPrincipal;
    }

    static class RequestLogImpl implements RequestLog
    {
        final String name;

        RequestLogImpl(String name)
        {
            this.name = name;
        }

        @Override
        public void log(Request request,
                        Response response)
        {
            RequestSettings requestSettings = (RequestSettings) request.getAttribute(c_attribute_StateTracker);
            if (requestSettings == null)
            {
                requestSettings = new RequestSettings();
            }

            String principal;

            CookiePrincipal principalRequest = requestSettings.requestPrincipal;
            if (principalRequest != null && principalRequest.isAuthenticated())
            {
                principal = principalRequest.getName();
            }
            else
            {
                ContainerRequestContext requestContext    = requestSettings.requestContext;
                CookiePrincipal         principalResponse = requestContext != null ? CookiePrincipal.getFromContext(requestContext) : null;

                if (principalResponse != null && principalResponse.isAuthenticated())
                {
                    principal = principalResponse.getName();
                }
                else
                {
                    principal = "anonymous";
                }
            }

            ResourceTracker resourceTracker = requestSettings.resourceTracker;
            Severity        level;

            final String sourceAddress = request.getRemoteAddr();
            final String protocol      = request.getProtocol();
            final String method        = request.getMethod();
            final String uri           = request.getOriginalURI();

            if (resourceTracker != null)
            {
                level = resourceTracker.logLevel;
            }
            else if (StringUtils.equalsIgnoreCase(method, "OPTIONS"))
            {
                level = Severity.Debug;
            }
            else
            {
                level = Severity.Info;
            }

            LocalDateTime now          = LocalDateTime.now();
            LocalDateTime requestStart = LocalDateTime.ofInstant(Instant.ofEpochMilli(request.getTimeStamp()), ZoneId.systemDefault());

            final int status = response.getCommittedMetaData()
                                       .getStatus();

            final long bytesRead = request.getContentRead();

            final long bytesWritten = response.getHttpChannel()
                                              .getBytesWritten();

            final long executionTime = Duration.between(requestStart, now)
                                               .toMillis();

            if (resourceTracker != null)
            {
                resourceTracker.stats.update(status, executionTime, bytesRead, bytesWritten);

                String principalPath = "/by-user/" + principal;

                var statsByUser = resourceTracker.statsMap.get(principalPath);
                if (statsByUser == null)
                {
                    statsByUser = new RequestStatistics(principalPath);
                    resourceTracker.statsMap.put(principalPath, statsByUser);
                }

                statsByUser.update(status, executionTime, bytesRead, bytesWritten);
            }

            if (status == 412 && level == Severity.Info)
            {
                // Downgrade permission failures to Debug.
                level = Severity.Debug;
            }

            boolean log = LoggerInstance.isEnabled(level);

            if (requestSettings.dontLog)
            {
                log = false;
            }

            if (requestSettings.logOnlyOnFailure && status / 100 == 2)
            {
                log = false;
            }

            if (log)
            {
                final String referrer = getHeader(request, HttpHeader.REFERER);

                if (LoggerInstance.isEnabled(Severity.Debug))
                {
                    LoggerInstance.debug("%s %-7s %-120s %d %,12d %,6dmsec | %s | %s \"%s\" \"%s\"",
                                         protocol,
                                         method,
                                         uri,
                                         status,
                                         bytesWritten,
                                         executionTime,
                                         principal,
                                         sourceAddress,
                                         referrer,
                                         getHeader(request, HttpHeader.USER_AGENT));
                }
                else
                {
                    LoggerInstance.log(null,
                                       level,
                                       null,
                                       null,
                                       "%s %-7s %-120s %d %,12d %,6dmsec | %s | %s \"%s\"",
                                       protocol,
                                       method,
                                       uri,
                                       status,
                                       bytesWritten,
                                       executionTime,
                                       principal,
                                       sourceAddress,
                                       referrer);
                }
            }
        }

        private static String getHeader(Request request,
                                        HttpHeader header)
        {
            String res = request.getHeader(header.toString());
            return StringUtils.isNotEmpty(res) ? res : "-";
        }
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

    @Override
    public RequestLog build(String name)
    {
        return new RequestLogImpl(name);
    }

    //--//

    public static void dontLog()
    {
        RequestSettings s = s_settings.get();
        if (s != null)
        {
            s.dontLog = true;
        }
    }

    public static void logOnlyOnFailure()
    {
        RequestSettings s = s_settings.get();
        if (s != null)
        {
            s.logOnlyOnFailure = true;
        }
    }

    //--//

    static class RequestLogDynamicFeature implements DynamicFeature
    {
        private final ConcurrentMap<String, RequestStatistics> stats;

        RequestLogDynamicFeature(ConcurrentMap<String, RequestStatistics> stats)
        {
            this.stats = stats;
        }

        @Override
        public void configure(ResourceInfo resourceInfo,
                              FeatureContext context)
        {
            Class<?> resourceClass  = resourceInfo.getResourceClass();
            Method   resourceMethod = resourceInfo.getResourceMethod();

            if (resourceClass.isAnnotationPresent(Path.class) && resourceMethod.isAnnotationPresent(Path.class))
            {
                ResourceTracker rt = new ResourceTracker(resourceClass, resourceMethod, stats);
                context.register(rt);
            }
        }
    }

    static class ResourceTracker implements ContainerRequestFilter,
                                            ContainerResponseFilter
    {
        private final Class<?>                                 resourceClass;
        private final Method                                   resourceMethod;
        private final Severity                                 logLevel;
        private final ConcurrentMap<String, RequestStatistics> statsMap;
        private final RequestStatistics                        stats;

        private ResourceTracker(Class<?> resourceClass,
                                Method resourceMethod,
                                ConcurrentMap<String, RequestStatistics> statsMap)
        {
            this.resourceClass  = resourceClass;
            this.resourceMethod = resourceMethod;
            this.statsMap       = statsMap;

            Optio3RequestLogLevel anno = resourceMethod.getAnnotation(Optio3RequestLogLevel.class);
            if (anno == null)
            {
                anno = resourceClass.getAnnotation(Optio3RequestLogLevel.class);
            }
            logLevel = anno != null ? anno.value() : Severity.Info;

            Path pathAnnoClass  = resourceClass.getAnnotation(Path.class);
            Path pathAnnoMethod = resourceMethod.getAnnotation(Path.class);

            stats = new RequestStatistics(pathAnnoClass.value() + "/" + pathAnnoMethod.value());

            statsMap.put(stats.path, stats);
        }

        ResourceTracker(Class<?> servletClass,
                        String servletPath,
                        ConcurrentMap<String, RequestStatistics> statsMap)
        {
            this.resourceClass  = servletClass;
            this.resourceMethod = null;
            this.statsMap       = statsMap;

            Optio3RequestLogLevel anno = resourceClass.getAnnotation(Optio3RequestLogLevel.class);
            logLevel = anno != null ? anno.value() : Severity.Info;

            stats = new RequestStatistics(servletPath);

            statsMap.put(stats.path, stats);
        }

        @Override
        public void filter(ContainerRequestContext requestContext)
        {
            RequestSettings requestSettings = new RequestSettings();
            s_settings.set(requestSettings);

            requestSettings.resourceTracker  = this;
            requestSettings.requestContext   = requestContext;
            requestSettings.requestPrincipal = CookiePrincipal.getFromContext(requestContext);

            requestContext.setProperty(c_attribute_StateTracker, requestSettings);
        }

        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext)
        {
            s_settings.set(null);
        }

        void track(HttpServletRequest req)
        {
            RequestSettings requestSettings = new RequestSettings();
            requestSettings.resourceTracker = this;

            req.setAttribute(c_attribute_StateTracker, requestSettings);
        }
    }
}
