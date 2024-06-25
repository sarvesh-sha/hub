/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;
import com.optio3.util.CollectionUtils;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.SocketAddressResolver;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.common.scopes.SimpleContainerScope;

public abstract class JsonWebSocketClient<T> extends JsonWebSocket<T>
{
    public static final Logger LoggerInstance = new Logger(JsonWebSocketClient.class, true);

    //--//

    private static class CachingSocketAddressResolver implements SocketAddressResolver
    {
        private static class Entry
        {
            MonotonousTime    validUntil;
            List<InetAddress> addresses;

            void refreshValidity()
            {
                validUntil = TimeUtils.computeTimeoutExpiration(6, TimeUnit.HOURS);
            }

            List<InetSocketAddress> resolve(int port)
            {
                return CollectionUtils.transformToList(addresses, (address) -> new InetSocketAddress(address, port));
            }
        }

        //--//

        private static final ConcurrentMap<String, Entry> s_lookup = Maps.newConcurrentMap();

        //--//

        private final SocketAddressResolver                          m_nestedResolver;
        private final JsonWebSocketDnsHints                          m_dnsHints;
        private final ConcurrentMap<String, List<InetSocketAddress>> m_lastResults = Maps.newConcurrentMap();

        CachingSocketAddressResolver(JsonWebSocketDnsHints dnsHints,
                                     ThreadPoolExecutor executor,
                                     Scheduler scheduler,
                                     long timeout)
        {
            m_nestedResolver = new SocketAddressResolver.Async(executor, scheduler, timeout);
            m_dnsHints       = dnsHints;
        }

        @Override
        public void resolve(String host,
                            int port,
                            Promise<List<InetSocketAddress>> promise)
        {
            m_lastResults.remove(host);

            Entry entry = s_lookup.get(host);
            if (entry != null && !TimeUtils.isTimeoutExpired(entry.validUntil))
            {
                // Still valid, use it directly.
                recordResult(promise, host, entry.resolve(port));
                return;
            }

            Promise<List<InetSocketAddress>> nestedPromise = new Promise<List<InetSocketAddress>>()
            {
                @Override
                public void succeeded(List<InetSocketAddress> result)
                {
                    recordResult(promise, host, result);
                }

                @Override
                public void failed(Throwable x)
                {
                    if (entry != null)
                    {
                        // If we have a cache entry, try it even if it's stale.
                        recordResult(promise, host, entry.resolve(port));
                        return;
                    }

                    List<InetAddress> hints = m_dnsHints.lookup(host);
                    if (CollectionUtils.isNotEmpty(hints))
                    {
                        recordResult(promise, host, CollectionUtils.transformToList(hints, (address) -> new InetSocketAddress(address, port)));
                        return;
                    }

                    promise.failed(x);
                }
            };

            m_nestedResolver.resolve(host, port, nestedPromise);
        }

        private void recordResult(Promise<List<InetSocketAddress>> promise,
                                  String host,
                                  List<InetSocketAddress> result)
        {
            m_lastResults.put(host, result);
            promise.succeeded(result);
        }

        public List<InetAddress> markSuccess(String host)
        {
            List<InetSocketAddress> addressesWithPorts = m_lastResults.get(host);
            if (addressesWithPorts == null)
            {
                addressesWithPorts = Collections.emptyList();
            }

            CachingSocketAddressResolver.Entry entry = s_lookup.get(host);
            if (entry == null)
            {
                entry = new Entry();
                s_lookup.put(host, entry);
            }

            entry.addresses = CollectionUtils.transformToList(addressesWithPorts, InetSocketAddress::getAddress);
            entry.refreshValidity();

            return Lists.newArrayList(entry.addresses);
        }
    }

    private static class SchedulerImpl extends AbstractLifeCycle implements Scheduler
    {
        @Override
        public Task schedule(Runnable task,
                             long delay,
                             TimeUnit units)
        {
            ScheduledFuture<?> future = Executors.scheduleOnDefaultPool(task, delay, units);

            return () -> future.cancel(false);
        }
    }

    //--//

    private final String                m_connectionUrl;
    private final JsonWebSocketDnsHints m_dnsHints;
    private final String                m_userName;
    private final String                m_password;
    private final HttpCookie            m_token;

    private HttpClient      m_httpClient;
    private WebSocketClient m_client;

    protected JsonWebSocketClient(JsonWebSocketDnsHints dnsHints,
                                  String connectionUrl,
                                  String userName,
                                  String password)
    {
        m_connectionUrl = connectionUrl;
        m_dnsHints      = dnsHints;
        m_userName      = userName;
        m_password      = password;
        m_token         = null;
    }

    protected JsonWebSocketClient(JsonWebSocketDnsHints dnsHints,
                                  String connectionUrl,
                                  String token)
    {
        m_connectionUrl = connectionUrl;
        m_dnsHints      = dnsHints;
        m_userName      = null;
        m_password      = null;
        m_token         = HttpCookie.parse(token)
                                    .get(0);
    }

    public String describeConnection()
    {
        return String.format("%s over WebSocket", m_connectionUrl);
    }

    public List<InetAddress> connectToServer() throws
                                               Exception
    {
        try
        {
            final int connectTimeout = 60 * 1000;

            // Use our thread pool for both HttpClient and WebSocketClient!
            ThreadPoolExecutor executor  = Executors.getDefaultThreadPool();
            Scheduler          scheduler = new SchedulerImpl();

            URI parsedUri = new URI(m_connectionUrl);

            m_httpClient = new HttpClient(new SslContextFactory.Client(true));
            m_httpClient.setExecutor(executor);
            m_httpClient.setScheduler(scheduler);
            m_httpClient.setConnectTimeout(connectTimeout);

            CachingSocketAddressResolver resolver = new CachingSocketAddressResolver(m_dnsHints, executor, scheduler, m_httpClient.getAddressResolutionTimeout());
            m_httpClient.setSocketAddressResolver(resolver);

            SimpleContainerScope scope = new SimpleContainerScope(new WebSocketPolicy(WebSocketBehavior.CLIENT), null, executor, null, null);
            m_client = new WebSocketClient(scope, null, null, m_httpClient);
            m_client.addBean(m_httpClient);

            final WebSocketPolicy policy = m_client.getPolicy();
            policy.setIdleTimeout(30 * 60 * 1000);
            policy.setMaxTextMessageSize(WebSocketWrapper.MaxMessageSize);
            policy.setMaxBinaryMessageSize(WebSocketWrapper.MaxMessageSize);

            if (m_userName != null)
            {
                BasicAuthentication.BasicResult result = new BasicAuthentication.BasicResult(parsedUri, m_userName, m_password);
                m_httpClient.getAuthenticationStore()
                            .addAuthenticationResult(result);
            }

            if (m_token != null)
            {
                CookieStore cookieStore = new HttpCookieStore();
                m_httpClient.setCookieStore(cookieStore);
                cookieStore.add(parsedUri, m_token);
            }

            m_client.start();

            ClientUpgradeRequest request = new ClientUpgradeRequest();
            m_client.connect(this, parsedUri, request)
                    .get(2 * connectTimeout, TimeUnit.MILLISECONDS);

            return resolver.markSuccess(parsedUri.getHost());
        }
        catch (Exception e)
        {
            onConnected().completeExceptionally(e);

            throw e;
        }
    }

    @Override
    public void close()
    {
        super.close();

        WebSocketClient client     = m_client;
        HttpClient      httpClient = m_httpClient;
        m_client     = null;
        m_httpClient = null;

        if (client != null || httpClient != null)
        {
            Executors.closeWithTimeout(() ->
                                       {
                                           if (client != null)
                                           {
                                               client.stop();
                                           }

                                           if (httpClient != null)
                                           {
                                               httpClient.stop();
                                           }
                                       }, 2 * 60 * 1000, (t) ->
                                       {
                                           LoggerInstance.error("Failed to close WebSocketClient, due to %s", t);
                                       });
        }
    }

    public boolean isOpen()
    {
        return m_client != null;
    }
}
