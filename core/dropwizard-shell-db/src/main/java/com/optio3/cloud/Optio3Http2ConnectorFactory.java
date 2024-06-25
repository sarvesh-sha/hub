/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import java.util.Collections;

import javax.annotation.Nullable;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.concurrency.Executors;
import io.dropwizard.jetty.HttpsConnectorFactory;
import io.dropwizard.jetty.Jetty93InstrumentedConnectionFactory;
import io.dropwizard.jetty.SslReload;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.parser.WindowRateControl;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NegotiatingServerConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.ThreadPool;

@JsonTypeName("optio3-h2")
public class Optio3Http2ConnectorFactory extends HttpsConnectorFactory
{

    /**
     * Supported protocols
     */
    private static final String H2                   = "h2";
    private static final String H2_17                = "h2-17";
    private static final String HTTP_1_1             = "http/1.1";
    private static final String HTTP2_DEFAULT_CIPHER = "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256";

    @Min(100)
    @Max(Integer.MAX_VALUE)
    private int maxConcurrentStreams = Math.max(100, 16 * Executors.getNumberOfProcessors());

    @Min(1)
    @Max(Integer.MAX_VALUE)
    private int initialStreamRecvWindow = 65535;

    @Min(10)
    @Max(10_000)
    private int maxRateControl = Math.max(200, 2 * maxConcurrentStreams);

    @JsonProperty
    public int getMaxConcurrentStreams()
    {
        return maxConcurrentStreams;
    }

    @JsonProperty
    public void setMaxConcurrentStreams(int maxConcurrentStreams)
    {
        this.maxConcurrentStreams = maxConcurrentStreams;
    }

    @JsonProperty
    public int getInitialStreamRecvWindow()
    {
        return initialStreamRecvWindow;
    }

    @JsonProperty
    public void setInitialStreamRecvWindow(int initialStreamRecvWindow)
    {
        this.initialStreamRecvWindow = initialStreamRecvWindow;
    }

    @JsonProperty
    public int getMaxRateControl()
    {
        return maxRateControl;
    }

    @JsonProperty
    public void setMaxRateControl(int maxRateControl)
    {
        this.maxRateControl = maxRateControl;
    }

    @Override
    public Connector build(Server server,
                           MetricRegistry metrics,
                           String name,
                           @Nullable ThreadPool threadPool)
    {
        // HTTP/2 requires that a server MUST support TLSv1.2 and TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 cipher
        // See http://http2.github.io/http2-spec/index.html#rfc.section.9.2.2
        setSupportedProtocols(Collections.singletonList("TLSv1.2"));
        checkSupportedCipherSuites();

        // Setup connection factories
        final HttpConfiguration            httpConfig = buildHttpConfiguration();
        final HttpConnectionFactory        http1      = buildHttpConnectionFactory(httpConfig);
        final HTTP2ServerConnectionFactory http2      = new HTTP2ServerConnectionFactory(httpConfig);
        http2.setMaxConcurrentStreams(maxConcurrentStreams);
        http2.setInitialStreamRecvWindow(initialStreamRecvWindow);
        http2.setRateControlFactory(new WindowRateControl.Factory(maxRateControl));

        // NOTE: Firefox wants to negotiate HTTP/1.1 for WebSockets, add it to the supported set.
        final NegotiatingServerConnectionFactory alpn = new ALPNServerConnectionFactory(H2, H2_17, HTTP_1_1);
        alpn.setDefaultProtocol(HTTP_1_1); // Speak HTTP 1.1 over TLS if negotiation fails

        final SslContextFactory sslContextFactory = configureSslContextFactory(new SslContextFactory.Server());
        sslContextFactory.addLifeCycleListener(logSslInfoOnStart(sslContextFactory));
        server.addBean(sslContextFactory);
        server.addBean(new SslReload(sslContextFactory, this::configureSslContextFactory));

        // We should use ALPN as a negotiation protocol. Old clients that don't support it will be served
        // via HTTPS. New clients, however, that want to use HTTP/2 will use TLS with ALPN extension.
        // If negotiation succeeds, the client and server switch to HTTP/2 protocol.
        final SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, "alpn");

        return buildConnector(server,
                              new ScheduledExecutorScheduler(),
                              buildBufferPool(),
                              name,
                              threadPool,
                              new Jetty93InstrumentedConnectionFactory(sslConnectionFactory, metrics.timer(httpConnections())),
                              alpn,
                              http2,
                              http1);
    }

    void checkSupportedCipherSuites()
    {
        if (getSupportedCipherSuites() == null)
        {
            setSupportedCipherSuites(Lists.newArrayList(HTTP2_DEFAULT_CIPHER,
                                                        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                                                        "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
                                                        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                                                        "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256"));
        }
        else if (!getSupportedCipherSuites().contains(HTTP2_DEFAULT_CIPHER))
        {
            throw new IllegalArgumentException("HTTP/2 server configuration must include cipher: " + HTTP2_DEFAULT_CIPHER);
        }
    }
}
