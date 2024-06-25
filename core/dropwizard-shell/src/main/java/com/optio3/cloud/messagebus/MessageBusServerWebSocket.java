/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.optio3.asyncawait.AsyncBackground;
import com.optio3.cloud.AbstractApplication;
import com.optio3.cloud.JsonConnectionCapability;
import com.optio3.cloud.JsonWebSocket;
import com.optio3.cloud.annotation.Optio3WebSocketEndpoint;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.messagebus.payload.MbData;
import com.optio3.cloud.messagebus.transport.Endpoint;
import com.optio3.cloud.messagebus.transport.SystemTransport;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.serialization.Reflection;
import com.optio3.util.IdGenerator;
import com.optio3.util.TimeUtils;
import org.eclipse.jetty.websocket.api.WebSocketException;

/**
 * This class represents the server side of a MessageBus connection.
 *
 * The Broker uses this instance to talk to a client.
 */
public abstract class MessageBusServerWebSocket extends JsonWebSocket<MessageBusPayload>
{
    public static final Logger LoggerInstance = MessageBusBroker.LoggerInstance.createSubLogger(MessageBusServerWebSocket.class);

    //--//

    private final TransportSecurityPolicy s_policy = new TransportSecurityPolicy()
    {
        // Empty for now.
    };

    //--//

    final class TransmitSideOfSystemTransport implements SystemTransport
    {
        private long m_lastActive;

        TransmitSideOfSystemTransport()
        {
            markAsActive();
        }

        @Override
        public void close()
        {
            // Nothing to do.
        }

        @Override
        public boolean isOpen()
        {
            if (m_transmitTransport == null)
            {
                return false;
            }

            long now = TimeUtils.nowMilliUtc();
            return now < m_lastActive + m_maxIdleTime;
        }

        @Override
        public boolean isService()
        {
            return false;
        }

        @Override
        public void markAsActive()
        {
            m_lastActive = TimeUtils.nowMilliUtc();
        }

        @Override
        public String getEndpointId()
        {
            return m_endpointId;
        }

        @Override
        public String getPurposeInfo()
        {
            return "Server-side WebSocket";
        }

        @Override
        public EnumSet<JsonConnectionCapability> exchangeCapabilities(EnumSet<JsonConnectionCapability> available,
                                                                      EnumSet<JsonConnectionCapability> required)
        {
            EnumSet<JsonConnectionCapability> local = getLocalCapabilities();
            available.retainAll(local);
            required.retainAll(local);

            setReceiveCapabilities(available);
            setTransmitCapabilities(required);

            return local;
        }

        @Override
        public @NotNull CookiePrincipal getTransportPrincipal()
        {
            return getPrincipal();
        }

        @Override
        public TransportSecurityPolicy getPolicy()
        {
            return s_policy;
        }

        @Override
        public Endpoint getEndpointForDestination(String destination)
        {
            return getBroker().getEndpoint(destination);
        }

        @Override
        public CompletableFuture<Void> dispatch(MessageBusPayload msg,
                                                MessageBusPayloadCallback notifyPayloadSize) throws
                                                                                             Exception
        {
            try
            {
                sendMessage(msg, (size) ->
                {
                    m_ep.recordOutgoingMessage(size);

                    if (notifyPayloadSize != null)
                    {
                        notifyPayloadSize.accept(size);
                    }
                });

                markAsActive();
            }
            catch (Exception e)
            {
                if (e instanceof WebSocketException)
                {
                    // This is expected.
                }
                else
                {
                    LoggerInstance.error("dispatch unexpectedly failed: %s", e);
                }

                await(asyncClose());
            }

            return wrapAsync(null);
        }

        @AsyncBackground(reason = "We don't want to process the close on the WebSocket thread")
        private CompletableFuture<Void> asyncClose()
        {
            MessageBusServerWebSocket.this.close();

            return wrapAsync(null);
        }
    }

    //--//

    private final ContextualLogger contextualLogger = new ContextualLogger(LoggerInstance)
    {
        @Override
        public String getPrefix()
        {
            return MessageBusBroker.formatPrefix(getBroker().contextualLogger, "SERVER EP", m_endpointId);
        }
    };

    private final String m_endpointId = IdGenerator.newGuid();
    private final long   m_maxIdleTime;

    @Inject
    private AbstractApplication m_app;

    private final Supplier<MessageBusBroker> m_brokerSupplier = Suppliers.memoize(() -> m_app.getServiceNonNull(MessageBusBroker.class));

    private Endpoint                      m_ep;
    private TransmitSideOfSystemTransport m_transmitTransport;
    private SystemTransport               m_receiveTransport;

    protected MessageBusServerWebSocket()
    {
        Optio3WebSocketEndpoint anno = getClass().getAnnotation(Optio3WebSocketEndpoint.class);
        m_maxIdleTime = anno.timeout();
    }

    MessageBusBroker getBroker()
    {
        return m_brokerSupplier.get();
    }

    @Override
    protected void onConnect()
    {
        contextualLogger.debugVerbose("New connection");
        if (contextualLogger.isEnabled(Severity.DebugObnoxious))
        {
            contextualLogger.debugObnoxious("%s", new Exception());
        }

        m_transmitTransport = new TransmitSideOfSystemTransport();

        m_ep = getBroker().registerSystemTransport(m_transmitTransport);
        m_receiveTransport = m_ep.getTransport();

        m_ep.recordConnection();
    }

    @Override
    protected void onClose()
    {
        contextualLogger.debugVerbose("Connection closed");
        if (contextualLogger.isEnabled(Severity.DebugObnoxious))
        {
            contextualLogger.debugObnoxious("%s", new Exception());
        }

        getBroker().unregisterSystemTransport(m_transmitTransport);

        m_transmitTransport = null;
        m_receiveTransport = null;
    }

    @Override
    protected void onError(Throwable cause)
    {
        close();
    }

    @Override
    protected void onMessage(InetSocketAddress physicalConnection,
                             MessageBusPayload req,
                             int size)
    {
        try
        {
            req.physicalConnection = physicalConnection;
            req.messageSize = size;
            req.endpoint = m_ep;
            req.principal = getPrincipal();

            m_ep.recordIncomingMessage(size);

            contextualLogger.debugVerbose("Received a message (%d bytes): %s ", size, req);

            MbData data = Reflection.as(req, MbData.class);
            if (data != null)
            {
                // Make sure the client cannot spoof the origin.
                data.origin = m_endpointId;
            }

            m_receiveTransport.dispatch(req, null);
        }
        catch (Exception e)
        {
            contextualLogger.error("Unexpected error while processing message %s: %s",
                                   req.getClass()
                                      .getName(),
                                   req);
            contextualLogger.error("%s", e);
        }
    }
}
