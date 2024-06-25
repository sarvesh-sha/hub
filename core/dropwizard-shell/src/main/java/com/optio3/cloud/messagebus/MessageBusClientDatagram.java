/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.optio3.cloud.JsonConnectionCapability;
import com.optio3.cloud.JsonDatagram;
import com.optio3.cloud.messagebus.channel.RpcWorker;
import com.optio3.logging.Severity;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

/**
 * This class represents the client side of a MessageBus connection over WebSockets.
 */
public class MessageBusClientDatagram extends MessageBusClientBase
{
    class DatagramImpl extends JsonDatagram<MessageBusPayload>
    {
        private DatagramSocketWorker m_socket;
        private InetSocketAddress    m_target;
        private boolean              m_connected;

        DatagramImpl(SessionConfiguration sessionCfg)
        {
            super(sessionCfg);

            for (InetAddress host : sessionCfg.hosts)
            {
                m_target = new InetSocketAddress(host, sessionCfg.port);
                break;
            }
        }

        String describeConnection()
        {
            return String.format("%s over UDP", m_target.getHostName());
        }

        @Override
        public void close()
        {
            onClose();

            super.close();
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
            m_socket.close();

            releaseIdentity();

            s_statisticsTotal.add(m_statistics);
            m_statistics = new MessageBusStatistics();

            dumpStatistics(LoggerInstanceForStatistics, Severity.Info, s_statisticsTotal);
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

        //--//

        @Override
        protected InetSocketAddress getPhysicalConnection()
        {
            return null;
        }

        @Override
        protected void receivedKeepAlive()
        {
            if (!m_connected)
            {
                m_connected = true;

                onConnectionOpened();

                try
                {
                    onConnect();
                }
                catch (Throwable t)
                {
                    close();
                }
            }
        }

        @Override
        protected void sendFrame(OutputBuffer ob)
        {
            recordTxPacket(ob);
            m_socket.sendPacket(m_target, ob.toByteArray());
        }

        void connectToServer()
        {
            m_socket = new DatagramSocketWorker(0)
            {
                @Override
                protected void processPacket(InetSocketAddress source,
                                             InputBuffer ib)
                {
                    try
                    {
                        recordRxPacket(ib);

                        PublicHeader publicHeader = decodePublicHeader(ib);
                        if (publicHeader != null)
                        {
                            if (!matchPublicHeader(publicHeader))
                            {
                                LoggerInstance.info("Detected stale session");
                                markAsStale();
                                closeConnection();
                            }
                            else
                            {
                                SharedHeader sharedHeader = decodeSharedHeader(ib);
                                if (sharedHeader != null)
                                {
                                    processIncomingFrame(ib, sharedHeader);

                                    markActivity();
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

            // Initiate exchange with server.
            activateKeepAlive();
        }
    }

    //--//

    private final DatagramImpl m_datagram;

    //--//

    public MessageBusClientDatagram(JsonDatagram.SessionConfiguration sessionCfg)
    {
        super(sessionCfg.endpointId);

        m_datagram = new DatagramImpl(sessionCfg);
    }

    //--//

    @Override
    public String describeConnection()
    {
        return m_datagram.describeConnection();
    }

    @Override
    public MessageBusStatistics sampleStatistics()
    {
        return m_datagram.sampleStatistics();
    }

    @Override
    public boolean isConnectionOpen()
    {
        return true;
    }

    @Override
    public void startConnection() throws
                                  Exception
    {
        m_datagram.connectToServer();
    }

    @Override
    public CompletableFuture<Void> onDisconnected() throws
                                                    Exception
    {
        return m_datagram.onDisconnected();
    }

    @Override
    public void closeConnection()
    {
        m_datagram.close();
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
    protected void keepAlive()
    {
        // UDP doesn't need keep alive at the MessageBus level.
    }

    //--//

    @Override
    protected CompletableFuture<Void> postProcessIdentity(EnumSet<JsonConnectionCapability> common) throws
                                                                                                    Exception
    {
        // Nothing to do.
        return wrapAsync(null);
    }

    @Override
    protected EnumSet<JsonConnectionCapability> getLocalCapabilities()
    {
        return m_datagram.getLocalCapabilities();
    }

    @Override
    protected void setTransmitCapabilities(EnumSet<JsonConnectionCapability> required)
    {
        m_datagram.setTransmitCapabilities(required);
    }

    @Override
    protected void setReceiveCapabilities(EnumSet<JsonConnectionCapability> required)
    {
        m_datagram.setReceiveCapabilities(required);
    }

    @Override
    protected void sendMessage(MessageBusPayload msg,
                               Consumer<Integer> notifyPayloadSize) throws
                                                                    Exception
    {
        m_datagram.sendMessage(msg, notifyPayloadSize);
    }
}
