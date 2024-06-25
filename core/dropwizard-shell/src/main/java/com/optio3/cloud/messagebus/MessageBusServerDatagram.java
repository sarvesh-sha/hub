/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.net.InetSocketAddress;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

import javax.validation.constraints.NotNull;

import com.optio3.asyncawait.AsyncBackground;
import com.optio3.cloud.JsonConnectionCapability;
import com.optio3.cloud.JsonDatagram;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.messagebus.payload.MbData;
import com.optio3.cloud.messagebus.transport.Endpoint;
import com.optio3.cloud.messagebus.transport.StableIdentity;
import com.optio3.cloud.messagebus.transport.SystemTransport;
import com.optio3.concurrency.DelayedAction;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.serialization.Reflection;
import com.optio3.stream.InputBuffer;
import com.optio3.util.TimeUtils;

/**
 * This class represents the server side of a MessageBus connection.
 *
 * The Broker uses this instance to talk to a client.
 */
public abstract class MessageBusServerDatagram extends JsonDatagram<MessageBusPayload>
{
    public static final Logger LoggerInstance = MessageBusBroker.LoggerInstance.createSubLogger(MessageBusServerDatagram.class);

    //--//

    private final TransportSecurityPolicy s_policy = new TransportSecurityPolicy()
    {
        // Empty for now.
    };

    //--//

    final class TransmitSideOfSystemTransport implements SystemTransport
    {
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

            return m_cfg.isValid();
        }

        @Override
        public boolean isService()
        {
            return false;
        }

        @Override
        public void markAsActive()
        {
            // Nothing to do.
        }

        @Override
        public String getEndpointId()
        {
            return m_cfg.endpointId;
        }

        @Override
        public String getPurposeInfo()
        {
            return "Server-side Datagram";
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
            return m_cfg.principal;
        }

        @Override
        public TransportSecurityPolicy getPolicy()
        {
            return s_policy;
        }

        @Override
        public Endpoint getEndpointForDestination(String destination)
        {
            return m_broker.getEndpoint(destination);
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
                LoggerInstance.error("dispatch unexpectedly failed: %s", e);

                await(asyncClose());
            }

            return wrapAsync(null);
        }

        @AsyncBackground(reason = "We don't want to process the close on the WebSocket thread")
        private CompletableFuture<Void> asyncClose()
        {
            MessageBusServerDatagram.this.close();

            return wrapAsync(null);
        }
    }

    //--//

    private final ContextualLogger contextualLogger = new ContextualLogger(LoggerInstance)
    {
        @Override
        public String getPrefix()
        {
            return MessageBusBroker.formatPrefix(m_broker.contextualLogger, "SERVER EP", m_cfg.endpointId);
        }
    };

    private final DelayedAction m_shutdown = new DelayedAction(this::shutdown);

    private final MessageBusBroker m_broker;

    private Endpoint                      m_ep;
    private TransmitSideOfSystemTransport m_transmitTransport;
    private SystemTransport               m_receiveTransport;

    protected InetSocketAddress m_lastSource;
    protected ZonedDateTime     m_lastPacket;

    protected MessageBusServerDatagram(MessageBusBroker broker,
                                       SessionConfiguration cfg)
    {
        super(cfg);

        m_broker = broker;

        m_shutdown.schedule(cfg.sessionExpiration);

        m_lastPacket = TimeUtils.now();
    }

    @Override
    public void close()
    {
        m_shutdown.cancel();

        unregister();

        super.close();
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

        m_ep               = m_broker.registerSystemTransport(m_transmitTransport);
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

        m_broker.unregisterSystemTransport(m_transmitTransport);

        m_transmitTransport = null;
        m_receiveTransport  = null;

        s_statisticsTotal.add(m_statistics);
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
            req.messageSize        = size;
            req.endpoint           = m_ep;
            req.principal          = m_cfg.principal;

            m_ep.recordIncomingMessage(size);

            contextualLogger.debugVerbose("Received a message (%d bytes): %s ", size, req);

            MbData data = Reflection.as(req, MbData.class);
            if (data != null)
            {
                // Make sure the client cannot spoof the origin.
                data.origin = m_cfg.endpointId;
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

    @Override
    protected InetSocketAddress getPhysicalConnection()
    {
        return m_lastSource;
    }

    //--//

    public void processFrame(InetSocketAddress source,
                             SharedHeader header,
                             InputBuffer ib)
    {
        recordRxPacket(ib);

        m_lastPacket = TimeUtils.now();

        if (m_lastSource == null || !m_lastSource.equals(source))
        {
            contextualLogger.debug("Updated transport: %s", source);
            m_lastSource = source;
        }

        processIncomingFrame(ib, header);

        markActivity();
    }

    protected abstract void unregister();

    protected abstract void shutdown();

    //--//

    protected void report(MessageBusDatagramSession sd)
    {
        sd.displayName = "<no identity>";

        if (m_ep != null)
        {
            sd.contextSysId = m_ep.getContextRecordId();

            StableIdentity epId = m_ep.getIdentity();
            if (epId != null)
            {
                sd.displayName = epId.displayName;
                sd.rpcId       = epId.rpcId;
            }

            String instanceId = m_ep.getContextInstanceId();
            if (instanceId != null)
            {
                sd.displayName += " - " + instanceId;
            }
        }

        sd.statistics = m_statistics.copy();

        sd.udpAddress = m_lastSource != null ? String.format("%s:%d",
                                                             m_lastSource.getAddress()
                                                                         .getHostAddress(),
                                                             m_lastSource.getPort()) : "<no address>";

        sd.lastPacket = m_lastPacket;
    }
}
