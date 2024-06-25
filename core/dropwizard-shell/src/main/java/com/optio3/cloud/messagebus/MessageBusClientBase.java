/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import javax.validation.constraints.NotNull;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.asyncawait.AsyncBackground;
import com.optio3.cloud.JsonConnectionCapability;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.messagebus.payload.MbControl_ExchangeCapabilities;
import com.optio3.cloud.messagebus.payload.MbControl_ExchangeCapabilities_Reply;
import com.optio3.cloud.messagebus.payload.MbControl_GetIdentity;
import com.optio3.cloud.messagebus.payload.MbControl_GetIdentity_Reply;
import com.optio3.cloud.messagebus.payload.MbControl_JoinChannel;
import com.optio3.cloud.messagebus.payload.MbControl_JoinChannel_Reply;
import com.optio3.cloud.messagebus.payload.MbControl_KeepAlive;
import com.optio3.cloud.messagebus.payload.MbControl_KeepAlive_Reply;
import com.optio3.cloud.messagebus.payload.MbControl_LeaveChannel;
import com.optio3.cloud.messagebus.payload.MbControl_LeaveChannel_Reply;
import com.optio3.cloud.messagebus.payload.MbControl_ListChannels;
import com.optio3.cloud.messagebus.payload.MbControl_ListChannels_Reply;
import com.optio3.cloud.messagebus.payload.MbControl_ListMembers;
import com.optio3.cloud.messagebus.payload.MbControl_ListMembers_Reply;
import com.optio3.cloud.messagebus.payload.MbControl_Reply;
import com.optio3.cloud.messagebus.payload.MbData;
import com.optio3.cloud.messagebus.payload.MbData_Message_Reply;
import com.optio3.cloud.messagebus.transport.DataTransport;
import com.optio3.cloud.messagebus.transport.Endpoint;
import com.optio3.cloud.messagebus.transport.SystemTransport;
import com.optio3.logging.Logger;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.eclipse.jetty.websocket.api.WebSocketException;

/**
 * This class represents the client side of a MessageBus connection.
 *
 * Clients use this calls to talk to the Broker sitting on the server.
 */
public abstract class MessageBusClientBase implements MessageBusClient,
                                                      AutoCloseable
{
    public static final Logger LoggerInstance = MessageBusBroker.LoggerInstance.createSubLogger(MessageBusClientBase.class);

    private static final Duration c_maxIdleTime = Duration.of(10, ChronoUnit.MINUTES);

    class DataTransportPair
    {
        private final SystemToDataTransport<?, ?> m_outboundPath;
        private final DataTransport<?>            m_inboundPath;

        public DataTransportPair(SystemToDataTransport<?, ?> outboundPath,
                                 DataTransport<?> inboundPath)
        {
            this.m_outboundPath = outboundPath;
            this.m_inboundPath = inboundPath;
        }

        void close()
        {
            m_outboundPath.close();
            m_inboundPath.close();
        }

        CompletableFuture<Void> dispatchWithNoReply(MbData data) throws
                                                                 Exception
        {
            if (data instanceof MbData_Message_Reply)
            {
                if (!m_outboundPath.handleReply(data))
                {
                    contextualLogger.debug("Received a reply with no waiters: %s", data);
                }

                return wrapAsync(null);
            }

            return m_inboundPath.dispatchWithNoReply(data, null);
        }
    }

    final class OutboundTransport implements SystemTransport
    {
        private MonotonousTime m_activeTimeout;

        OutboundTransport()
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

            return !TimeUtils.isTimeoutExpired(m_activeTimeout);
        }

        @Override
        public boolean isService()
        {
            return false;
        }

        @Override
        public void markAsActive()
        {
            m_activeTimeout = TimeUtils.computeTimeoutExpiration(c_maxIdleTime);
        }

        @Override
        public String getEndpointId()
        {
            return m_endpointId;
        }

        @Override
        public String getPurposeInfo()
        {
            return m_transmitTransport == null ? "<closed>" : "Receive side of " + m_transmitTransport.getPurposeInfo();
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
            // We are the client side. This API is only usable on the server side.
            return null;
        }

        @Override
        public CompletableFuture<Void> dispatch(MessageBusPayload msg,
                                                MessageBusPayloadCallback notifyPayloadSize) throws
                                                                                             Exception
        {
            try
            {
                sendMessage(msg, notifyPayloadSize != null ? notifyPayloadSize::accept : null);

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

        @AsyncBackground(reason = "We don't want to process the close on the receive thread")
        private CompletableFuture<Void> asyncClose()
        {
            closeConnection();

            return wrapAsync(null);
        }
    }

    private final ContextualLogger contextualLogger = new ContextualLogger(LoggerInstance)
    {
        @Override
        public String getPrefix()
        {
            return MessageBusBroker.formatPrefix("CLIENT", m_endpointId);
        }
    };

    //--//

    private       String                 m_endpointId;
    protected     OutboundTransport      m_transmitTransport;
    private final MessageBusReplyHandler m_rhForSystemMessages = new MessageBusReplyHandler()
    {
        @Override
        public String getContextId()
        {
            return m_endpointId;
        }
    };

    private final Map<String, DataTransportPair> m_joinedChannels = Maps.newHashMap();

    private final CompletableFuture<Void> m_gotIdentity = new CompletableFuture<>();

    private int m_keepAliveFailures;

    //--//

    protected MessageBusClientBase(String endpointId)
    {
        m_endpointId = endpointId;
    }

    //--//

    @Override
    public void close()
    {
        closeConnection();
    }

    //--//

    protected abstract CompletableFuture<Void> postProcessIdentity(EnumSet<JsonConnectionCapability> common) throws
                                                                                                             Exception;

    protected abstract EnumSet<JsonConnectionCapability> getLocalCapabilities();

    protected abstract void setTransmitCapabilities(EnumSet<JsonConnectionCapability> required);

    protected abstract void setReceiveCapabilities(EnumSet<JsonConnectionCapability> required);

    protected abstract void sendMessage(MessageBusPayload msg,
                                        Consumer<Integer> notifyPayloadSize) throws
                                                                             Exception;

    protected CompletableFuture<Void> routeAsync(MessageBusPayload req)
    {
        try
        {
            contextualLogger.debugVerbose("Received a message: %s", req);

            if (req instanceof MbControl_Reply)
            {
                if (!m_rhForSystemMessages.handleReply(req))
                {
                    contextualLogger.debug("Received a reply with no waiters: %s", req);
                }

                return wrapAsync(null);
            }

            if (m_endpointId == null)
            {
                contextualLogger.error("Received a message before endpointId could be established: %s", req);

                return wrapAsync(null);
            }

            MbData data = Reflection.as(req, MbData.class);
            if (data != null)
            {
                boolean accept = MbData.isBroadcast(data.destination) || m_endpointId.equals(data.destination);
                if (!accept)
                {
                    contextualLogger.error("Received a message for channel %s sent to wrong address %s: %s", data.channel, data.destination, data);
                    return wrapAsync(null);
                }

                DataTransportPair pair = getChannel(data.channel);
                if (pair == null)
                {
                    contextualLogger.error("Received a message for channel %s sent to wrong address %s: %s", data.channel, data.destination, data);

                    return wrapAsync(null);
                }

                return pair.dispatchWithNoReply(data);
            }

            contextualLogger.error("Unexpected message %s on session %s: %s",
                                   req.getClass()
                                      .getName(),
                                   m_endpointId,
                                   req);
        }
        catch (Exception e)
        {
            contextualLogger.error("Unexpected error while processing message %s on session %s: %s",
                                   req.getClass()
                                      .getName(),
                                   m_endpointId,
                                   req);
            contextualLogger.error("%s", e);
        }

        return wrapAsync(null);
    }

    //--//

    protected CompletableFuture<String> retrieveIdentity()
    {
        try
        {
            MbControl_GetIdentity       req   = new MbControl_GetIdentity();
            MbControl_GetIdentity_Reply reply = await(sendSystemMessage(req, MbControl_GetIdentity_Reply.class, null));
            if (reply.endpointIdentity == null)
            {
                throw new RuntimeException("GetIdentity command failed");
            }

            m_endpointId = reply.endpointIdentity;
            contextualLogger.invalidatePrefix();

            //
            // First we exchange the capabilities, without enabling any.
            //
            EnumSet<JsonConnectionCapability> available = await(exchangeCapabilities(getLocalCapabilities(), null));

            EnumSet<JsonConnectionCapability> common = EnumSet.copyOf(getLocalCapabilities());
            common.retainAll(available);

            //
            // Then we activate the common capabilities on the Rx path.
            //
            setReceiveCapabilities(common);

            //
            // Next, we tell the remote side which capabilities to activate.
            //
            await(exchangeCapabilities(common, common));

            //
            // Finally, we activate the common capabilities on the Tx path.
            //
            setTransmitCapabilities(common);

            await(postProcessIdentity(common));

            //--//

            keepAlive();

            m_gotIdentity.complete(null);
        }
        catch (Exception e)
        {
            m_gotIdentity.completeExceptionally(e);
            closeConnection();
        }

        return wrapAsync(null);
    }

    protected void releaseIdentity()
    {
        failIdentity(new TimeoutException("Closed"));

        m_rhForSystemMessages.close();

        for (DataTransportPair pair : removeAllChannels())
        {
            pair.close();
        }
    }

    protected void failIdentity(Throwable t)
    {
        m_gotIdentity.completeExceptionally(t);
    }

    protected void keepAlive()
    {
        keepAliveImpl();
    }

    private CompletableFuture<Void> keepAliveImpl()
    {
        while (true)
        {
            try
            {
                // Use random to make sure all clients don't call at the same time.
                int nextKeepAlive = (int) (60 + 20 * Math.random());

                await(sleep(nextKeepAlive, TimeUnit.SECONDS));

                if (!isConnectionOpen())
                {
                    break;
                }

                Stopwatch st = Stopwatch.createStarted();

                MbControl_KeepAlive       req   = new MbControl_KeepAlive();
                MbControl_KeepAlive_Reply reply = await(sendSystemMessage(req, MbControl_KeepAlive_Reply.class, null));

                LoggerInstance.debug("Got keep-alive message in %,dmsec", st.elapsed(TimeUnit.MILLISECONDS));

                m_keepAliveFailures = 0;
            }
            catch (Throwable t)
            {
                LoggerInstance.debug("Failed to exchange keep-alive message: %s", t);

                m_keepAliveFailures++;
                if (m_keepAliveFailures >= 5)
                {
                    closeConnection();
                    break;
                }
            }
        }

        return wrapAsync(null);
    }

    private CompletableFuture<EnumSet<JsonConnectionCapability>> exchangeCapabilities(EnumSet<JsonConnectionCapability> available,
                                                                                      EnumSet<JsonConnectionCapability> required)
    {
        try
        {
            MbControl_ExchangeCapabilities req = new MbControl_ExchangeCapabilities();

            req.available = JsonConnectionCapability.encode(available);
            req.required = JsonConnectionCapability.encode(required);

            MbControl_ExchangeCapabilities_Reply reply = await(sendSystemMessage(req, MbControl_ExchangeCapabilities_Reply.class, null));
            return wrapAsync(JsonConnectionCapability.decode(reply.available));
        }
        catch (Exception e)
        {
            return wrapAsync(null);
        }
    }

    public CompletableFuture<List<String>> listChannels() throws
                                                          Exception
    {
        await(m_gotIdentity);

        MbControl_ListChannels       req   = new MbControl_ListChannels();
        MbControl_ListChannels_Reply reply = await(sendSystemMessage(req, MbControl_ListChannels_Reply.class, null));

        if (reply.availableChannels == null)
        {
            throw new RuntimeException("ListChannels command failed");
        }

        return wrapAsync(reply.availableChannels);
    }

    public CompletableFuture<List<String>> listMembers(String channel) throws
                                                                       Exception
    {
        await(m_gotIdentity);

        MbControl_ListMembers req = new MbControl_ListMembers();
        req.channel = channel;

        MbControl_ListMembers_Reply reply = await(sendSystemMessage(req, MbControl_ListMembers_Reply.class, null));

        if (reply.members == null)
        {
            throw new RuntimeException("ListMembers command failed");
        }

        return wrapAsync(reply.members);
    }

    //--//

    public CompletableFuture<String> getEndpointId() throws
                                                     Exception
    {
        await(m_gotIdentity);

        return wrapAsync(m_endpointId);
    }

    public <TRequest, TReply> CompletableFuture<Boolean> join(MessageBusChannelSubscriber<TRequest, TReply> inboundPath) throws
                                                                                                                         Exception
    {
        await(m_gotIdentity);

        //
        // Add to set of joined channels before we send the message,
        // so we can process new requests regardless of the interleaving of asynchronous tasks.
        //
        DataTransportPair pair = addToChannel(inboundPath);
        if (pair == null)
        {
            return wrapAsync(false);
        }

        try
        {
            String channel = inboundPath.getChannelName();

            MbControl_JoinChannel req = new MbControl_JoinChannel();
            req.channel = channel;

            MbControl_JoinChannel_Reply reply = await(sendSystemMessage(req, MbControl_JoinChannel_Reply.class, null));

            if (!reply.success)
            {
                throw new RuntimeException("JoinChannel command failed");
            }

            // Success, don't remove the registration.
            pair = null;

            return wrapAsync(true);
        }
        finally
        {
            if (pair != null)
            {
                // Failure, remove the registration.
                removeFromChannel(inboundPath);

                inboundPath.setTransmitTransport(null);
            }
        }
    }

    public <TRequest> CompletableFuture<Boolean> leave(DataTransport<TRequest> inboundPath) throws
                                                                                            Exception
    {
        await(m_gotIdentity);

        inboundPath.setTransmitTransport(null);

        DataTransportPair pair = removeFromChannel(inboundPath);
        if (pair == null)
        {
            return wrapAsync(false);
        }

        String channelName = inboundPath.getChannelName();

        MbControl_LeaveChannel req = new MbControl_LeaveChannel();
        req.channel = channelName;

        MbControl_LeaveChannel_Reply reply = await(sendSystemMessage(req, MbControl_LeaveChannel_Reply.class, null));

        if (!reply.success)
        {
            throw Exceptions.newRuntimeException("LeaveChannel '%s' command failed", channelName);
        }

        return wrapAsync(true);
    }

    //--//

    protected <TInbound extends MessageBusPayload> CompletableFuture<TInbound> sendSystemMessage(MessageBusPayload data,
                                                                                                 Class<TInbound> replyClass,
                                                                                                 MessageBusPayloadCallback notifyPayloadSize) throws
                                                                                                                                              Exception
    {
        data.assignNewId();

        CompletableFuture<TInbound> replyFuture = m_rhForSystemMessages.createNewReplyHandler(replyClass, 10, TimeUnit.SECONDS, data.messageId);

        contextualLogger.debugVerbose("[MSG: %s] sendSystemMessage: %s", data.messageId, data);

        await(m_transmitTransport.dispatch(data, notifyPayloadSize));

        TInbound reply = await(replyFuture);

        return wrapAsync(reply);
    }

    //--//

    private List<DataTransportPair> removeAllChannels()
    {
        synchronized (m_joinedChannels)
        {
            List<DataTransportPair> pairs = Lists.newArrayList(m_joinedChannels.values());

            m_joinedChannels.clear();

            return pairs;
        }
    }

    private DataTransportPair getChannel(String channel)
    {
        synchronized (m_joinedChannels)
        {
            return m_joinedChannels.get(channel);
        }
    }

    private <TRequest, TReply> DataTransportPair addToChannel(DataTransport<TRequest> inboundPath)
    {
        synchronized (m_joinedChannels)
        {
            String channelName = inboundPath.getChannelName();

            DataTransportPair pair = m_joinedChannels.get(channelName);
            if (pair != null)
            {
                return null;
            }

            SystemToDataTransport<TRequest, TReply> facadeSys = new SystemToDataTransport<>(m_transmitTransport, inboundPath);
            inboundPath.setTransmitTransport(facadeSys.getTransmitTransport());

            pair = new DataTransportPair(facadeSys, inboundPath);

            m_joinedChannels.put(channelName, pair);
            return pair;
        }
    }

    private <TRequest> DataTransportPair removeFromChannel(DataTransport<TRequest> inboundPath)
    {
        synchronized (m_joinedChannels)
        {
            String channelName = inboundPath.getChannelName();

            DataTransportPair pair = m_joinedChannels.get(channelName);
            if (pair == null || pair.m_inboundPath != inboundPath)
            {
                return null;
            }

            m_joinedChannels.remove(channelName);
            return pair;
        }
    }
}
