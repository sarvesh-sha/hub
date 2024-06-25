/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.async;

import java.time.ZonedDateTime;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseAsyncModel;
import com.optio3.serialization.CustomTypeDescriptor;
import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationTag;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.serialization.TypeDescriptorFactory;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public class SetOperationalModeAsync extends BaseAsyncModel
{
    public enum ModeId
    {
        // @formatter:off
        Production(  0),
        Normal    (  1),
        Shipping  (  2),
        Recovery  (  3);
        // @formatter:on

        private final byte            m_encoding;
        private final ModeIdOrUnknown m_singleton;

        ModeId(int encoding)
        {
            m_encoding  = (byte) encoding;
            m_singleton = new ModeIdOrUnknown(this);
        }

        public static ModeId parse(String value)
        {
            for (ModeId t : values())
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
        public static ModeId parse(short value)
        {
            for (ModeId t : values())
            {
                if (t.m_encoding == value)
                {
                    return t;
                }
            }

            return null;
        }

        @HandlerForEncoding
        public byte encoding()
        {
            return m_encoding;
        }

        public ModeIdOrUnknown forRequest()
        {
            return m_singleton;
        }
    }

    @CustomTypeDescriptor(factory = ModeIdOrUnknown.Factory.class)
    public static class ModeIdOrUnknown implements Comparable<ModeIdOrUnknown>
    {
        private static final int c_unknownMarker = -1;

        public static class Factory extends TypeDescriptorFactory
        {
            @Override
            public TypeDescriptor create(Class<?> clz)
            {
                TypeDescriptor td = Reflection.getDescriptor(ModeId.class);

                return new TypeDescriptor(clz, td.size, td.kind)
                {
                    @Override
                    public Object fromLongValue(long value)
                    {
                        ModeId objectType = ModeId.parse((byte) value);

                        if (objectType != null)
                        {
                            return objectType.forRequest();
                        }

                        return new ModeIdOrUnknown(null, value);
                    }

                    @Override
                    public long asLongValue(Object value)
                    {
                        ModeIdOrUnknown v = (ModeIdOrUnknown) value;

                        return v.asLongValue();
                    }
                };
            }
        }

        //--//

        public final ModeId value;
        public final long   unknown;

        private ModeIdOrUnknown(ModeId value,
                                long unknown)
        {
            this.value   = value;
            this.unknown = unknown;
        }

        ModeIdOrUnknown(ModeId value)
        {
            this.value   = value;
            this.unknown = c_unknownMarker;
        }

        @JsonCreator
        private ModeIdOrUnknown(String value)
        {
            this.value   = ModeId.parse(value);
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

        public static ModeIdOrUnknown parse(String value)
        {
            return new ModeIdOrUnknown(value);
        }

        //--//

        @Override
        public int hashCode()
        {
            return Long.hashCode(asLongValue());
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }

            Number num = Reflection.as(o, Number.class);
            if (num != null)
            {
                return num.longValue() == asLongValue();
            }

            ModeIdOrUnknown ot1 = Reflection.as(o, ModeIdOrUnknown.class);
            if (ot1 != null)
            {
                return value == ot1.value && unknown == ot1.unknown;
            }

            ModeId ot2 = Reflection.as(o, ModeId.class);
            if (ot2 != null)
            {
                return value == ot2;
            }

            return false;
        }

        @Override
        public int compareTo(ModeIdOrUnknown o)
        {
            return compare(this, o);
        }

        public static int compare(ModeIdOrUnknown o1,
                                  ModeIdOrUnknown o2)
        {
            if (o1 == o2)
            {
                return 0;
            }

            if (o1 == null)
            {
                return 1;
            }

            if (o2 == null)
            {
                return -1;
            }

            long l1 = o1.asLongValue();
            long l2 = o2.asLongValue();

            return Long.compare(l1, l2);
        }

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

    @SerializationTag(number = 0)
    public ModeIdOrUnknown mode;

    @SerializationTag(number = 1)
    public ZonedDateTime validUntil;

    //--//

    @Override
    public boolean encodeValue(String fieldName,
                               OutputBuffer buffer,
                               Object value)
    {
        switch (fieldName)
        {
            case "validUntil":
                return emitTimestamp(buffer, (ZonedDateTime) value);
        }

        return false;
    }

    @Override
    public Optional<Object> provideValue(String fieldName,
                                         InputBuffer buffer)
    {
        switch (fieldName)
        {
            case "validUntil":
                return readTimestamp(buffer);
        }

        return Optional.empty();
    }
}
