/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseDataFieldModel;
import com.optio3.lang.Unsigned16;
import com.optio3.serialization.CustomTypeDescriptor;
import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationTag;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.serialization.TypeDescriptorFactory;

public class DeviceTripField extends BaseDataFieldModel
{
    public enum Id
    {
        // @formatter:off
        None     (0),
        Ignition (1),
        Movement (2),
        RunDetect(3);
        // @formatter:on

        private final byte        m_encoding;
        private final IdOrUnknown m_singleton;

        Id(int encoding)
        {
            m_encoding  = (byte) encoding;
            m_singleton = new IdOrUnknown(this);
        }

        public static Id parse(String value)
        {
            for (Id t : values())
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
        public static Id parse(byte value)
        {
            for (Id t : values())
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
                        Id objectType = Id.parse((byte) value);

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

    //--//

    @SerializationTag(number = 0)
    public IdOrUnknown type;

    @SerializationTag(number = 1) // Seconds
    public Unsigned16 trimmingAmount;
}
