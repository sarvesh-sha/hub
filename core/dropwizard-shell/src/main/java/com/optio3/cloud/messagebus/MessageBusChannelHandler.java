/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.JsonWebSocket;
import com.optio3.cloud.messagebus.payload.MbData;
import com.optio3.cloud.messagebus.payload.MbData_Message;
import com.optio3.cloud.messagebus.payload.MbData_Message_Reply;
import com.optio3.cloud.messagebus.transport.DataTransport;
import com.optio3.cloud.messagebus.transport.DataTransportWithReplies;
import com.optio3.cloud.messagebus.transport.Endpoint;
import com.optio3.logging.Logger;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;

public abstract class MessageBusChannelHandler<TRequest, TReply> implements DataTransport<TRequest>
{
    public static final Logger LoggerInstance = MessageBusBroker.LoggerInstance.createSubLogger(MessageBusChannelHandler.class);

    private final ContextualLogger contextualLogger = new ContextualLogger(LoggerInstance)
    {
        @Override
        public String getPrefix()
        {
            return MessageBusBroker.formatPrefix("CHANNEL", m_channelName);
        }
    };

    private final String m_channelName;

    protected final Class<TRequest> requestClass;
    protected final Class<TReply>   replyClass;

    private DataTransportWithReplies<TRequest, TReply> m_outboundPath = MessageBusBroker.getPlaceholderTransport();

    protected MessageBusChannelHandler(String channelName)
    {
        m_channelName = channelName;
        requestClass = Reflection.searchTypeArgument(MessageBusChannelHandler.class, this, 0);
        replyClass = Reflection.searchTypeArgument(MessageBusChannelHandler.class, this, 1);

        if (requestClass == null || replyClass == null)
        {
            // sanity check, should never happen
            throw Exceptions.newIllegalArgumentException("Incorrect usage: Subscriber for channel %s constructed without actual type information: %s", channelName, getClass().getName());
        }
    }

    @Override
    public void close()
    {
        m_outboundPath.close();
    }

    @Override
    public String getChannelName()
    {
        return m_channelName;
    }

    @Override
    public String getEndpointId()
    {
        return m_outboundPath.getEndpointId();
    }

    //--//

    @Override
    public void setTransmitTransport(DataTransportWithReplies<?, ?> transport)
    {
        if (transport == null)
        {
            transport = MessageBusBroker.getPlaceholderTransport();
        }

        @SuppressWarnings("unchecked") DataTransportWithReplies<TRequest, TReply> outboundPath = (DataTransportWithReplies<TRequest, TReply>) transport;

        m_outboundPath = outboundPath;
    }

    public DataTransportWithReplies<TRequest, TReply> getTransmitTransport()
    {
        return m_outboundPath;
    }

    //--//

    @Override
    public CompletableFuture<Void> dispatchWithNoReply(MbData data,
                                                       MessageBusPayloadCallback notifyPayloadSize) throws
                                                                                                    Exception
    {
        MbData_Message msg = Reflection.as(data, MbData_Message.class);
        if (msg != null)
        {
            TRequest payload = JsonWebSocket.deserializeValue(requestClass, msg.payload);

            return receivedMessage(msg, payload);
        }

        return wrapAsync(null);
    }

    //--//

    public Endpoint getEndpointForDestination(String destination)
    {
        return m_outboundPath.getEndpointForDestination(destination);
    }

    public CompletableFuture<Void> sendMessageWithNoReply(String destination,
                                                          TRequest msg,
                                                          MessageBusPayloadCallback notifyPayloadSize) throws
                                                                                                       Exception
    {
        MbData_Message data = new MbData_Message();
        data.destination = destination;
        data.convertPayload(msg);

        contextualLogger.debug("sendMessageWithNoReply: %s - %s", data.destination, msg);

        return m_outboundPath.dispatchWithNoReply(data, notifyPayloadSize);
    }

    public <T extends TReply> CompletableFuture<T> sendMessageWithReply(String destination,
                                                                        TRequest msg,
                                                                        Class<T> replyClass,
                                                                        MessageBusPayloadCallback notifyPayloadSize,
                                                                        int timeoutForReply,
                                                                        TimeUnit timeoutUnit) throws
                                                                                              Exception
    {
        MbData_Message data = new MbData_Message();
        data.destination = destination;
        data.convertPayload(msg);

        contextualLogger.debug("sendMessageWithReply: %s - %s", data.destination, msg);

        return m_outboundPath.dispatchWithReply(data, replyClass, notifyPayloadSize, timeoutForReply, timeoutUnit);
    }

    public CompletableFuture<Void> replyToMessage(MbData_Message from,
                                                  TReply obj,
                                                  MessageBusPayloadCallback notifyPayloadSize) throws
                                                                                               Exception
    {
        MbData_Message_Reply reply = MbData_Message_Reply.prepareForReply(from, obj);

        contextualLogger.debug("replyToMessage: %s - %s", reply.destination, obj);

        return m_outboundPath.dispatchWithNoReply(reply, notifyPayloadSize);
    }

    //--//

    protected abstract CompletableFuture<Void> receivedMessage(MbData_Message data,
                                                               TRequest obj) throws
                                                                             Exception;
}
