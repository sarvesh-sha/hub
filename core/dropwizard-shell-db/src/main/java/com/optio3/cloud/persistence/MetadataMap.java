/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.optio3.cloud.BinaryObjectMappers;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.BoxingUtils;
import com.optio3.util.Exceptions;

public class MetadataMap
{
    private static final TypeReference<TreeMap<String, byte[]>> c_typeRef = new TypeReference<>()
    {
    };

    private static final TreeMap<String, byte[]> s_empty = new TreeMap<>();
    private static final ObjectMapper            s_mapper;

    static
    {
        s_mapper = BinaryObjectMappers.SkipNulls.copy();
        s_mapper.addHandler(new DeserializationProblemHandler()
        {
            @Override
            public Object handleWeirdNumberValue(DeserializationContext ctxt,
                                                 Class<?> targetType,
                                                 Number valueToConvert,
                                                 String failureMsg) throws
                                                                    IOException
            {
                if (targetType == Boolean.class)
                {
                    return valueToConvert.intValue() != 0;
                }

                return super.handleWeirdNumberValue(ctxt, targetType, valueToConvert, failureMsg);
            }

            @Override
            public Object handleWeirdStringValue(DeserializationContext ctxt,
                                                 Class<?> targetType,
                                                 String valueToConvert,
                                                 String failureMsg) throws
                                                                    IOException
            {
                if ("NULL".equals(valueToConvert))
                {
                    return null;
                }

                if (targetType == Boolean.class)
                {
                    if ("TRUE".equals(valueToConvert))
                    {
                        return Boolean.TRUE;
                    }

                    if ("FALSE".equals(valueToConvert))
                    {
                        return Boolean.FALSE;
                    }
                }

                if (targetType.isEnum() && valueToConvert.startsWith("\"") && valueToConvert.endsWith("\""))
                {
                    return resolveEnum(valueToConvert.substring(1, valueToConvert.length() - 1), targetType);
                }

                return super.handleWeirdStringValue(ctxt, targetType, valueToConvert, failureMsg);
            }

            private <T extends Enum<T>> T resolveEnum(String enumValue,
                                                      Class<?> clz)
            {
                @SuppressWarnings("unchecked") Class<T> clzEnum = (Class<T>) clz;

                return Enum.valueOf(clzEnum, enumValue);
            }
        });
    }

    //--//

    private byte[]                  m_stateCompressed;
    private TreeMap<String, byte[]> m_state;
    private boolean                 m_shared;

    private MetadataMap(byte[] stateCompressed,
                        TreeMap<String, byte[]> state)
    {
        m_stateCompressed = stateCompressed;
        m_state           = state;
        m_shared          = true;
    }

    public static MetadataMap decodeMetadata(byte[] metadataCompressed)
    {
        if (metadataCompressed == null || metadataCompressed.length < 4)
        {
            return empty();
        }

        try
        {
            if (metadataCompressed[0] == 'O' && metadataCompressed[1] == 'P' && metadataCompressed[2] == 'v')
            {
                byte version = metadataCompressed[3];

                switch (version)
                {
                    case 1:
                        var state = s_mapper.readValue(metadataCompressed, 4, metadataCompressed.length - 4, c_typeRef);
                        return new MetadataMap(metadataCompressed, state);

                    default:
                        throw Exceptions.newIllegalArgumentException("Unvalid metadata version %d", version);
                }
            }
            else
            {
                TypeReference<TreeMap<String, Object>> typeRefLegacy = new TypeReference<>()
                {
                };

                TreeMap<String, Object> stateLegacy = ObjectMappers.deserializeFromGzip(ObjectMappers.SkipNulls, metadataCompressed, typeRefLegacy);
                MetadataMap             map         = empty();

                for (Map.Entry<String, Object> entry : stateLegacy.entrySet())
                {
                    String key   = entry.getKey();
                    Object value = entry.getValue();

                    if (value instanceof String)
                    {
                        String txt = (String) value;

                        if ((txt.startsWith("{") && txt.endsWith("}")) || (txt.startsWith("[") && txt.endsWith("]")))
                        {
                            JsonNode node = ObjectMappers.SkipNulls.readTree(txt);

                            map.putObject(key, node);
                            continue;
                        }
                    }

                    map.putObject(key, value);
                }

                return map;
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static byte[] encodeMetadata(MetadataMap map)
    {
        if (map == null || map.m_state.isEmpty())
        {
            return null;
        }

        if (!map.m_shared || map.m_stateCompressed == null)
        {
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            try
            {
                output.write('O');
                output.write('P');
                output.write('v');
                output.write(1);

                s_mapper.writeValue(output, map.m_state);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }

            map.m_stateCompressed = output.toByteArray();
            map.m_shared          = true;
        }

        return map.m_stateCompressed;
    }

    public static MetadataMap empty()
    {
        return new MetadataMap(null, s_empty);
    }

    public MetadataMap copy()
    {
        return m_shared ? new MetadataMap(m_stateCompressed, m_state) : new MetadataMap(null, m_state != null ? new TreeMap<>(m_state) : s_empty);
    }

    //--//

    private void makePrivate()
    {
        if (m_shared)
        {
            m_state           = new TreeMap<>(m_state);
            m_stateCompressed = null;
            m_shared          = false;
        }
    }

    //--//

    public int size()
    {
        return m_state.size();
    }

    public Set<String> keySet()
    {
        // The set can be used to remove items.
        makePrivate();

        return m_state.keySet();
    }

    public void remove(String key)
    {
        if (m_state.containsKey(key))
        {
            makePrivate();

            m_state.remove(key);
        }
    }

    public boolean contains(String key)
    {
        return m_state.containsKey(key);
    }

    public void toStream(String key,
                         ByteArrayOutputStream stream) throws
                                                       IOException
    {
        byte[] raw = m_state.get(key);
        if (raw != null)
        {
            stream.write(raw);
        }
    }

    //--//

    public void putString(String key,
                          String value)
    {
        putObject(key, value);
    }

    public boolean putStringOrRemoveIfNull(String key,
                                           String value)
    {
        return putOrRemoveIfNull(key, value, this::putString);
    }

    public String getString(String key)
    {
        return getObject(key, String.class);
    }

    //--//

    public void putDateTime(String key,
                            ZonedDateTime value)
    {
        putObject(key, value);
    }

    public boolean putDateTimeOrRemoveIfNull(String key,
                                             ZonedDateTime value)
    {
        return putOrRemoveIfNull(key, value, this::putDateTime);
    }

    public ZonedDateTime getDateTime(String key)
    {
        return getObject(key, ZonedDateTime.class);
    }

    //--//

    public void putBoolean(String key,
                           boolean value)
    {
        putObject(key, value);
    }

    public boolean putBooleanOrRemoveIfNull(String key,
                                            Boolean value)
    {
        return putOrRemoveIfNull(key, value, this::putBoolean);
    }

    public Boolean getBoolean(String key)
    {
        return getObject(key, Boolean.class);
    }

    public boolean getBooleanOrDefault(String key,
                                       boolean defaultValue)
    {
        return BoxingUtils.get(getBoolean(key), defaultValue);
    }

    //--//

    public void putInt(String key,
                       int value)
    {
        putObject(key, value);
    }

    public boolean putIntOrRemoveIfNull(String key,
                                        Integer value)
    {
        return putOrRemoveIfNull(key, value, this::putInt);
    }

    public Integer getInt(String key)
    {
        return getObject(key, Integer.class);
    }

    public int getIntOrDefault(String key,
                               int defaultValue)
    {
        return BoxingUtils.get(getInt(key), defaultValue);
    }

    //--//

    public void putLong(String key,
                        long value)
    {
        putObject(key, value);
    }

    public boolean putLongOrRemoveIfNull(String key,
                                         Long value)
    {
        return putOrRemoveIfNull(key, value, this::putLong);
    }

    public Long getLong(String key)
    {
        return getObject(key, Long.class);
    }

    public long getLongOrDefault(String key,
                                 long defaultValue)
    {
        return BoxingUtils.get(getLong(key), defaultValue);
    }

    //--//

    public void putDouble(String key,
                          double value)
    {
        putObject(key, value);
    }

    public boolean putDoubleOrRemoveIfNull(String key,
                                           Double value)
    {
        return putOrRemoveIfNull(key, value, this::putDouble);
    }

    public Double getDouble(String key)
    {
        return getObject(key, Double.class);
    }

    public double getDoubleOrDefault(String key,
                                     double defaultValue)
    {
        return BoxingUtils.get(getDouble(key), defaultValue);
    }

    //--//

    public void putObject(String key,
                          Object obj)
    {
        byte[] value    = obj != null ? ObjectMappers.serializeToGzip(s_mapper, obj) : new byte[0];
        byte[] oldValue = m_state.get(key);

        if (Arrays.equals(oldValue, value))
        {
            // Identical values, don't update.
            return;
        }

        makePrivate();

        m_state.put(key, value);
    }

    public boolean putObjectOrRemoveIfNull(String key,
                                           Object value)
    {
        return putOrRemoveIfNull(key, value, this::putObject);
    }

    public <T> T getObject(String key,
                           Class<T> clz)
    {
        byte[] raw = m_state.get(key);
        return raw != null && raw.length > 0 ? ObjectMappers.deserializeFromGzip(s_mapper, raw, clz) : null;
    }

    public <T> T getObject(String key,
                           TypeReference<T> typeRef)
    {
        byte[] raw = m_state.get(key);
        return raw != null ? ObjectMappers.deserializeFromGzip(s_mapper, raw, typeRef) : null;
    }

    //--//

    public void putTags(String key,
                        MetadataTagsMap tags)
    {
        if (tags == null || tags.isEmpty())
        {
            remove(key);
        }
        else
        {
            putObject(key, tags);
        }
    }

    public MetadataTagsMap getTags(String key)
    {
        MetadataTagsMap tags = getObject(key, MetadataTagsMap.class);
        return tags != null ? tags : new MetadataTagsMap();
    }

    public <T> T modifyTags(String key,
                            Function<MetadataTagsMap, T> callback)
    {
        MetadataTagsMap tags = getTags(key);

        T res = callback.apply(tags);

        putTags(key, tags);

        return res;
    }

    public void modifyTags(String key,
                           Consumer<MetadataTagsMap> callback)
    {
        MetadataTagsMap tags = getTags(key);

        callback.accept(tags);

        putTags(key, tags);
    }

    //--//

    private <T> boolean putOrRemoveIfNull(String key,
                                          T value,
                                          BiConsumer<String, T> setter)
    {
        if (value == null)
        {
            remove(key);
            return false;
        }
        else
        {
            setter.accept(key, value);
            return true;
        }
    }
}
