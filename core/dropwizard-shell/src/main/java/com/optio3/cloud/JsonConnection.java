/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.optio3.cloud.messagebus.MessageBusStatistics;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import io.dropwizard.jackson.Jackson;

public abstract class JsonConnection<T> implements AutoCloseable
{
    private static final ObjectMapper s_objectMapper;

    static
    {
        final ObjectMapper mapper = Jackson.newObjectMapper();
        ObjectMappers.configureEnumAsStrings(mapper, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL); // Do not use NON_DEFAULT, or empty collections will be converted to null!!!!
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mapper.findAndRegisterModules();

        s_objectMapper = mapper;
    }

    //--//

    public static ObjectMapper getObjectMapper()
    {
        return s_objectMapper;
    }

    public static final Logger LoggerInstance = new Logger(JsonConnection.class, true);

    protected static final MessageBusStatistics s_statisticsTotal = new MessageBusStatistics();
    protected              MessageBusStatistics m_statistics      = new MessageBusStatistics();
    protected              MessageBusStatistics m_statisticsPreviousSample;

    protected final Class<T> m_messageClass;

    protected final EnumSet<JsonConnectionCapability> m_ourCapabilities;
    private final   CompletableFuture<Void>           m_connected;
    private final   CompletableFuture<Void>           m_disconnected;

    protected JsonConnection()
    {
        m_messageClass = Reflection.searchTypeArgument(JsonConnection.class, this, 0);
        if (m_messageClass == null)
        {
            // sanity check, should never happen
            throw Exceptions.newIllegalArgumentException("Incorrect usage: JsonConnection %s constructed without actual type information", getClass().getName());
        }

        EnumSet<JsonConnectionCapability> cap = EnumSet.noneOf(JsonConnectionCapability.class);

        cap.add(JsonConnectionCapability.BinaryStream);
        cap.add(JsonConnectionCapability.CompressedStream);
        cap.add(JsonConnectionCapability.UDPv1);

        m_ourCapabilities = cap;

        setReceiveCapabilities(null);
        setTransmitCapabilities(null);

        m_connected    = new CompletableFuture<Void>();
        m_disconnected = new CompletableFuture<Void>();
    }

    //--//

    public final CompletableFuture<Void> onConnected()
    {
        return m_connected;
    }

    public final CompletableFuture<Void> onDisconnected()
    {
        return m_disconnected;
    }

    //--//

    protected abstract void onConnect() throws
                                        Exception;

    protected abstract void onClose() throws
                                      Exception;

    protected abstract void onError(Throwable cause) throws
                                                     Exception;

    protected abstract void onMessage(InetSocketAddress physicalConnection,
                                      T msg,
                                      int size) throws
                                                Exception;

    protected void onConnectionClosing()
    {
        m_disconnected.complete(null);
    }

    protected void onConnectionClosed()
    {
        if (!m_connected.isDone())
        {
            m_connected.completeExceptionally(new TimeoutException("Closed"));
        }
    }

    protected void onConnectionOpened()
    {
        m_connected.complete(null);
    }

    //--//

    public MessageBusStatistics sampleStatistics()
    {
        var current = m_statistics.copy();

        var delta = new MessageBusStatistics();
        delta.add(current);
        delta.subtract(m_statisticsPreviousSample);

        m_statisticsPreviousSample = current;

        return delta;
    }

    //--//

    public EnumSet<JsonConnectionCapability> getLocalCapabilities()
    {
        return m_ourCapabilities;
    }

    public abstract void setTransmitCapabilities(EnumSet<JsonConnectionCapability> required);

    public abstract void setReceiveCapabilities(EnumSet<JsonConnectionCapability> required);

    public abstract void sendMessage(T msg,
                                     Consumer<Integer> notifyPayloadSize) throws
                                                                          Exception;

    //--//

    public final void receiveMessageText(InetSocketAddress physicalConnection,
                                         String message) throws
                                                         Exception
    {
        T msg = deserializeValue(m_messageClass, message);

        onMessageWithLogging(physicalConnection, msg, message.length());
    }

    public final void receiveMessageBinary(InetSocketAddress physicalConnection,
                                           ByteArrayInputStream stream) throws
                                                                        Exception
    {
        int len = stream.available();

        T msg = s_objectMapper.readValue(stream, m_messageClass);

        onMessageWithLogging(physicalConnection, msg, len);
    }

    public final void receiveMessageCompressed(InetSocketAddress physicalConnection,
                                               ByteArrayInputStream stream) throws
                                                                            Exception
    {
        int len = stream.available();

        InflaterInputStream decompressor = new InflaterInputStream(stream);

        T msg = s_objectMapper.readValue(decompressor, m_messageClass);

        onMessageWithLogging(physicalConnection, msg, len);
    }

    private void onMessageWithLogging(InetSocketAddress physicalConnection,
                                      T msg,
                                      int size) throws
                                                Exception
    {
        if (LoggerInstance.isEnabled(Severity.DebugVerbose))
        {
            LoggerInstance.debugVerbose("receivedMessage (%d bytes): %s", size, ObjectMappers.prettyPrintAsJson(msg));
        }

        m_statistics.messageRx++;

        onMessage(physicalConnection, msg, size);
    }

    //--//

    public final String prepareMessageText(T msg,
                                           Consumer<Integer> notifyPayloadSize) throws
                                                                                Exception
    {
        String json = serializeValue(msg);

        if (notifyPayloadSize != null)
        {
            notifyPayloadSize.accept(json.length());
        }

        return json;
    }

    public final ByteArrayOutputStream prepareMessageBinary(T msg,
                                                            Consumer<Integer> notifyPayloadSize) throws
                                                                                                 Exception
    {
        ByteArrayOutputStream binary = new ByteArrayOutputStream();

        getObjectMapper().writeValue(binary, msg);

        if (notifyPayloadSize != null)
        {
            notifyPayloadSize.accept(binary.size());
        }

        return binary;
    }

    public final ByteArrayOutputStream prepareMessageCompressed(T msg,
                                                                Consumer<Integer> notifyPayloadSize) throws
                                                                                                     Exception
    {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        DeflaterOutputStream  compressor = new DeflaterOutputStream(compressed);

        s_objectMapper.writeValue(compressor, msg);

        if (notifyPayloadSize != null)
        {
            notifyPayloadSize.accept(compressed.size());
        }

        return compressed;
    }

    //--//

    public static <V> V deserializeValue(Class<V> valClass,
                                         String val) throws
                                                     JsonParseException,
                                                     JsonMappingException,
                                                     IOException
    {
        return s_objectMapper.readValue(val, valClass);
    }

    public static <V> V deserializeValue(JavaType valClass,
                                         String val) throws
                                                     JsonParseException,
                                                     JsonMappingException,
                                                     IOException
    {
        return s_objectMapper.readValue(val, valClass);
    }

    public static <V> V deserializeValue(Class<V> valClass,
                                         JsonNode tree) throws
                                                        JsonParseException,
                                                        JsonMappingException,
                                                        IOException
    {
        return s_objectMapper.readValue(tree.traverse(), valClass);
    }

    public static <V> V deserializeValue(JavaType valClass,
                                         JsonNode tree) throws
                                                        JsonParseException,
                                                        JsonMappingException,
                                                        IOException
    {
        return s_objectMapper.readValue(tree.traverse(), valClass);
    }

    public static String serializeValue(Object val) throws
                                                    JsonProcessingException
    {
        return s_objectMapper.writeValueAsString(val);
    }

    public static JsonNode serializeValueAsTree(Object val)
    {
        return s_objectMapper.valueToTree(val);
    }
}
