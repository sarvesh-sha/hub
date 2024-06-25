/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.optio3.cloud.AbstractApplication;
import com.optio3.util.Exceptions;
import com.optio3.util.function.FunctionWithException;
import org.apache.commons.lang3.StringUtils;

public final class PersistAsJsonHelper<F, E>
{
    private final Class<F>    m_persistenceType;
    private final Callable<F> m_getter;
    private final Consumer<F> m_setter;

    private final FunctionWithException<F, E> m_deserializer;
    private final FunctionWithException<E, F> m_serializer;

    private final boolean m_ignoreGetFailures;

    private boolean m_valueValid;
    private E       m_value;
    private boolean m_valueChanged;

    public PersistAsJsonHelper(Callable<F> getter,
                               Consumer<F> setter,
                               Class<F> persistenceType,
                               Class<E> clz,
                               ObjectMapper mapper)
    {
        this(getter, setter, persistenceType, clz, mapper, false);
    }

    public PersistAsJsonHelper(Callable<F> getter,
                               Consumer<F> setter,
                               Class<F> persistenceType,
                               Class<E> clz,
                               ObjectMapper mapper,
                               boolean ignoreGetFailures)
    {
        this(getter, setter, (val) -> deserializeHelper(mapper, clz, persistenceType, val), (val) -> serializeHelper(mapper, clz, persistenceType, val), persistenceType, ignoreGetFailures);
    }

    public PersistAsJsonHelper(Callable<F> getter,
                               Consumer<F> setter,
                               Class<F> persistenceType,
                               TypeReference<E> typeRef,
                               ObjectMapper mapper)
    {
        this(getter, setter, persistenceType, typeRef, mapper, false);
    }

    public PersistAsJsonHelper(Callable<F> getter,
                               Consumer<F> setter,
                               Class<F> persistenceType,
                               TypeReference<E> typeRef,
                               ObjectMapper mapper,
                               boolean ignoreGetFailures)
    {
        this(getter, setter, (val) -> deserializeHelper(mapper, typeRef, persistenceType, val), (val) -> serializeHelper(mapper, typeRef, persistenceType, val), persistenceType, ignoreGetFailures);
    }

    public PersistAsJsonHelper(Callable<F> getter,
                               Consumer<F> setter,
                               FunctionWithException<F, E> deserializer,
                               FunctionWithException<E, F> serializer,
                               Class<F> persistenceType)
    {
        this(getter, setter, deserializer, serializer, persistenceType, false);
    }

    public PersistAsJsonHelper(Callable<F> getter,
                               Consumer<F> setter,
                               FunctionWithException<F, E> deserializer,
                               FunctionWithException<E, F> serializer,
                               Class<F> persistenceType,
                               boolean ignoreGetFailures)
    {
        if (persistenceType != String.class && persistenceType != byte[].class)
        {
            throw Exceptions.newIllegalArgumentException("Invalid persistence type: %s", persistenceType);
        }

        m_persistenceType = persistenceType;
        m_getter          = getter;
        m_setter          = setter;

        m_deserializer = deserializer;
        m_serializer   = serializer;

        m_ignoreGetFailures = ignoreGetFailures;
    }

    public E get()
    {
        if (!m_valueValid)
        {
            m_value      = getNoCaching();
            m_valueValid = true;
        }

        return m_value;
    }

    public E getNoCaching()
    {
        try
        {
            F valueRaw = m_getter.call();

            return m_deserializer.apply(valueRaw);
        }
        catch (Exception e)
        {
            if (m_ignoreGetFailures)
            {
                AbstractApplication.LoggerInstance.error("Failed to deserialize value: %s", e);
                return null;
            }

            throw new RuntimeException(e);
        }
    }

    public boolean set(E value)
    {
        try
        {
            m_value      = value;
            m_valueValid = true;

            F oldValueRaw = m_getter.call();
            F newValueRaw = m_serializer.apply(value);

            if (m_persistenceType == String.class)
            {
                if (StringUtils.equals((String) oldValueRaw, (String) newValueRaw))
                {
                    return false;
                }
            }
            else
            {
                if (Arrays.equals((byte[]) oldValueRaw, (byte[]) newValueRaw))
                {
                    return false;
                }
            }

            m_valueChanged = true;
            m_setter.accept(newValueRaw);
            return true;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public boolean wasChanged()
    {
        return m_valueChanged;
    }

    //--//

    private static <F, E> F serializeHelper(ObjectMapper mapper,
                                            Class<E> clz,
                                            Class<F> persistenceType,
                                            E val) throws
                                                   IOException
    {
        if (val == null)
        {
            return null;
        }

        return serializeHelper(mapper.writerFor(clz), persistenceType, val);
    }

    private static <F, E> F serializeHelper(ObjectMapper mapper,
                                            TypeReference<E> typeRef,
                                            Class<F> persistenceType,
                                            E val) throws
                                                   IOException
    {
        if (val == null)
        {
            return null;
        }

        return serializeHelper(mapper.writerFor(typeRef), persistenceType, val);
    }

    private static <F, E> F serializeHelper(ObjectWriter writer,
                                            Class<F> persistenceType,
                                            E val) throws
                                                   IOException
    {
        if (persistenceType == String.class)
        {
            return persistenceType.cast(writer.writeValueAsString(val));
        }
        else
        {
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            DeflaterOutputStream  compressor = new DeflaterOutputStream(compressed);

            writer.writeValue(compressor, val);

            return persistenceType.cast(compressed.toByteArray());
        }
    }

    private static <E, F> E deserializeHelper(ObjectMapper mapper,
                                              Class<E> clz,
                                              Class<F> persistenceType,
                                              F val) throws
                                                     IOException
    {
        if (val == null)
        {
            return null;
        }

        if (persistenceType == String.class)
        {
            return mapper.readValue((String) val, clz);
        }
        else
        {
            ByteArrayInputStream compressed   = new ByteArrayInputStream((byte[]) val);
            InflaterInputStream  decompressor = new InflaterInputStream(compressed);

            return mapper.readValue(decompressor, clz);
        }
    }

    private static <E, F> E deserializeHelper(ObjectMapper mapper,
                                              TypeReference<E> typeRef,
                                              Class<F> persistenceType,
                                              F val) throws
                                                     IOException
    {
        if (val == null)
        {
            return null;
        }

        if (persistenceType == String.class)
        {
            return mapper.readValue((String) val, typeRef);
        }
        else
        {
            ByteArrayInputStream compressed   = new ByteArrayInputStream((byte[]) val);
            InflaterInputStream  decompressor = new InflaterInputStream(compressed);

            return mapper.readValue(decompressor, typeRef);
        }
    }
}
