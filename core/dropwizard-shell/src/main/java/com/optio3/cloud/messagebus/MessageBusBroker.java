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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.validation.constraints.NotNull;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.JsonConnectionCapability;
import com.optio3.cloud.JsonDatagram;
import com.optio3.cloud.JsonWebSocketDnsHints;
import com.optio3.cloud.annotation.Optio3MessageBusChannel;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.messagebus.payload.MbControl;
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
import com.optio3.cloud.messagebus.payload.MbControl_ListSubscriptions;
import com.optio3.cloud.messagebus.payload.MbControl_ListSubscriptions_Reply;
import com.optio3.cloud.messagebus.payload.MbControl_UpgradeToUDP;
import com.optio3.cloud.messagebus.payload.MbControl_UpgradeToUDP_Reply;
import com.optio3.cloud.messagebus.payload.MbData;
import com.optio3.cloud.messagebus.payload.MbData_Message;
import com.optio3.cloud.messagebus.payload.MbData_Message_Reply;
import com.optio3.cloud.messagebus.transport.DataTransport;
import com.optio3.cloud.messagebus.transport.DataTransportWithReplies;
import com.optio3.cloud.messagebus.transport.Endpoint;
import com.optio3.cloud.messagebus.transport.StableIdentity;
import com.optio3.cloud.messagebus.transport.SystemTransport;
import com.optio3.logging.Logger;
import com.optio3.logging.RedirectingLogger;
import com.optio3.serialization.SerializationHelper;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.BoxingUtils;
import com.optio3.util.Encryption;
import com.optio3.util.Exceptions;
import com.optio3.util.IdGenerator;
import com.optio3.util.TimeUtils;

public class MessageBusBroker
{
    public static class ForStaleSessions
    {
    }

    public static final Logger LoggerInstance                 = new Logger(MessageBusBroker.class, true);
    public static final Logger LoggerInstanceForStaleSessions = new Logger(MessageBusBroker.ForStaleSessions.class, false);

    public static final int UDP_PORT = 20443;

    //--//

    private static final DataTransportWithReplies<?, ?> s_placeholderTransport = new DataTransportWithReplies<Object, Object>()
    {
        @Override
        public <T extends Object> CompletableFuture<T> dispatchWithReply(MbData_Message data,
                                                                         Class<T> replyClass,
                                                                         MessageBusPayloadCallback notifyPayloadSize,
                                                                         int timeoutForReply,
                                                                         TimeUnit timeoutUnit) throws
                                                                                               Exception
        {
            CompletableFuture<T> f = new CompletableFuture<>();
            f.completeExceptionally(new TimeoutException("Closed"));
            return f;
        }

        @Override
        public void close()
        {
        }

        @Override
        public String getChannelName()
        {
            return null;
        }

        @Override
        public String getEndpointId()
        {
            return null;
        }

        @Override
        public void setTransmitTransport(DataTransportWithReplies<?, ?> transport)
        {
        }

        @Override
        public Endpoint getEndpointForDestination(String destination)
        {
            return null;
        }

        @Override
        public CompletableFuture<Void> dispatchWithNoReply(MbData data,
                                                           MessageBusPayloadCallback notifyPayloadSize) throws
                                                                                                        Exception
        {
            return null;
        }
    };

    public static <TRequest, TReply> DataTransportWithReplies<TRequest, TReply> getPlaceholderTransport()
    {
        @SuppressWarnings("unchecked") DataTransportWithReplies<TRequest, TReply> placeholderTransport = (DataTransportWithReplies<TRequest, TReply>) s_placeholderTransport;
        return placeholderTransport;
    }

    //--//

    private final class StableIdentityImpl extends StableIdentity
    {
        private void flushEndpoint(EndpointImpl ep,
                                   boolean updateTimestamp)
        {
            if (updateTimestamp)
            {
                updateTimestamp();
            }

            accumulate(ep.m_statistics);
            ep.m_statistics.clear();
        }
    }

    private final class EndpointImpl implements Endpoint
    {
        private final ReceiveSideOfSystemTransport m_receiveSideOfSystemTransport;
        private       String                       m_contextSysId;
        private       String                       m_contextInstanceId;
        private       StableIdentityImpl           m_stableIdentity;
        private final StableIdentity.Statistics    m_statistics = new StableIdentity.Statistics();
        private       long                         m_lastIncomingMessageUTC;

        private EndpointImpl(ReceiveSideOfSystemTransport receiveSideOfSystemTransport)
        {
            m_receiveSideOfSystemTransport = receiveSideOfSystemTransport;
        }

        private void close()
        {
            m_receiveSideOfSystemTransport.close();

            if (m_stableIdentity != null)
            {
                // Only update the timestamp for incoming messages.
                m_stableIdentity.flushEndpoint(this, false);
            }
        }

        @Override
        public SystemTransport getTransport()
        {
            return m_receiveSideOfSystemTransport;
        }

        @Override
        public void recordConnection()
        {
            m_statistics.connections++;
        }

        @Override
        public void recordIncomingMessage(int size)
        {
            m_lastIncomingMessageUTC = TimeUtils.nowMilliUtc();

            m_statistics.messagesRx++;
            m_statistics.bytesRx += size;

            if (m_stableIdentity != null)
            {
                m_stableIdentity.flushEndpoint(this, true);
            }
        }

        @Override
        public void recordOutgoingMessage(int size)
        {
            m_statistics.messagesTx++;
            m_statistics.bytesTx += size;

            if (m_stableIdentity != null)
            {
                // Only update the timestamp for incoming messages.
                m_stableIdentity.flushEndpoint(this, false);
            }
        }

        @Override
        public void setContext(String sysId,
                               String instanceId)
        {
            m_contextSysId      = sysId;
            m_contextInstanceId = instanceId;
        }

        @Override
        public String getContextRecordId()
        {
            return m_contextSysId;
        }

        @Override
        public String getContextInstanceId()
        {
            return m_contextInstanceId;
        }

        @Override
        public StableIdentity ensureIdentity(String id)
        {
            StableIdentityImpl stableIdentity;

            synchronized (m_lock)
            {
                stableIdentity = m_stableIdentities.get(id);
                if (stableIdentity == null)
                {
                    stableIdentity = new StableIdentityImpl();
                    m_stableIdentities.put(id, stableIdentity);
                }
            }

            m_stableIdentity = stableIdentity;

            return stableIdentity;
        }

        @Override
        public StableIdentity getIdentity()
        {
            return m_stableIdentity;
        }

        @Override
        public long getTimestampOfLastIncomingMessage()
        {
            return m_lastIncomingMessageUTC;
        }
    }

    //--//

    final class ReceiveSideOfSystemTransport implements SystemTransport
    {
        private final ContextualLogger contextualLogger = new ContextualLogger(LoggerInstance)
        {
            @Override
            public String getPrefix()
            {
                return MessageBusBroker.formatPrefix(MessageBusBroker.this.contextualLogger, "RECEIVE EP", getEndpointId());
            }
        };

        private final SystemTransport m_outboundPath;

        private final Set<String> m_subscribedChannels = Sets.newHashSet();

        ReceiveSideOfSystemTransport(SystemTransport outboundPath)
        {
            m_outboundPath = outboundPath;
        }

        //--//

        @Override
        public void close()
        {
            for (String channel : getSubscribedChannels())
            {
                MessageBusChannel ch = getChannel(channel);
                if (ch != null)
                {
                    removeChannel(ch);
                }
            }
        }

        @Override
        public boolean isOpen()
        {
            return m_outboundPath.isOpen();
        }

        @Override
        public boolean isService()
        {
            return m_outboundPath.isService();
        }

        @Override
        public void markAsActive()
        {
            m_outboundPath.markAsActive();
        }

        @Override
        public String getEndpointId()
        {
            return m_outboundPath.getEndpointId();
        }

        @Override
        public String getPurposeInfo()
        {
            return "Receive side of " + m_outboundPath.getPurposeInfo();
        }

        @Override
        public EnumSet<JsonConnectionCapability> exchangeCapabilities(EnumSet<JsonConnectionCapability> available,
                                                                      EnumSet<JsonConnectionCapability> required)
        {
            return m_outboundPath.exchangeCapabilities(available, required);
        }

        @Override
        public TransportSecurityPolicy getPolicy()
        {
            return m_outboundPath.getPolicy();
        }

        @Override
        public @NotNull CookiePrincipal getTransportPrincipal()
        {
            return m_outboundPath.getTransportPrincipal();
        }

        @Override
        public Endpoint getEndpointForDestination(String destination)
        {
            return MessageBusBroker.this.getEndpoint(destination);
        }

        @Override
        public CompletableFuture<Void> dispatch(MessageBusPayload msg,
                                                MessageBusPayloadCallback notifyPayloadSize) throws
                                                                                             Exception
        {
            try
            {
                if (msg instanceof MbControl)
                {
                    if (msg instanceof MbControl_KeepAlive)
                    {
                        MbControl_KeepAlive_Reply reply = new MbControl_KeepAlive_Reply();
                        reply.messageId = msg.messageId;

                        return m_outboundPath.dispatch(reply, notifyPayloadSize);
                    }

                    if (msg instanceof MbControl_GetIdentity)
                    {
                        MbControl_GetIdentity_Reply reply = new MbControl_GetIdentity_Reply();
                        reply.messageId = msg.messageId;

                        reply.endpointIdentity = m_outboundPath.getEndpointId();

                        return m_outboundPath.dispatch(reply, notifyPayloadSize);
                    }

                    if (msg instanceof MbControl_ExchangeCapabilities)
                    {
                        MbControl_ExchangeCapabilities       req   = (MbControl_ExchangeCapabilities) msg;
                        MbControl_ExchangeCapabilities_Reply reply = new MbControl_ExchangeCapabilities_Reply();
                        reply.messageId = msg.messageId;

                        EnumSet<JsonConnectionCapability> available = JsonConnectionCapability.decode(req.available);
                        EnumSet<JsonConnectionCapability> required  = JsonConnectionCapability.decode(req.required);

                        reply.available = JsonConnectionCapability.encode(m_outboundPath.exchangeCapabilities(available, required));

                        return m_outboundPath.dispatch(reply, notifyPayloadSize);
                    }

                    if (msg instanceof MbControl_UpgradeToUDP)
                    {
                        MbControl_UpgradeToUDP       req   = (MbControl_UpgradeToUDP) msg;
                        MbControl_UpgradeToUDP_Reply reply = new MbControl_UpgradeToUDP_Reply();
                        reply.messageId = msg.messageId;

                        evaluateDatagramRequest(req, reply);

                        return m_outboundPath.dispatch(reply, notifyPayloadSize);
                    }

                    if (msg instanceof MbControl_ListChannels)
                    {
                        MbControl_ListChannels_Reply reply = new MbControl_ListChannels_Reply();
                        reply.messageId = msg.messageId;

                        reply.availableChannels = await(listPeerChannels());

                        return m_outboundPath.dispatch(reply, notifyPayloadSize);
                    }

                    if (msg instanceof MbControl_ListSubscriptions)
                    {
                        MbControl_ListSubscriptions_Reply reply = new MbControl_ListSubscriptions_Reply();
                        reply.messageId = msg.messageId;

                        reply.subscribedChannels = getSubscribedChannels();

                        return m_outboundPath.dispatch(reply, notifyPayloadSize);
                    }

                    if (msg instanceof MbControl_JoinChannel)
                    {
                        MbControl_JoinChannel       req   = (MbControl_JoinChannel) msg;
                        MbControl_JoinChannel_Reply reply = new MbControl_JoinChannel_Reply();
                        reply.messageId = req.messageId;

                        MessageBusChannel ch = getChannel(req.channel);
                        if (ch != null && ch.getPolicy()
                                            .canJoin(getTransportPrincipal()))
                        {
                            addChannel(ch);
                            reply.success = true;
                        }

                        return m_outboundPath.dispatch(reply, notifyPayloadSize);
                    }

                    if (msg instanceof MbControl_LeaveChannel)
                    {
                        MbControl_LeaveChannel       req   = (MbControl_LeaveChannel) msg;
                        MbControl_LeaveChannel_Reply reply = new MbControl_LeaveChannel_Reply();
                        reply.messageId = req.messageId;

                        MessageBusChannel ch = getChannel(req.channel);
                        if (ch != null)
                        {
                            removeChannel(ch);
                            reply.success = true;
                        }

                        return m_outboundPath.dispatch(reply, notifyPayloadSize);
                    }

                    if (msg instanceof MbControl_ListMembers)
                    {
                        MbControl_ListMembers       req   = (MbControl_ListMembers) msg;
                        MbControl_ListMembers_Reply reply = new MbControl_ListMembers_Reply();
                        reply.messageId = req.messageId;

                        if (isChannelSubscribed(req.channel))
                        {
                            MessageBusChannel ch = getChannel(req.channel);
                            if (ch != null && ch.getPolicy()
                                                .canListMembers(getTransportPrincipal()))
                            {
                                reply.members = await(listPeerMembers(req.channel));
                            }
                        }

                        return m_outboundPath.dispatch(reply, notifyPayloadSize);
                    }

                    return wrapAsync(null);
                }

                if (msg instanceof MbData)
                {
                    MbData  data    = (MbData) msg;
                    boolean isReply = msg instanceof MbData_Message_Reply;

                    contextualLogger.debug("Received a message (reply=%s) for channel %s: %s", isReply, data.channel, data);

                    if (!isChannelSubscribed(data.channel))
                    {
                        // Drop messages for unknown or unsubscribed channels on the floor.
                        contextualLogger.info("Received a message (reply=%s) for a non-subscribed channel %s: %s", isReply, data.channel, data);
                        return wrapAsync(null);
                    }

                    routeMessage(this, data, notifyPayloadSize);

                    return wrapAsync(null);
                }

                return wrapAsync(null);
            }
            catch (Exception e)
            {
                contextualLogger.error("Failed to dispatch message %s due to exception: %s", msg, e);
                throw e;
            }
        }

        private void evaluateDatagramRequest(MbControl_UpgradeToUDP req,
                                             MbControl_UpgradeToUDP_Reply reply)
        {
            if (m_datagram != null)
            {
                if (req.isIntel && !m_enableDatagramIntel)
                {
                    return;
                }

                switch (req.version)
                {
                    case JsonDatagram.PROTOCOL_V1:
                    {
                        reply.port      = UDP_PORT;
                        reply.headerId  = m_sharedSession.headerId;
                        reply.headerKey = m_sharedSession.headerKey;

                        reply.sessionId       = Encryption.generateRandomValue64Bit();
                        reply.sessionKey      = Encryption.generateRandomValues(Encryption.AES128SingleBlock.BLOCKSIZE);
                        reply.sessionValidity = 86400;

                        reply.endpointId = IdGenerator.newGuid();

                        JsonDatagram.SessionConfiguration cfg = new JsonDatagram.SessionConfiguration(reply, false);
                        cfg.principal = req.principal;

                        newDatagramSession(cfg);
                    }
                }
            }
        }

        private void newDatagramSession(JsonDatagram.SessionConfiguration cfg)
        {
            MessageBusServerDatagram session = new MessageBusServerDatagram(MessageBusBroker.this, cfg)
            {
                private boolean m_connected;

                @Override
                protected void receivedKeepAlive()
                {
                    if (!m_connected)
                    {
                        m_connected = true;
                        onConnect();
                    }

                    activateKeepAlive();
                }

                @Override
                protected void sendFrame(OutputBuffer ob)
                {
                    if (m_lastSource != null)
                    {
                        recordTxPacket(ob);
                        m_datagram.sendPacket(m_lastSource, ob.toByteArray());
                    }
                }

                @Override
                protected void unregister()
                {
                    synchronized (m_datagramSessions)
                    {
                        m_datagramSessions.remove(cfg.sessionId);
                    }
                }

                @Override
                protected void shutdown()
                {
                    unregister();

                    close();
                }
            };

            synchronized (m_datagramSessions)
            {
                m_datagramSessions.put(cfg.sessionId, session);
            }
        }

        //--//

        List<String> getSubscribedChannels()
        {
            synchronized (m_subscribedChannels)
            {
                return Lists.newArrayList(m_subscribedChannels);
            }
        }

        boolean isChannelSubscribed(String channel)
        {
            synchronized (m_subscribedChannels)
            {
                return m_subscribedChannels.contains(channel);
            }
        }

        void addChannel(MessageBusChannel ch)
        {
            synchronized (m_subscribedChannels)
            {
                m_subscribedChannels.add(ch.getChannelName());
            }

            ch.join(m_outboundPath);
        }

        void removeChannel(MessageBusChannel ch)
        {
            synchronized (m_subscribedChannels)
            {
                m_subscribedChannels.remove(ch.getChannelName());
            }

            ch.leave(m_outboundPath);
        }
    }

    //--//

    final ContextualLogger contextualLogger = new ContextualLogger(LoggerInstance)
    {
        @Override
        public String getPrefix()
        {
            return MessageBusBroker.formatPrefix("BROKER", m_brokerIdentity);
        }
    };

    static String formatPrefix(String ctx,
                               String id)
    {
        return String.format("[%-10s: %s]", ctx, id);
    }

    static String formatPrefix(RedirectingLogger outerLogger,
                               String ctx,
                               String id)
    {
        return String.format("%s [%-10s: %s]", outerLogger.getPrefix(), ctx, id);
    }

    //--//

    private final Object m_lock = new Object();

    private final String m_brokerIdentity    = IdGenerator.newGuid();
    private final String m_serviceEndpointId = String.format("%s@%s", WellKnownDestination.Service.getId(), m_brokerIdentity);

    private JsonDatagram.SessionConfiguration m_sharedSession;

    private       JsonDatagram.DatagramSocketWorker   m_datagram;
    private       boolean                             m_enableDatagramIntel;
    private final Map<Long, MessageBusServerDatagram> m_datagramSessions = Maps.newHashMap();

    private final Map<String, MessageBusChannel>  m_channels                 = Maps.newHashMap();
    private final Map<String, EndpointImpl>       m_endpoints                = Maps.newHashMap();
    private final Map<String, StableIdentityImpl> m_stableIdentities         = Maps.newHashMap();
    private final MessageTracker                  m_duplicateTracker_request = new MessageTracker(10240);
    private final MessageTracker                  m_duplicateTracker_reply   = new MessageTracker(10240);

    private PeeringProvider m_peers;

    //--//

    public MessageBusBroker()
    {
    }

    public void startUdpWorker(boolean enableIntel)
    {
        m_enableDatagramIntel = enableIntel;

        if (m_datagram == null)
        {
            m_sharedSession = new JsonDatagram.SessionConfiguration(UDP_PORT);

            m_datagram = new JsonDatagram.DatagramSocketWorker(UDP_PORT)
            {
                @Override
                protected void processPacket(InetSocketAddress source,
                                             InputBuffer ib)
                {
                    try
                    {
                        JsonDatagram.PublicHeader publicHeader = m_sharedSession.decodePublicHeader(ib);
                        if (publicHeader != null)
                        {
                            boolean stale = true;

                            if (m_sharedSession.matchPublicHeader(publicHeader))
                            {
                                JsonDatagram.SharedHeader sharedHeader = m_sharedSession.decodeSharedHeader(ib);
                                if (sharedHeader != null)
                                {
                                    MessageBusServerDatagram session;

                                    synchronized (m_datagramSessions)
                                    {
                                        session = m_datagramSessions.get(sharedHeader.sessionId);
                                    }

                                    if (session != null)
                                    {
                                        if (sharedHeader.close)
                                        {
                                            session.shutdown();
                                        }
                                        else
                                        {
                                            session.processFrame(source, sharedHeader, ib);
                                        }

                                        stale = false;
                                    }
                                }
                            }

                            if (stale)
                            {
                                LoggerInstanceForStaleSessions.debug("Detected stale session from %s: %d / %d", source, publicHeader.version, publicHeader.transportId);

                                publicHeader.transportId = (short) ~publicHeader.transportId;

                                try (OutputBuffer ob = new OutputBuffer())
                                {
                                    SerializationHelper.write(ob, publicHeader);

                                    m_datagram.sendPacket(source, ob.toByteArray());
                                }
                            }
                        }
                    }
                    finally
                    {
                        ib.close();
                    }
                }
            };
        }
    }

    public String getBrokerId()
    {
        return m_brokerIdentity;
    }

    public Map<String, StableIdentity> getIdentities()
    {
        Map<String, StableIdentity> res = Maps.newHashMap();

        synchronized (m_lock)
        {
            res.putAll(m_stableIdentities);
        }

        return res;
    }

    public List<MessageBusDatagramSession> reportSessions()
    {
        List<MessageBusDatagramSession> results = Lists.newArrayList();

        synchronized (m_datagramSessions)
        {
            m_datagramSessions.forEach((sessionId, datagram) ->
                                       {
                                           var sd = new MessageBusDatagramSession();
                                           sd.sessionId = String.format("%08X", (int) (long) sessionId);
                                           datagram.report(sd);

                                           results.add(sd);
                                       });
        }

        results.sort((a, b) -> TimeUtils.compare(a.lastPacket, b.lastPacket));

        return results;
    }

    public List<String> listChannels()
    {
        synchronized (m_lock)
        {
            return Lists.newArrayList(m_channels.keySet());
        }
    }

    public MessageBusChannel getChannel(String channel)
    {
        synchronized (m_lock)
        {
            return m_channels.get(channel);
        }
    }

    public <C extends MessageBusChannelProvider<?, ?>> C getChannelProvider(Class<C> t)
    {
        Optio3MessageBusChannel anno = t.getAnnotation(Optio3MessageBusChannel.class);
        if (anno == null)
        {
            throw Exceptions.newRuntimeException("Missing @Optio3MessageBusChannel on type '%s'", t);
        }

        MessageBusChannel ch = getChannel(anno.name());
        if (ch == null)
        {
            return null;
        }

        return t.cast(ch.getProvider());
    }

    public CompletableFuture<String> connectToPeer(JsonWebSocketDnsHints dnsHints,
                                                   String url,
                                                   String username,
                                                   String password) throws
                                                                    Exception
    {
        if (m_peers != null)
        {
            return m_peers.connectAsClient(dnsHints, url, username, password);
        }

        return AsyncRuntime.asNull();
    }

    public CompletableFuture<List<String>> listPeerChannels()
    {
        return m_peers.listChannels(null);
    }

    public CompletableFuture<List<String>> listPeerMembers(String channelName)
    {
        return m_peers.listMembers(null, channelName);
    }

    public Endpoint getEndpoint(String identity)
    {
        synchronized (m_lock)
        {
            return m_endpoints.get(identity);
        }
    }

    //--//

    void setPeeringProvider(PeeringProvider peers)
    {
        m_peers = peers;
    }

    public <TRequest, TReply> void registerChannelProvider(String endpointId,
                                                           DataTransport<TRequest> inboundPath,
                                                           ChannelSecurityPolicy security)
    {
        String            channelName = inboundPath.getChannelName();
        MessageBusChannel ch          = new MessageBusChannel(channelName, inboundPath, security);

        synchronized (m_lock)
        {
            if (m_channels.putIfAbsent(channelName, ch) != null)
            {
                throw Exceptions.newIllegalArgumentException("Channel %s already registered", channelName);
            }
        }

        final String  effectiveId = BoxingUtils.get(endpointId, m_serviceEndpointId);
        final boolean isService   = SystemTransport.isService(effectiveId);

        SystemTransport transmitTransport = new SystemTransport()
        {
            @Override
            public void close()
            {
                // Nothing to do.
            }

            @Override
            public boolean isOpen()
            {
                // Always open.
                return true;
            }

            @Override
            public boolean isService()
            {
                return isService;
            }

            @Override
            public void markAsActive()
            {
                // Nothing to do.
            }

            @Override
            public String getEndpointId()
            {
                return effectiveId;
            }

            @Override
            public String getPurposeInfo()
            {
                return "Channel provider";
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
                return MessageBusBroker.this.getEndpoint(destination);
            }

            @Override
            public CompletableFuture<Void> dispatch(MessageBusPayload msg,
                                                    MessageBusPayloadCallback notifyPayloadSize)
            {
                routeMessage(this, (MbData) msg, notifyPayloadSize);

                return wrapAsync(null);
            }
        };

        //
        // Create the inbound and outbound pipelines.
        //
        // SystemToDataTransport is the pipeline from the channel to the provider.
        // SystemToDataTransport.Outbound is the pipeline from the provider to the broker.
        //
        // Connect the inbound pipeline to the channel.
        //
        //
        SystemToDataTransport<TRequest, TReply> facadeSys = new SystemToDataTransport<TRequest, TReply>(transmitTransport, inboundPath);
        inboundPath.setTransmitTransport(facadeSys.getTransmitTransport());

        ch.join(facadeSys);
    }

    public <TRequest, TReply> void registerLocalChannelSubscriber(MessageBusChannelSubscriber<TRequest, TReply> subscriber)
    {
        String            channelName = subscriber.getChannelName();
        MessageBusChannel ch          = getChannel(channelName);
        if (ch == null)
        {
            throw Exceptions.newIllegalArgumentException("Channel %s not registered", channelName);
        }

        SystemTransport transmitTransport = new SystemTransport()
        {
            private final String m_endpointId = IdGenerator.newGuid();

            @Override
            public void close()
            {
                // Nothing to do.
            }

            @Override
            public boolean isOpen()
            {
                // Always open.
                return true;
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
                return m_endpointId;
            }

            @Override
            public String getPurposeInfo()
            {
                return "Channel subscriber";
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
                return MessageBusBroker.this.getEndpoint(destination);
            }

            @Override
            public CompletableFuture<Void> dispatch(MessageBusPayload msg,
                                                    MessageBusPayloadCallback notifyPayloadSize)
            {
                routeMessage(this, (MbData) msg, notifyPayloadSize);

                return wrapAsync(null);
            }
        };

        //
        // Create the inbound and outbound pipelines.
        //
        // SystemToDataTransport is the pipeline from the channel to the provider.
        // SystemToDataTransport.Outbound is the pipeline from the provider to the broker.
        //
        // Connect the inbound pipeline to the channel.
        //
        //
        SystemToDataTransport<TRequest, TReply> facadeSys = new SystemToDataTransport<TRequest, TReply>(transmitTransport, subscriber);
        subscriber.setTransmitTransport(facadeSys.getTransmitTransport());

        ch.join(facadeSys);
    }

    Endpoint registerSystemTransport(SystemTransport endpointTx)
    {
        ReceiveSideOfSystemTransport endpointRx = new ReceiveSideOfSystemTransport(endpointTx);
        EndpointImpl                 ep         = new EndpointImpl(endpointRx);

        synchronized (m_lock)
        {
            m_endpoints.put(endpointTx.getEndpointId(), ep);
        }

        return ep;
    }

    void unregisterSystemTransport(SystemTransport transportTx)
    {
        EndpointImpl ep;

        synchronized (m_lock)
        {
            ep = m_endpoints.remove(transportTx.getEndpointId());
        }

        if (ep != null)
        {
            ep.close();
        }
    }

    //--//

    void routeMessage(SystemTransport connection,
                      MbData data,
                      MessageBusPayloadCallback notifyPayloadSize)
    {
        boolean forwardToPeers;

        boolean        isReply = data instanceof MbData_Message_Reply;
        MessageTracker tracker = isReply ? m_duplicateTracker_reply : m_duplicateTracker_request;
        if (!tracker.track(data.messageId))
        {
            contextualLogger.debug("Received a duplicate message (reply=%s) for channel %s: %s", isReply, data.channel, data);
            return;
        }

        MessageBusChannel ch = getChannel(data.channel);
        if (ch != null)
        {
            contextualLogger.debug("[CH: %s] distributeToSubscribers: %s -> %s", data.channel, data.messageId, data.destination);

            CookiePrincipal       principal = connection != null ? connection.getTransportPrincipal() : CookiePrincipal.createEmpty();
            ChannelSecurityPolicy policy    = ch.getPolicy();

            if (!policy.canSend(principal, data))
            {
                // Drop message due to policy.
                contextualLogger.debug("[CH: %s] distributeToSubscribers: %s -> %s: Endpoint can't send", data.channel, data.messageId, data.destination);
                return;
            }

            boolean isForLocalService = MbData.isForLocalService(data.destination);
            boolean isForServices     = MbData.isForServices(data.destination);
            boolean isBroadcast       = MbData.isBroadcast(data.destination);

            if (isBroadcast && !policy.canSendBroadcast(principal, data))
            {
                // Drop message due to policy.
                contextualLogger.debug("[CH: %s] distributeToSubscribers: %s -> %s: Endpoint can't send broadcast", data.channel, data.messageId, data.destination);
                return;
            }

            boolean delivered = false;

            for (SystemTransport st : ch.getTransports())
            {
                if (!st.isOpen())
                {
                    // Remove a dead subscriber, just in case 'leave' did not trigger.
                    ch.leave(st);
                    continue;
                }

                String transportId = st.getEndpointId();

                contextualLogger.debug("[CH: %s] distributeToSubscribers: Checking %s...", data.channel, transportId);

                if (transportId.equals(data.origin))
                {
                    // Don't send message back to the same endpoint that sent it.
                    continue;
                }

                boolean accept = false;

                accept |= (isForLocalService || isForServices) && st.isService();
                accept |= transportId.equals(data.destination);
                accept |= isBroadcast;

                if (accept)
                {
                    try
                    {
                        contextualLogger.debug("[CH: %s] distributeToSubscribers: deliver to %s", data.channel, transportId);

                        delivered = true;

                        // Don't wait, we don't need to see the result.
                        st.dispatch(data, notifyPayloadSize);
                    }
                    catch (Exception e)
                    {
                        contextualLogger.error("[CH: %s] distributeToSubscribers: delivery to %s failed: %s", data.channel, transportId, e);
                    }
                }
            }

            forwardToPeers = (isBroadcast || isForServices) || !delivered;
        }
        else
        {
            // Only send to peers if it's a known channel.
            forwardToPeers = false;
        }

        if (forwardToPeers && m_peers != null)
        {
            m_peers.forwardMessage(data);
        }
    }
}
