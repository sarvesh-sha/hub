/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.AsyncBackground;
import com.optio3.cloud.JsonWebSocket;
import com.optio3.cloud.JsonWebSocketDnsHints;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.messagebus.channel.RpcWorker;
import com.optio3.cloud.messagebus.payload.MbControl_UpgradeToUDP;
import com.optio3.cloud.messagebus.payload.MbControl_UpgradeToUDP_Reply;
import com.optio3.cloud.messagebus.payload.MbData;
import com.optio3.cloud.messagebus.payload.MbData_Message;
import com.optio3.cloud.messagebus.payload.MbData_Message_Reply;
import com.optio3.cloud.messagebus.payload.peering.ExchangeBrokerIdentity;
import com.optio3.cloud.messagebus.payload.peering.ExchangeBrokerIdentityReply;
import com.optio3.cloud.messagebus.payload.peering.ForwardMessage;
import com.optio3.cloud.messagebus.payload.peering.ListChannels;
import com.optio3.cloud.messagebus.payload.peering.ListChannelsReply;
import com.optio3.cloud.messagebus.payload.peering.ListMembers;
import com.optio3.cloud.messagebus.payload.peering.ListMembersReply;
import com.optio3.cloud.messagebus.payload.peering.Peering;
import com.optio3.cloud.messagebus.payload.peering.PeeringReply;
import com.optio3.cloud.messagebus.transport.DataTransport;
import com.optio3.cloud.messagebus.transport.DataTransportWithReplies;
import com.optio3.cloud.messagebus.transport.Endpoint;
import com.optio3.cloud.messagebus.transport.SystemTransport;
import com.optio3.logging.Logger;
import com.optio3.serialization.Reflection;
import com.optio3.service.IServiceProvider;
import com.optio3.util.CollectionUtils;

public class PeeringProvider
{
    public static final Logger LoggerInstance = MessageBusBroker.LoggerInstance.createSubLogger(PeeringProvider.class);

    public static final String CHANNEL_NAME = "<<PEERING>>";

    private class PeerInfo implements AutoCloseable
    {
        String brokerIdentity;

        //--//

        private MessageBusClient m_clientSocket;
        private ClientSide       m_clientSubscriber;

        //--//

        private final Set<String> m_serverEndpoints = Sets.newHashSet();

        //--//

        @Override
        public void close()
        {
            MessageBusClient clientSocket = m_clientSocket;
            if (clientSocket != null)
            {
                m_clientSocket = null;
                clientSocket.closeConnection();
            }

            m_clientSubscriber = null;
        }

        //--//

        void addServerEndpoint(String serverEndpoint)
        {
            m_serverEndpoints.add(serverEndpoint);
        }

        boolean removeServerEndpoint(String serverEndpoint)
        {
            m_serverEndpoints.remove(serverEndpoint);

            // Can remove?
            return m_serverEndpoints.isEmpty() && m_clientSocket == null;
        }

        CompletableFuture<Void> connectAsClient(JsonWebSocketDnsHints dnsHints,
                                                String url,
                                                String username,
                                                String password) throws
                                                                 Exception
        {
            MessageBusClient socket = new MessageBusClientWebSocket(dnsHints, url, username, password)
            {
                @Override
                public boolean shouldUpgrade()
                {
                    return false;
                }

                @Override
                public boolean prepareUpgrade(RpcWorker rpcWorker)
                {
                    return false;
                }

                @Override
                protected MbControl_UpgradeToUDP completeUpgradeRequest(MbControl_UpgradeToUDP req)
                {
                    return null; // We don't want to upgrade to UDP for the peering protocol.
                }

                @Override
                protected void completeUpgradeResponse(MbControl_UpgradeToUDP_Reply res,
                                                       List<InetAddress> addresses)
                {
                    // Nothing to do.
                }
            };

            try
            {
                ClientSide subscriber = PeeringProvider.this.new ClientSide();

                contextualLogger.debug("connectAsClient: try to contact peer at %s", url);

                socket.startConnection();

                String id = await(socket.getEndpointId());

                contextualLogger.debug("connectAsClient: assigned ID %s", id);

                await(socket.join(subscriber));

                ExchangeBrokerIdentity cmd = new ExchangeBrokerIdentity();
                cmd.brokerIdentity = m_broker.getBrokerId();
                ExchangeBrokerIdentityReply reply = await(subscriber.sendMessageWithReply(WellKnownDestination.Service.getId(), cmd, ExchangeBrokerIdentityReply.class, null, 10, TimeUnit.SECONDS));

                contextualLogger.debug("connectAsClient: peer url %s, broker=%s", url, reply.brokerIdentity);

                brokerIdentity = reply.brokerIdentity;

                m_clientSocket = socket;
                m_clientSubscriber = subscriber;

                socket = null;
            }
            catch (Exception e)
            {
                contextualLogger.error("Failed to establish identity of peer broker %s: %s", url, e);
                throw e;
            }
            finally
            {
                if (socket != null)
                {
                    await(moveOutOfThisThread()); // See below...
                    socket.closeConnection();
                }
            }

            return wrapAsync(null);
        }

        //
        // All this method does is to move away from the calling thread.
        // This is required to close the socket used for connecting with the peer,
        // because the continuation resurfaced on a thread owned by the HttpClient library,
        // which tries to close itself on socket shutdown... 
        //
        @AsyncBackground
        private CompletableFuture<Void> moveOutOfThisThread()
        {
            return wrapAsync(null);
        }

        public <T extends PeeringReply> CompletableFuture<T> sendMessageWithReply(Peering msg,
                                                                                  Class<T> replyClass,
                                                                                  int timeoutForReply,
                                                                                  TimeUnit timeoutUnit) throws
                                                                                                        Exception
        {
            contextualLogger.debug("sendMessageWithReply: %s", msg);

            try
            {
                if (m_clientSubscriber != null)
                {
                    return m_clientSubscriber.sendMessageWithReply(WellKnownDestination.Service.getId(), msg, replyClass, null, timeoutForReply, timeoutUnit);
                }

                for (String serverEndpoint : m_serverEndpoints)
                {
                    // Found one, deliver message through it and exit.
                    MbData_Message data = new MbData_Message();
                    data.destination = serverEndpoint;
                    data.convertPayload(msg);

                    return m_serverSide.m_outboundPath.dispatchWithReply(data, replyClass, null, timeoutForReply, timeoutUnit);
                }
            }
            catch (Exception e)
            {
                contextualLogger.error("Failed to dispatch message %s due to exception: %s", msg, e);
                throw e;
            }

            return noRoute();
        }

        public CompletableFuture<Void> sendMessageWithNoReply(Peering msg) throws
                                                                           Exception
        {
            contextualLogger.debug("sendMessageWithNoReply: %s", msg);

            if (m_clientSubscriber != null)
            {
                return m_clientSubscriber.sendMessageWithNoReply(WellKnownDestination.Service.getId(), msg, null);
            }

            for (String serverEndpoint : m_serverEndpoints)
            {
                // Found one, deliver message through it and exit.
                MbData_Message data = new MbData_Message();
                data.destination = serverEndpoint;
                data.convertPayload(msg);

                return m_serverSide.m_outboundPath.dispatchWithNoReply(data, null);
            }

            return noRoute();
        }

        private <T> CompletableFuture<T> noRoute()
        {
            CompletableFuture<T> res = new CompletableFuture<>();
            res.completeExceptionally(new RuntimeException("No route to peer " + brokerIdentity));
            return res;
        }
    }

    private class ServerSide implements DataTransport<Peering>,
                                        ChannelLifecycle
    {
        private DataTransportWithReplies<Peering, PeeringReply> m_outboundPath = MessageBusBroker.getPlaceholderTransport();

        //--//

        @Override
        public void close()
        {
            // Nothing to do.
        }

        @Override
        public String getChannelName()
        {
            return CHANNEL_NAME;
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

            @SuppressWarnings("unchecked") DataTransportWithReplies<Peering, PeeringReply> outboundPath = (DataTransportWithReplies<Peering, PeeringReply>) transport;

            m_outboundPath = outboundPath;
        }

        //--//

        @Override
        public Endpoint getEndpointForDestination(String destination)
        {
            return m_broker.getEndpoint(destination);
        }

        @Override
        public CompletableFuture<Void> dispatchWithNoReply(MbData data,
                                                           MessageBusPayloadCallback notifyPayloadSize) throws
                                                                                                        Exception
        {
            contextualLogger.debug("ServerSide-receivedMessage: %s/%s", data.channel, data.messageId);

            Peering obj = JsonWebSocket.deserializeValue(Peering.class, data.payload);

            return PeeringProvider.this.receivedMessage(m_outboundPath, (MbData_Message) data, obj);
        }

        //--//

        @Override
        public void onJoin(SystemTransport transport)
        {
            // Nothing to do.
        }

        @Override
        public void onLeave(SystemTransport transport)
        {
            String serverEndpoint = transport.getEndpointId();

            contextualLogger.debug("onLeave: %s", serverEndpoint);

            synchronized (m_peers)
            {
                for (PeerInfo pi : grabPeers())
                {
                    if (pi.removeServerEndpoint(serverEndpoint))
                    {
                        m_peers.remove(pi.brokerIdentity);
                    }
                }

                refreshActivePeers();
            }
        }
    }

    private class ClientSide extends MessageBusChannelSubscriber<Peering, PeeringReply>
    {
        ClientSide()
        {
            super(CHANNEL_NAME);
        }

        @Override
        protected CompletableFuture<Void> receivedMessage(MbData_Message data,
                                                          Peering obj) throws
                                                                       Exception
        {
            contextualLogger.debug("ClientSide-receivedMessage: %s/%s", data.channel, data.messageId);

            return PeeringProvider.this.receivedMessage(getTransmitTransport(), data, obj);
        }
    }

    //--//

    private final ContextualLogger contextualLogger = new ContextualLogger(LoggerInstance)
    {
        @Override
        public String getPrefix()
        {
            return MessageBusBroker.formatPrefix("PEER", m_broker.getBrokerId());
        }
    };

    private final Map<String, PeerInfo> m_peers       = Maps.newHashMap();
    private       List<PeerInfo>        m_peersActive = Collections.emptyList();

    //--//

    private final MessageBusBroker m_broker;
    private       ServerSide       m_serverSide;

    //--//

    private PeeringProvider(MessageBusBroker broker)
    {
        m_broker = broker;
    }

    public static void init(IServiceProvider provider)
    {
        MessageBusBroker broker = provider.getServiceNonNull(MessageBusBroker.class);
        PeeringProvider  peers  = new PeeringProvider(broker);

        broker.setPeeringProvider(peers);

        peers.m_serverSide = peers.new ServerSide();
        broker.registerChannelProvider(WellKnownDestination.Service.getId(), peers.m_serverSide, new ChannelSecurityPolicy()
        {
            @Override
            public boolean canJoin(@NotNull CookiePrincipal principal)
            {
                // Only allow Machine accounts to connect to the Peering channel.
                return principal.isInRole(WellKnownRole.Machine);
            }

            @Override
            public boolean canListMembers(@NotNull CookiePrincipal principal)
            {
                return true;
            }

            @Override
            public boolean canSend(@NotNull CookiePrincipal principal,
                                   MbData data)
            {
                return true;
            }

            @Override
            public boolean canSendBroadcast(@NotNull CookiePrincipal principal,
                                            MbData data)
            {
                return true;
            }
        });
    }

    public CompletableFuture<String> connectAsClient(JsonWebSocketDnsHints dnsHints,
                                                     String url,
                                                     String username,
                                                     String password) throws
                                                                      Exception
    {
        PeerInfo pi = new PeerInfo();

        await(pi.connectAsClient(dnsHints, url, username, password));

        synchronized (m_peers)
        {
            if (m_peers.putIfAbsent(pi.brokerIdentity, pi) != null)
            {
                pi.close();
            }

            refreshActivePeers();
        }

        return wrapAsync(pi.brokerIdentity);
    }

    public CompletableFuture<List<String>> listChannels(List<String> brokersPath)
    {
        if (brokersPath == null)
        {
            brokersPath = Collections.emptyList();
        }

        List<String> availableChannels = m_broker.listChannels();

        List<PeerInfo> targets = grabPeers();

        ListChannels repeatReq = new ListChannels();
        repeatReq.brokersPath = mergePaths(brokersPath, targets);

        for (PeerInfo pi : targets)
        {
            if (!brokersPath.contains(pi.brokerIdentity))
            {
                try
                {
                    contextualLogger.debugVerbose("Sending ListChannels request to peer %s", pi.brokerIdentity);

                    ListChannelsReply res = await(pi.sendMessageWithReply(repeatReq, ListChannelsReply.class, 10, TimeUnit.SECONDS));
                    if (res.availableChannels != null)
                    {
                        for (String channel : res.availableChannels)
                        {
                            CollectionUtils.addIfMissingAndNotNull(availableChannels, channel);
                        }
                    }
                }
                catch (Exception e)
                {
                    contextualLogger.error("ListChannels failed: %s", e);
                }
            }
        }

        return wrapAsync(availableChannels);
    }

    public CompletableFuture<List<String>> listMembers(List<String> brokersPath,
                                                       String channelName)
    {
        if (brokersPath == null)
        {
            brokersPath = Collections.emptyList();
        }

        List<String> members = Lists.newArrayList();

        MessageBusChannel ch = m_broker.getChannel(channelName);
        if (ch != null)
        {
            members.addAll(ch.getMemberIds());
        }

        List<PeerInfo> targets = grabPeers();

        ListMembers repeatReq = new ListMembers();
        repeatReq.channel = channelName;
        repeatReq.brokersPath = mergePaths(brokersPath, targets);

        for (PeerInfo pi : targets)
        {
            if (!brokersPath.contains(pi.brokerIdentity))
            {
                try
                {
                    contextualLogger.debugVerbose("Sending ListMembers request to peer %s", pi.brokerIdentity);

                    ListMembersReply res = await(pi.sendMessageWithReply(repeatReq, ListMembersReply.class, 10, TimeUnit.SECONDS));
                    if (res.members != null)
                    {
                        for (String member : res.members)
                        {
                            CollectionUtils.addIfMissingAndNotNull(members, member);
                        }
                    }
                }
                catch (Exception e)
                {
                    contextualLogger.error("ListMembers on peer %s failed: %s", pi.brokerIdentity, e);
                }
            }
        }

        return wrapAsync(members);
    }

    //--//

    private CompletableFuture<Void> receivedMessage(DataTransportWithReplies<Peering, PeeringReply> subscriber,
                                                    MbData_Message data,
                                                    Peering obj) throws
                                                                 Exception
    {
        contextualLogger.debug("receivedMessage: %s -> %s - %s", data.origin, data.destination, obj);

        ExchangeBrokerIdentity req_id = Reflection.as(obj, ExchangeBrokerIdentity.class);
        if (req_id != null)
        {
            synchronized (m_peers)
            {
                PeerInfo pi = m_peers.get(req_id.brokerIdentity);
                if (pi == null)
                {
                    pi = new PeerInfo();
                    pi.brokerIdentity = req_id.brokerIdentity;
                    m_peers.put(pi.brokerIdentity, pi);

                    refreshActivePeers();
                }

                pi.addServerEndpoint(data.origin);
            }

            ExchangeBrokerIdentityReply replyPayload = new ExchangeBrokerIdentityReply();
            replyPayload.brokerIdentity = m_broker.getBrokerId();

            return replyToMessage(subscriber, data, replyPayload, null);
        }

        ListChannels req_listCh = Reflection.as(obj, ListChannels.class);
        if (req_listCh != null)
        {
            ListChannelsReply replyPayload = new ListChannelsReply();
            replyPayload.availableChannels = await(PeeringProvider.this.listChannels(req_listCh.brokersPath));

            return replyToMessage(subscriber, data, replyPayload, null);
        }

        ListMembers req_listMembers = Reflection.as(obj, ListMembers.class);
        if (req_listMembers != null)
        {
            ListMembersReply replyPayload = new ListMembersReply();
            replyPayload.members = await(PeeringProvider.this.listMembers(req_listMembers.brokersPath, req_listMembers.channel));

            return replyToMessage(subscriber, data, replyPayload, null);
        }

        ForwardMessage req_fwd = Reflection.as(obj, ForwardMessage.class);
        if (req_fwd != null)
        {
            MbData            forwardedData = req_fwd.msg;
            MessageBusChannel ch            = m_broker.getChannel(forwardedData.channel);
            if (ch != null)
            {
                try
                {
                    contextualLogger.debug("Route message %s to channel %s", forwardedData.messageId, forwardedData.channel);

                    m_broker.routeMessage(null, forwardedData, null);
                }
                catch (Exception e)
                {
                    contextualLogger.error("Route message %s to channel %s failed: %s", forwardedData.messageId, forwardedData.channel, e);
                }
            }
        }

        return wrapAsync(null);
    }

    private CompletableFuture<Void> replyToMessage(DataTransportWithReplies<Peering, PeeringReply> transport,
                                                   MbData_Message data,
                                                   Peering payload,
                                                   MessageBusPayloadCallback notifyPayloadSize) throws
                                                                                                Exception
    {
        contextualLogger.debugVerbose("replying to %s with %s", data.destination, payload);

        MbData_Message_Reply replyData = MbData_Message_Reply.prepareForReply(data, payload);

        return transport.dispatchWithNoReply(replyData, notifyPayloadSize);
    }

    //--//

    void forwardMessage(MbData data)
    {
        if (CHANNEL_NAME.equals(data.channel))
        {
            // Peering channel messages are never forwarded.
            return;
        }

        contextualLogger.debug("ForwardMessage: %s", data.messageId);

        List<PeerInfo> targets = grabPeers();

        ForwardMessage req = new ForwardMessage();

        // Make a private copy for the Peers.
        req.msg = data.makeCopy();
        req.msg.brokersPath = mergePaths(data.brokersPath, targets);

        for (PeerInfo pi : targets)
        {
            if (data.alreadyVisited(pi.brokerIdentity))
            {
                contextualLogger.debug("ForwardMessage %s already sent to peer %s", data.messageId, pi.brokerIdentity);
            }
            else
            {
                try
                {
                    contextualLogger.debug("ForwardMessage %s to peer %s", data.messageId, pi.brokerIdentity);
                    pi.sendMessageWithNoReply(req);
                }
                catch (Exception e)
                {
                    contextualLogger.error("ForwardMessage %s failed: %s", data.messageId, e);
                }
            }
        }
    }

    //--//

    private List<PeerInfo> grabPeers()
    {
        return m_peersActive;
    }

    private void refreshActivePeers()
    {
        m_peersActive = Lists.newArrayList(m_peers.values());
    }

    private List<String> mergePaths(List<String> incomingPath,
                                    List<PeerInfo> targets)
    {
        List<String> outgoingPath = Lists.newArrayList();

        if (incomingPath != null)
        {
            outgoingPath.addAll(incomingPath);
        }

        //
        // Include ourselves in the path, to avoid bounce backs.
        //
        CollectionUtils.addIfMissingAndNotNull(outgoingPath, m_broker.getBrokerId());

        //
        // Update the path the request has taken with all the target brokers.
        // Required to avoid infinite forwarding loops.
        //
        for (PeerInfo pi : targets)
        {
            CollectionUtils.addIfMissingAndNotNull(outgoingPath, pi.brokerIdentity);
        }

        return outgoingPath;
    }
}
