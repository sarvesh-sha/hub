/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseAsyncModel;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseWireModel;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.async.ConnectToOemAsync;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.async.DeleteSystemParameterAsync;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.async.DoVersionCheckAsync;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.async.RemoteResetAsync;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.async.SetOperationalModeAsync;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.async.SetSystemParameterAsync;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.async.UnknownAsync;
import com.optio3.lang.Unsigned32;
import com.optio3.lang.Unsigned8;
import com.optio3.serialization.CustomTypeDescriptor;
import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationHelper;
import com.optio3.serialization.SerializationTag;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.serialization.TypeDescriptorFactory;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public class AsyncMessageRequestPayload extends BaseWireModel
{
    public enum Id
    {
        // @formatter:off
        DoVersionCheck       (0x0001, DoVersionCheckAsync.class),
        RemoteReset          (0x0002, RemoteResetAsync.class),
        SetOperationalMode   (0x0003, SetOperationalModeAsync.class),
        ConnectToOem         (0x0007, ConnectToOemAsync.class),
        SetSystemParameter   (0x0008, SetSystemParameterAsync.class),
        DeleteSystemParameter(0x0009, DeleteSystemParameterAsync.class);
        // @formatter:on

        private final byte                            m_encoding;
        private final Class<? extends BaseAsyncModel> m_clz;
        private final IdOrUnknown                     m_singleton;

        Id(int encoding,
           Class<? extends BaseAsyncModel> clz)
        {
            m_encoding  = (byte) encoding;
            m_clz       = clz;
            m_singleton = new IdOrUnknown(this);
        }

        public static Id getEnum(BaseAsyncModel obj)
        {
            Class<? extends BaseWireModel> clz = obj.getClass();

            for (Id t : Id.values())
            {
                if (t.m_clz == clz)
                {
                    return t;
                }
            }

            return null;
        }

        public static Id parse(String value)
        {
            for (Id t : Id.values())
            {
                if (t.name()
                     .equalsIgnoreCase(value))
                {
                    return t;
                }
            }

            return null;
        }

        @HandlerForDecoding
        public static Id parse(short value)
        {
            for (Id t : Id.values())
            {
                if (t.m_encoding == value)
                {
                    return t;
                }
            }

            return null;
        }

        @HandlerForEncoding
        public short encoding()
        {
            return m_encoding;
        }

        public Class<? extends BaseAsyncModel> getPayload()
        {
            return m_clz;
        }

        public IdOrUnknown forRequest()
        {
            return m_singleton;
        }
    }

    @CustomTypeDescriptor(factory = IdOrUnknown.Factory.class)
    public static class IdOrUnknown
    {
        private static final int c_unknownMarker = -1;

        public static class Factory extends TypeDescriptorFactory
        {
            @Override
            public TypeDescriptor create(Class<?> clz)
            {
                TypeDescriptor td = Reflection.getDescriptor(Id.class);

                return new TypeDescriptor(clz, td.size, td.kind)
                {
                    @Override
                    public Object fromLongValue(long value)
                    {
                        Id objectType = Id.parse((short) value);

                        if (objectType != null)
                        {
                            return objectType.forRequest();
                        }

                        return new IdOrUnknown(null, value);
                    }

                    @Override
                    public long asLongValue(Object value)
                    {
                        IdOrUnknown v = (IdOrUnknown) value;

                        return v.asLongValue();
                    }
                };
            }
        }

        //--//

        public final Id   value;
        public final long unknown;

        private IdOrUnknown(Id value,
                            long unknown)
        {
            this.value   = value;
            this.unknown = unknown;
        }

        IdOrUnknown(Id value)
        {
            this.value   = value;
            this.unknown = c_unknownMarker;
        }

        @JsonCreator
        private IdOrUnknown(String value)
        {
            this.value   = Id.parse(value);
            this.unknown = this.value != null ? c_unknownMarker : Long.parseLong(value);
        }

        @JsonValue
        public String toJsonValue()
        {
            return unknown != c_unknownMarker ? Long.toString(unknown) : value.name();
        }

        public long asLongValue()
        {
            if (value != null)
            {
                return value.encoding();
            }

            return unknown;
        }

        @JsonIgnore
        public boolean isUnknown()
        {
            return unknown != c_unknownMarker;
        }

        public static IdOrUnknown parse(String value)
        {
            return new IdOrUnknown(value);
        }

        public Class<? extends BaseAsyncModel> getPayload()
        {
            if (value != null)
            {
                return value.getPayload();
            }

            return UnknownAsync.class;
        }

        //--//

        @Override
        public String toString()
        {
            if (value != null)
            {
                return value.toString();
            }

            return Long.toString(unknown);
        }
    }

    static class FletcherChecksum
    {
        byte check1;
        byte check2;

        FletcherChecksum(byte[] buf)
        {
            byte sum1 = 0;
            byte sum2 = 0;

            for (byte b : buf)
            {
                sum1 += b;
                sum2 += sum1;
            }

            check1 = (byte) (-(sum1 + sum2));
            check2 = (byte) (-(sum1 + check1));
        }
    }

    @SerializationTag(number = 5)
    public Unsigned32 messageId;

    @SerializationTag(number = 9)
    public Unsigned32 canAddress = Unsigned32.box(0xFFFFFFFF);

    @SerializationTag(number = 13, width = 16)
    public IdOrUnknown messageType;

    @SerializationTag(number = 15)
    public Unsigned8 flags;

    @SerializationTag(number = 16)
    public BaseAsyncModel payload;

    //--//

    public void setPayload(BaseAsyncModel payload)
    {
        this.payload = payload;

        Id id = Id.getEnum(payload);
        if (id != null)
        {
            messageType = id.forRequest();
        }
    }

    @Override
    public boolean encodeValue(String fieldName,
                               OutputBuffer buffer,
                               Object value)
    {
        switch (fieldName)
        {
            case "payload":
                try (OutputBuffer nested = new OutputBuffer())
                {
                    nested.littleEndian = true;

                    SerializationHelper.write(nested, payload);

                    byte[] payload = nested.toByteArray();

                    FletcherChecksum actual = new FletcherChecksum(payload);

                    buffer.emit(payload);
                    buffer.emit1Byte(actual.check1);
                    buffer.emit1Byte(actual.check2);

                    return true;
                }
        }

        return false;
    }

    @Override
    public Optional<Object> provideValue(String fieldName,
                                         InputBuffer buffer)
    {
        switch (fieldName)
        {
            case "payload":
                int len = buffer.remainingLength() - 2;

                try (InputBuffer nested = buffer.readNestedBlock(len))
                {
                    nested.littleEndian = true;

                    FletcherChecksum actual = new FletcherChecksum(nested.readByteArray(len));
                    nested.setPosition(0);

                    byte expectedSum1 = (byte) buffer.read1ByteUnsigned();
                    byte expectedSum2 = (byte) buffer.read1ByteUnsigned();

                    if (actual.check1 != expectedSum1 || actual.check2 != expectedSum2)
                    {
                        throw new RuntimeException("Invalid checksum");
                    }

                    BaseAsyncModel payload = Reflection.newInstance(messageType.getPayload());
                    SerializationHelper.read(nested, payload);
                    return Optional.of(payload);
                }
        }

        return Optional.empty();
    }
}
