/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Charsets;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseDataFieldModel;
import com.optio3.serialization.CustomTypeDescriptor;
import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationTag;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.serialization.TypeDescriptorFactory;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public class DebugEventField extends BaseDataFieldModel
{
    public enum SeverityId
    {
        // @formatter:off
        Info    (  0),
        Warning (  1),
        Severe  (  2),
        Critical(  3);
        // @formatter:on

        private final byte m_encoding;

        SeverityId(int encoding)
        {
            m_encoding = (byte) encoding;
        }

        public static SeverityId parse(String value)
        {
            for (SeverityId t : values())
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
        public static SeverityId parse(byte value)
        {
            for (SeverityId t : values())
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
    }

    //--//

    public enum EventId
    {
        // @formatter:off
        GeneralDebugMessage (  0),
        ModemFirmwareVersion(200),
        IridiumIMEI         (201),
        BluetoothMAC        (202),
        WiFiMAC             (203);
        // @formatter:on

        private final byte             m_encoding;
        private final EventIdOrUnknown m_singleton;

        EventId(int encoding)
        {
            m_encoding  = (byte) encoding;
            m_singleton = new EventIdOrUnknown(this);
        }

        public static EventId parse(String value)
        {
            for (EventId t : values())
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
        public static EventId parse(byte value)
        {
            for (EventId t : values())
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

        public EventIdOrUnknown forRequest()
        {
            return m_singleton;
        }
    }

    @CustomTypeDescriptor(factory = EventIdOrUnknown.Factory.class)
    public static class EventIdOrUnknown
    {
        private static final int c_unknownMarker = -1;

        public static class Factory extends TypeDescriptorFactory
        {
            @Override
            public TypeDescriptor create(Class<?> clz)
            {
                TypeDescriptor td = Reflection.getDescriptor(EventId.class);

                return new TypeDescriptor(clz, td.size, td.kind)
                {
                    @Override
                    public Object fromLongValue(long value)
                    {
                        EventId objectType = EventId.parse((byte) value);

                        if (objectType != null)
                        {
                            return objectType.forRequest();
                        }

                        return new EventIdOrUnknown(null, value);
                    }

                    @Override
                    public long asLongValue(Object value)
                    {
                        EventIdOrUnknown v = (EventIdOrUnknown) value;

                        return v.asLongValue();
                    }
                };
            }
        }

        //--//

        public final EventId value;
        public final long    unknown;

        private EventIdOrUnknown(EventId value,
                                 long unknown)
        {
            this.value   = value;
            this.unknown = unknown;
        }

        EventIdOrUnknown(EventId value)
        {
            this.value   = value;
            this.unknown = c_unknownMarker;
        }

        @JsonCreator
        private EventIdOrUnknown(String value)
        {
            this.value   = EventId.parse(value);
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

        public static EventIdOrUnknown parse(String value)
        {
            return new EventIdOrUnknown(value);
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

    @SerializationTag(number = 0, width = 2, bitOffset = 0)
    public SeverityId severity;

    @SerializationTag(number = 0, width = 5, bitOffset = 2)
    public int moduleId;

    @SerializationTag(number = 1)
    public EventIdOrUnknown eventCode;

    @SerializationTag(number = 2)
    public String text;

    //--//

    @Override
    public boolean encodeValue(String fieldName,
                               OutputBuffer buffer,
                               Object value)
    {
        switch (fieldName)
        {
            case "text":
                for (int i = 0; i < text.length(); i++)
                {
                    buffer.emit1Byte(text.charAt(i));
                }
                return true;
        }

        return false;
    }

    @Override
    public Optional<Object> provideValue(String fieldName,
                                         InputBuffer buffer)
    {
        switch (fieldName)
        {
            case "text":
                byte[] bytes = buffer.readByteArray(buffer.remainingLength());
                return Optional.of(new String(bytes, Charsets.US_ASCII));
        }

        return Optional.empty();
    }
}
