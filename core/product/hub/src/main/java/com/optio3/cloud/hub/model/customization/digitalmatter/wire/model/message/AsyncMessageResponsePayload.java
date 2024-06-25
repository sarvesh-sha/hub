/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseWireModel;
import com.optio3.lang.Unsigned32;
import com.optio3.serialization.CustomTypeDescriptor;
import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationTag;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.serialization.TypeDescriptorFactory;

public class AsyncMessageResponsePayload extends BaseWireModel
{
    public enum Status
    {
        // @formatter:off
        Failure       (0x0000),
        Success       (0x0001),
        RetryLater    (0x0002);
        // @formatter:on

        private final byte            m_encoding;
        private final StatusOrUnknown m_singleton;

        Status(int encoding)
        {
            m_encoding  = (byte) encoding;
            m_singleton = new StatusOrUnknown(this);
        }

        public static Status parse(String value)
        {
            for (Status t : Status.values())
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
        public static Status parse(byte value)
        {
            for (Status t : Status.values())
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

        public StatusOrUnknown forRequest()
        {
            return m_singleton;
        }
    }

    @CustomTypeDescriptor(factory = StatusOrUnknown.Factory.class)
    public static class StatusOrUnknown
    {
        private static final int c_unknownMarker = -1;

        public static class Factory extends TypeDescriptorFactory
        {
            @Override
            public TypeDescriptor create(Class<?> clz)
            {
                TypeDescriptor td = Reflection.getDescriptor(Status.class);

                return new TypeDescriptor(clz, td.size, td.kind)
                {
                    @Override
                    public Object fromLongValue(long value)
                    {
                        Status objectType = Status.parse((byte) value);

                        if (objectType != null)
                        {
                            return objectType.forRequest();
                        }

                        return new StatusOrUnknown(null, value);
                    }

                    @Override
                    public long asLongValue(Object value)
                    {
                        StatusOrUnknown v = (StatusOrUnknown) value;

                        return v.asLongValue();
                    }
                };
            }
        }

        //--//

        public final Status value;
        public final long   unknown;

        private StatusOrUnknown(Status value,
                                long unknown)
        {
            this.value   = value;
            this.unknown = unknown;
        }

        StatusOrUnknown(Status value)
        {
            this.value   = value;
            this.unknown = c_unknownMarker;
        }

        @JsonCreator
        private StatusOrUnknown(String value)
        {
            this.value   = Status.parse(value);
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

        public static StatusOrUnknown parse(String value)
        {
            return new StatusOrUnknown(value);
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

    @SerializationTag(number = 5)
    public Unsigned32 messageId;

    @SerializationTag(number = 9)
    public StatusOrUnknown status;
}
