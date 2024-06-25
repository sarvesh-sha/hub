/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.TextNode;

public final class ObjectMappers
{
    public static final ObjectMapper SkipNulls;
    public static final ObjectMapper SkipNullsAllowEmptyBeans;
    public static final ObjectMapper SkipNullsCaseInsensitive;
    public static final ObjectMapper SkipDefaults;
    public static       ObjectMapper RestDefaults;

    static
    {
        {
            ObjectMapper objectMapper = new ObjectMapper();

            objectMapper.findAndRegisterModules();

            configureToSkipNulls(objectMapper);

            SkipNulls = objectMapper;
        }

        {
            ObjectMapper objectMapper = SkipNulls.copy();
            ObjectMappers.configureCaseInsensitive(objectMapper);
            SkipNullsCaseInsensitive = objectMapper;
        }

        {
            ObjectMapper objectMapper = SkipNulls.copy();
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            SkipNullsAllowEmptyBeans = objectMapper;
        }

        {
            ObjectMapper objectMapper = SkipNulls.copy();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
            SkipDefaults = objectMapper;
        }

        RestDefaults = SkipNulls;
    }

    //--//

    public static void configureToIgnoreMissingProperties(ObjectMapper mapper)
    {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static void configureToSkipNulls(ObjectMapper mapper)
    {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static void configureCaseInsensitive(ObjectMapper mapper)
    {
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
    }

    public static void configureEnumAsStrings(ObjectMapper mapper,
                                              boolean ignoreUnknowns)
    {
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        mapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
        mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, ignoreUnknowns);
    }

    //--//

    public static byte[] serializeToGzip(Object obj)
    {
        return serializeToGzip(SkipNulls, obj);
    }

    public static byte[] serializeToGzip(ObjectMapper om,
                                         Object obj)
    {
        ByteArrayOutputStream output     = new ByteArrayOutputStream();
        DeflaterOutputStream  compressor = new DeflaterOutputStream(output);

        try
        {
            om.writeValue(compressor, obj);
            compressor.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return output.toByteArray();
    }

    public static JsonNode deserializeFromGzipAsJsonTree(byte[] buffer)
    {
        return deserializeFromGzipAsJsonTree(SkipNulls, buffer);
    }

    public static JsonNode deserializeFromGzipAsJsonTree(ObjectMapper om,
                                                         byte[] buffer)
    {
        if (buffer == null)
        {
            return null;
        }

        ByteArrayInputStream input        = new ByteArrayInputStream(buffer);
        InflaterInputStream  decompressor = new InflaterInputStream(input);

        try
        {
            return om.readTree(decompressor);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static <T> T deserializeFromGzip(byte[] buffer,
                                            Class<T> clz)
    {
        return deserializeFromGzip(SkipNulls, buffer, clz);
    }

    public static <T> T deserializeFromGzip(ObjectMapper om,
                                            byte[] buffer,
                                            Class<T> clz)
    {
        ByteArrayInputStream input        = new ByteArrayInputStream(buffer);
        InflaterInputStream  decompressor = new InflaterInputStream(input);

        try
        {
            return om.readValue(decompressor, clz);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static <T> T deserializeFromGzip(byte[] buffer,
                                            TypeReference<T> typeRef)
    {
        return deserializeFromGzip(SkipNulls, buffer, typeRef);
    }

    public static <T> T deserializeFromGzip(ObjectMapper om,
                                            byte[] buffer,
                                            TypeReference<T> typeRef)
    {
        ByteArrayInputStream input        = new ByteArrayInputStream(buffer);
        InflaterInputStream  decompressor = new InflaterInputStream(input);

        try
        {
            return om.readValue(decompressor, typeRef);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static <T> T cloneThroughJson(ObjectMapper om,
                                         T obj)
    {
        try
        {
            if (obj == null)
            {
                return null;
            }

            if (om == null)
            {
                om = SkipNulls;
            }

            @SuppressWarnings("unchecked") Class<T> clz = (Class<T>) obj.getClass();

            return om.convertValue(obj, clz);
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    //--//

    public static String toJsonNoThrow(ObjectMapper om,
                                       Object obj)
    {
        try
        {
            if (om == null)
            {
                om = SkipNulls;
            }

            return om.writeValueAsString(obj);
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    //--//

    public static String getFieldAsText(JsonNode node,
                                        String prop)
    {
        node = node.get(prop);
        return node != null ? node.asText() : null;
    }

    public static String getTextField(JsonNode node,
                                      String prop)
    {
        node = node.get(prop);
        return node instanceof TextNode ? node.textValue() : null;
    }

    public static boolean getBooleanField(JsonNode node,
                                          String prop)
    {
        node = node.get(prop);
        return node instanceof BooleanNode && node.booleanValue();
    }

    public static int getIntegerField(JsonNode node,
                                      String prop)
    {
        node = node.get(prop);
        return node instanceof NumericNode ? node.asInt() : 0;
    }

    public static long getLongField(JsonNode node,
                                    String prop)
    {
        node = node.get(prop);
        return node instanceof NumericNode ? node.asLong() : 0;
    }

    public static double getDoubleField(JsonNode node,
                                        String prop)
    {
        node = node.get(prop);
        return node instanceof NumericNode ? node.asDouble() : 0;
    }

    //--//

    public static String prettyPrintAsJson(Object obj)
    {
        return prettyPrintAsJson(SkipNullsAllowEmptyBeans, obj);
    }

    public static String prettyPrintAsJson(ObjectMapper objectMapper,
                                           Object obj)
    {
        try
        {
            if (obj == null)
            {
                return "null";
            }

            ObjectWriter writer = objectMapper.writer(new DefaultPrettyPrinter().withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE));
            return writer.writeValueAsString(obj);
        }
        catch (JsonProcessingException e)
        {
            return e.getMessage();
        }
    }

    //--//

    public static int estimateSize(JsonNode node)
    {
        int size = 0;

        if (node != null)
        {
            try
            {
                JsonParser jp = node.traverse();

                while (true)
                {
                    JsonToken token = jp.nextToken();
                    if (token == null)
                    {
                        break;
                    }

                    switch (token)
                    {
                        case NOT_AVAILABLE:
                            break;

                        case START_OBJECT:
                        case END_OBJECT:
                        case START_ARRAY:
                        case END_ARRAY:
                            size += 1;
                            break;

                        case FIELD_NAME:
                            size += jp.getText()
                                      .length() + 4;
                            break;

                        case VALUE_EMBEDDED_OBJECT:
                        case VALUE_STRING:
                        case VALUE_NUMBER_INT:
                        case VALUE_NUMBER_FLOAT:
                        case VALUE_TRUE:
                        case VALUE_FALSE:
                        case VALUE_NULL:
                            size += jp.getText()
                                      .length() + 2;
                            break;
                    }
                }
            }
            catch (IOException e)
            {
                // Ignore failures.
            }
        }

        return size;
    }
}
