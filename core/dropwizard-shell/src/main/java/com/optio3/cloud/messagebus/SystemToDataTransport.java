/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import com.optio3.cloud.JsonConnectionCapability;
import com.optio3.cloud.JsonWebSocket;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.messagebus.payload.MbData;
import com.optio3.cloud.messagebus.payload.MbData_Message;
import com.optio3.cloud.messagebus.payload.MbData_Message_Reply;
import com.optio3.cloud.messagebus.transport.DataTransport;
import com.optio3.cloud.messagebus.transport.DataTransportWithReplies;
import com.optio3.cloud.messagebus.transport.Endpoint;
import com.optio3.cloud.messagebus.transport.SystemTransport;
import com.optio3.logging.Logger;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import com.optio3.util.IdGenerator;

public final class SystemToDataTransport<TRequest, TReply> implements SystemTransport,
                                                                      ChannelLifecycle
{
    public static final Logger LoggerInstance = MessageBusBroker.LoggerInstance.createSubLogger(SystemToDataTransport.class);

    private class Outbound implements DataTransportWithReplies<TRequest, TReply>
    {
        private final MessageBusReplyHandler m_rh = new MessageBusReplyHandler()
        {
            @Override
            public String getContextId()
            {
                return m_endpointId;
            }
        };

        private final SystemTransport m_outboundPath;

        Outbound(SystemTransport outboundPath)
        {
            m_outboundPath = outboundPath;
        }

        //--//

        @Override
        public void close()
        {
            m_outboundPath.close();
            m_rh.close();
        }

        boolean isOpen()
        {
            return m_outboundPath.isOpen();
        }

        void markAsActive()
        {
            m_outboundPath.markAsActive();
        }

        String getPurposeInfo()
        {
            return m_outboundPath.getPurposeInfo();
        }

        @Override
        public String getChannelName()
        {
            return m_inboundPath.getChannelName();
        }

        @Override
        public String getEndpointId()
        {
            return m_endpointId;
        }

        //--//

        @Override
        public void setTransmitTransport(DataTransportWithReplies<?, ?> transport)
        {
            throw new RuntimeException("Internal error: this method should not be called");
        }

        //--//

        @Override
        public Endpoint getEndpointForDestination(String destination)
        {
            return m_outboundPath.getEndpointForDestination(destination);
        }

        @Override
        public CompletableFuture<Void> dispatchWithNoReply(MbData data,
                                                           MessageBusPayloadCallback notifyPayloadSize) throws
                                                                                                        Exception
        {
            SystemTransport outboundPath = m_outboundPath;
            if (outboundPath != null)
            {
                fixupSender(data);

                contextualLogger.debugVerbose("[MSG: %s] dispatchWithNoReply: %s", data.messageId, data);

                await(outboundPath.dispatch(data, notifyPayloadSize));
            }

            return wrapAsync(null);
        }

        @Override
        public <T extends TReply> CompletableFuture<T> dispatchWithReply(MbData_Message data,
                                                                         Class<T> replyClass,
                                                                         MessageBusPayloadCallback notifyPayloadSize,
                                                                         int timeoutForReply,
                                                                         TimeUnit timeoutUnit) throws
                                                                                               Exception
        {
            fixupSender(data);

            CompletableFuture<MbData_Message_Reply> replyFuture = m_rh.createNewReplyHandler(MbData_Message_Reply.class, timeoutForReply, timeoutUnit, data.messageId);

            contextualLogger.debugVerbose("[MSG: %s] dispatchWithNoReply: %s", data.messageId, data);

            await(m_outboundPath.dispatch(data, notifyPayloadSize));

            MbData_Message_Reply reply = await(replyFuture);

            contextualLogger.debugVerbose("[MSG: %s] dispatchWithNoReply: got %s", data.messageId, reply);

            T result = JsonWebSocket.deserializeValue(replyClass, reply.payload);

            contextualLogger.debugVerbose("[MSG: %s] dispatchWithNoReply: result = %s", data.messageId, result);

            return wrapAsync(result);
        }

        //--//

        private void fixupSender(MbData data)
        {
            if (data.messageId == null)
            {
                data.messageId = IdGenerator.newGuid();
            }

            if (data.origin == null)
            {
                data.origin = m_endpointId;
            }

            data.channel = m_inboundPath.getChannelName();
        }
    }

    //--//

    private final ContextualLogger contextualLogger = new ContextualLogger(LoggerInstance)
    {
        @Override
        public String getPrefix()
        {
            return MessageBusBroker.formatPrefix("EP", m_endpointId);
        }
    };

    private final String  m_endpointId;
    private final boolean m_isService;

    private final Outbound                m_outboundPath;
    private final DataTransport<TRequest> m_inboundPath;

    SystemToDataTransport(SystemTransport outboundPath,
                          DataTransport<TRequest> inboundPath)
    {
        m_endpointId = outboundPath.getEndpointId();
        m_isService = SystemTransport.isService(m_endpointId);

        m_inboundPath = inboundPath;
        m_outboundPath = new Outbound(outboundPath);
    }

    //--//

    public boolean handleReply(MessageBusPayload req)
    {
        return m_outboundPath.m_rh.handleReply(req);
    }

    public DataTransportWithReplies<TRequest, TReply> getTransmitTransport()
    {
        return m_outboundPath;
    }

    //--//

    @Override
    public void close()
    {
        m_outboundPath.close();
    }

    @Override
    public boolean isOpen()
    {
        return m_outboundPath.isOpen();
    }

    @Override
    public boolean isService()
    {
        return m_isService;
    }

    @Override
    public void markAsActive()
    {
        m_outboundPath.markAsActive();
    }

    @Override
    public String getEndpointId()
    {
        return m_endpointId;
    }

    @Override
    public String getPurposeInfo()
    {
        return m_outboundPath.getPurposeInfo();
    }

    @Override
    public EnumSet<JsonConnectionCapability> exchangeCapabilities(EnumSet<JsonConnectionCapability> available,
                                                                  EnumSet<JsonConnectionCapability> required)
    {
        throw new RuntimeException("Internal error: unexpected call");
    }

    @Override
    public @NotNull CookiePrincipal getTransportPrincipal()
    {
        return CookiePrincipal.getMachine();
    }

    @Override
    public TransportSecurityPolicy getPolicy()
    {
        throw new RuntimeException("Internal error: unexpected call");
    }

    @Override
    public Endpoint getEndpointForDestination(String destination)
    {
        return m_outboundPath.getEndpointForDestination(destination);
    }

    @Override
    public CompletableFuture<Void> dispatch(MessageBusPayload msg,
                                            MessageBusPayloadCallback notifyPayloadSize) throws
                                                                                         Exception
    {
        MbData data = Reflection.as(msg, MbData.class);
        if (data != null)
        {
            boolean isReply = msg instanceof MbData_Message_Reply;

            contextualLogger.debug("Received a message (reply=%s) for channel %s: %s", isReply, data.channel, data);

            if (isReply)
            {
                if (!handleReply(msg))
                {
                    contextualLogger.debug("Received a reply with no waiters: %s", msg);
                }

                return wrapAsync(null);
            }

            if (!m_inboundPath.getChannelName()
                              .equals(data.channel))
            {
                // Drop messages for unknown or unsubscribed channels on the floor.
                contextualLogger.error("Received a message (reply=%s) for a non-subscribed channel %s: %s", isReply, data.channel, data);
                return wrapAsync(null);
            }

            return m_inboundPath.dispatchWithNoReply(data, notifyPayloadSize);
        }

        throw Exceptions.newRuntimeException("[EP: %s] Unexpected message %s: %s",
                                             m_endpointId,
                                             msg.getClass()
                                                .getName(),
                                             JsonWebSocket.serializeValue(msg));
    }

    //--//

    @Override
    public void onJoin(SystemTransport transport)
    {
        ChannelLifecycle cl = Reflection.as(m_inboundPath, ChannelLifecycle.class);
        if (cl != null)
        {
            cl.onJoin(transport);
        }
    }

    @Override
    public void onLeave(SystemTransport transport)
    {
        ChannelLifecycle cl = Reflection.as(m_inboundPath, ChannelLifecycle.class);
        if (cl != null)
        {
            cl.onLeave(transport);
        }
    }
}
