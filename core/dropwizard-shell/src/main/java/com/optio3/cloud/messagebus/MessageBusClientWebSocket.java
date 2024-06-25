/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.optio3.cloud.JsonConnectionCapability;
import com.optio3.cloud.JsonDatagram;
import com.optio3.cloud.JsonWebSocketClient;
import com.optio3.cloud.JsonWebSocketDnsHints;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.messagebus.channel.RpcWorker;
import com.optio3.cloud.messagebus.payload.MbControl_UpgradeToUDP;
import com.optio3.cloud.messagebus.payload.MbControl_UpgradeToUDP_Reply;

/**
 * This class represents the client side of a MessageBus connection over WebSockets.
 */
public abstract class MessageBusClientWebSocket extends MessageBusClientBase
{
    public static class NoUDP extends MessageBusClientWebSocket
    {
        public NoUDP(JsonWebSocketDnsHints dnsHints,
                     String connectionUrl,
                     String userName,
                     String password)
        {
            super(dnsHints, connectionUrl, userName, password);
        }

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
            return null;
        }

        @Override
        protected void completeUpgradeResponse(MbControl_UpgradeToUDP_Reply res,
                                               List<InetAddress> addresses)
        {
            // Nothing to do.
        }
    }

    class WebSocketImpl extends JsonWebSocketClient<MessageBusPayload>
    {
        WebSocketImpl(JsonWebSocketDnsHints dnsHints,
                      String connectionUrl,
                      String userName,
                      String password)
        {
            super(dnsHints, connectionUrl, userName, password);
        }

        WebSocketImpl(JsonWebSocketDnsHints dnsHints,
                      String connectionUrl,
                      String token)
        {
            super(dnsHints, connectionUrl, token);
        }

        @Override
        protected void onConnect()
        {
            m_transmitTransport = new OutboundTransport();

            retrieveIdentity();
        }

        @Override
        protected void onClose()
        {
            releaseIdentity();
        }

        @Override
        protected void onError(Throwable cause)
        {
            failIdentity(cause);
        }

        @Override
        protected void onMessage(InetSocketAddress physicalConnection,
                                 MessageBusPayload req,
                                 int size)
        {
            req.physicalConnection = physicalConnection;
            req.messageSize        = size;

            routeAsync(req);
        }
    }

    //--//

    private final WebSocketImpl     m_webSocket;
    private       List<InetAddress> m_addresses;

    //--//

    public MessageBusClientWebSocket(JsonWebSocketDnsHints dnsHints,
                                     String connectionUrl,
                                     WellKnownRole role,
                                     String hostId)
    {
        super(null);

        String authVersion = "v1";

        m_webSocket = new WebSocketImpl(dnsHints, connectionUrl, role.generateAuthPrincipal(authVersion, hostId), role.generateAuthCode(authVersion, hostId));
    }

    public MessageBusClientWebSocket(JsonWebSocketDnsHints dnsHints,
                                     String connectionUrl,
                                     String userName,
                                     String password)
    {
        super(null);

        m_webSocket = new WebSocketImpl(dnsHints, connectionUrl, userName, password);
    }

    public MessageBusClientWebSocket(JsonWebSocketDnsHints dnsHints,
                                     String connectionUrl,
                                     String token)
    {
        super(null);

        m_webSocket = new WebSocketImpl(dnsHints, connectionUrl, token);
    }

    //--//

    @Override
    public String describeConnection()
    {
        return m_webSocket.describeConnection();
    }

    @Override
    public MessageBusStatistics sampleStatistics()
    {
        return m_webSocket.sampleStatistics();
    }

    @Override
    public boolean isConnectionOpen()
    {
        return m_webSocket.isOpen();
    }

    @Override
    public void startConnection() throws
                                  Exception
    {
        m_addresses = m_webSocket.connectToServer();
    }

    @Override
    public CompletableFuture<Void> onDisconnected() throws
                                                    Exception
    {
        return m_webSocket.onDisconnected();
    }

    @Override
    public void closeConnection()
    {
        m_webSocket.close();
    }

    //--//

    @Override
    protected CompletableFuture<Void> postProcessIdentity(EnumSet<JsonConnectionCapability> common) throws
                                                                                                    Exception
    {
        if (common.contains(JsonConnectionCapability.UDPv1))
        {
            try
            {
                MbControl_UpgradeToUDP req = new MbControl_UpgradeToUDP();
                req.version = JsonDatagram.PROTOCOL_V1;

                req = completeUpgradeRequest(req);
                if (req != null)
                {
                    MbControl_UpgradeToUDP_Reply reply = await(sendSystemMessage(req, MbControl_UpgradeToUDP_Reply.class, null));
                    if (reply.endpointId != null)
                    {
                        completeUpgradeResponse(reply, m_addresses);
                    }
                }
            }
            catch (Throwable t)
            {
                // Ignore failures during upgrade.
            }
        }

        return wrapAsync(null);
    }

    protected abstract MbControl_UpgradeToUDP completeUpgradeRequest(MbControl_UpgradeToUDP req);

    protected abstract void completeUpgradeResponse(MbControl_UpgradeToUDP_Reply res,
                                                    List<InetAddress> addresses);

    @Override
    protected EnumSet<JsonConnectionCapability> getLocalCapabilities()
    {
        return m_webSocket.getLocalCapabilities();
    }

    @Override
    protected void setTransmitCapabilities(EnumSet<JsonConnectionCapability> required)
    {
        m_webSocket.setTransmitCapabilities(required);
    }

    @Override
    protected void setReceiveCapabilities(EnumSet<JsonConnectionCapability> required)
    {
        m_webSocket.setReceiveCapabilities(required);
    }

    @Override
    protected void sendMessage(MessageBusPayload msg,
                               Consumer<Integer> notifyPayloadSize) throws
                                                                    Exception
    {
        m_webSocket.sendMessage(msg, notifyPayloadSize);
    }
}
