/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.function.Consumer;

import javax.validation.constraints.NotNull;

import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;
import com.optio3.util.function.BiConsumerWithException;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.WriteCallback;

public abstract class JsonWebSocket<T> extends JsonConnection<T> implements WebSocketListener
{
    public static final Logger LoggerInstance = JsonConnection.LoggerInstance.createSubLogger(JsonWebSocket.class);

    private final CookiePrincipal m_principal;
    private final WriteCallback   m_writeCallback;

    private final Object m_sessionLock = new Object();

    private Session                                                          m_session;
    private BiConsumerWithException<T, Consumer<Integer>>                    m_sendMethod;
    private BiConsumerWithException<InetSocketAddress, ByteArrayInputStream> m_receiveMethod;

    protected JsonWebSocket()
    {
        m_principal = WebSocketWrapper.getPrincipal();

        m_writeCallback = new WriteCallback()
        {
            @Override
            public void writeFailed(Throwable x)
            {
                // If anything goes wrong, we want to close the connection.
                close();
            }

            @Override
            public void writeSuccess()
            {
            }
        };
    }

    public final @NotNull CookiePrincipal getPrincipal()
    {
        return m_principal;
    }

    //--//

    @Override
    public final void onWebSocketClose(int statusCode,
                                       String reason)
    {
        onConnectionClosing();

        try
        {
            LoggerInstance.debug("onWebSocketClose: %s - %s", statusCode, reason);

            onClose();
        }
        catch (Exception e)
        {
            LoggerInstance.error("onWebSocketClose received an unexpected exception: %s", e);
        }

        synchronized (m_sessionLock)
        {
            m_session = null;
        }

        onConnectionClosed();
    }

    @Override
    public final void onWebSocketConnect(Session session)
    {
        LoggerInstance.debug("onWebSocketConnect");

        synchronized (m_sessionLock)
        {
            m_session = session;
        }

        onConnectionOpened();

        try
        {
            onConnect();
        }
        catch (Throwable t)
        {
            onWebSocketError(t);

            close();
        }
    }

    @Override
    public final void onWebSocketError(Throwable cause)
    {
        onConnectionClosing();

        try
        {
            LoggerInstance.debug("onWebSocketError for exception: %s", cause);

            onError(cause);
        }
        catch (Exception e)
        {
            LoggerInstance.error("onWebSocketError received an unexpected exception: %s", e);
        }
    }

    @Override
    public final void onWebSocketBinary(byte[] payload,
                                        int offset,
                                        int len)
    {
        try
        {
            ByteArrayInputStream stream = new ByteArrayInputStream(payload, offset, len);

            m_receiveMethod.accept(null, stream);
        }
        catch (Throwable t)
        {
            onWebSocketError(t);

            close();
        }
    }

    private void receiveUnexpectedMessage(InetSocketAddress physicalConnection,
                                          ByteArrayInputStream stream)
    {
        throw new RuntimeException("Internal error: JsonWebSocket received a binary frame, unexpected");
    }

    @Override
    public final void onWebSocketText(String message)
    {
        try
        {
            LoggerInstance.debugVerbose("onWebSocketText: %s", message);

            receiveMessageText(null, message);
        }
        catch (Throwable t)
        {
            onWebSocketError(t);

            close();
        }
    }

    //--//

    @Override
    public void close()
    {
        onConnectionClosing();

        Session session;

        synchronized (m_sessionLock)
        {
            session = m_session;
            m_session = null;
        }

        if (session != null)
        {
            Executors.closeWithTimeout(session::close, 2 * 60 * 1000, (t) ->
            {
                LoggerInstance.error("Failed to close session...");
            });
        }

        onConnectionClosed();
    }

    @Override
    public void setTransmitCapabilities(EnumSet<JsonConnectionCapability> required)
    {
        if (required != null && required.contains(JsonConnectionCapability.CompressedStream))
        {
            m_sendMethod = this::sendMessageCompressed;
        }
        else if (required != null && required.contains(JsonConnectionCapability.BinaryStream))
        {
            m_sendMethod = this::sendMessageBinary;
        }
        else
        {
            m_sendMethod = this::sendMessageText;
        }
    }

    @Override
    public void setReceiveCapabilities(EnumSet<JsonConnectionCapability> required)
    {
        if (required != null && required.contains(JsonConnectionCapability.CompressedStream))
        {
            m_receiveMethod = this::receiveMessageCompressed;
        }
        else if (required != null && required.contains(JsonConnectionCapability.BinaryStream))
        {
            m_receiveMethod = this::receiveMessageBinary;
        }
        else
        {
            m_receiveMethod = this::receiveUnexpectedMessage;
        }
    }

    @Override
    public final void sendMessage(T msg,
                                  Consumer<Integer> notifyPayloadSize) throws
                                                                       Exception
    {
        try
        {
            m_sendMethod.accept(msg, notifyPayloadSize);
        }
        catch (Exception e)
        {
            if (e instanceof WebSocketException)
            {
                // This is expected.
            }
            else
            {
                LoggerInstance.error("SendMessage received an unexpected exception: %s", e);
            }

            close();

            throw e;
        }
    }

    private void sendMessageText(T msg,
                                 Consumer<Integer> notifyPayloadSize) throws
                                                                      Exception
    {
        String json = prepareMessageText(msg, notifyPayloadSize);

        LoggerInstance.debugVerbose("SendMessage: %s", json);

        synchronized (m_sessionLock)
        {
            if (m_session != null)
            {
                RemoteEndpoint remote = m_session.getRemote();
                if (remote != null)
                {
                    remote.sendString(json, m_writeCallback);
                }
            }
        }
    }

    private void sendMessageBinary(T msg,
                                   Consumer<Integer> notifyPayloadSize) throws
                                                                        Exception
    {
        ByteArrayOutputStream binary = prepareMessageBinary(msg, notifyPayloadSize);
        sendMessageAsBytes(binary);
    }

    private void sendMessageCompressed(T msg,
                                       Consumer<Integer> notifyPayloadSize) throws
                                                                            Exception
    {
        ByteArrayOutputStream compressed = prepareMessageCompressed(msg, notifyPayloadSize);
        sendMessageAsBytes(compressed);
    }

    private void sendMessageAsBytes(ByteArrayOutputStream compressed)
    {
        synchronized (m_sessionLock)
        {
            m_statistics.messageTx++;

            if (m_session != null)
            {
                RemoteEndpoint remote = m_session.getRemote();
                if (remote != null)
                {
                    remote.sendBytes(ByteBuffer.wrap(compressed.toByteArray()), m_writeCallback);
                }
            }
        }
    }
}
