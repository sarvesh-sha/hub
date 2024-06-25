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
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseWireModel;
import com.optio3.lang.Unsigned32;
import com.optio3.serialization.CustomTypeDescriptor;
import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationTag;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.serialization.TypeDescriptorFactory;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public class HelloPayload extends BaseWireModel
{
    public enum Id
    {
        // @formatter:off
        G52S             (17),
        G60              (23),
        G100             (28),
        Remora           (33),
        Dart             (34),
        Sting            (52),
        OysterCellular   (58),
        Remora2          (62),
        G62              (67),
        DartV2           (68),
        G200             (70),
        YabbyWiFiCellular(72),
        YabbyGPSCellular (73),
        FalconCellular   (74),
        Bolt             (75),
        Oyster2          (77),
        Eagle            (78),
        G120             (79);
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

    @SerializationTag(number = 5)
    public Unsigned32 deviceSerial;

    @SerializationTag(number = 9)
    public String imei;

    @SerializationTag(number = 25)
    public String iccid;

    @SerializationTag(number = 46)
    public IdOrUnknown productId;

    @SerializationTag(number = 47)
    public byte hardwareRevisionNumber;

    @SerializationTag(number = 48)
    public byte firmwareMajor;

    @SerializationTag(number = 49)
    public byte firmwareMinor;

    @SerializationTag(number = 50)
    public Unsigned32 flags;

    //--//

    @Override
    public boolean encodeValue(String fieldName,
                               OutputBuffer buffer,
                               Object value)
    {
        switch (fieldName)
        {
            case "imei":
                return emitString(buffer, (String) value, 16);

            case "iccid":
                return emitString(buffer, (String) value, 21);
        }

        return false;
    }

    @Override
    public Optional<Object> provideValue(String fieldName,
                                         InputBuffer buffer)
    {
        switch (fieldName)
        {
            case "imei":
                return readString(buffer, 16);

            case "iccid":
                return readString(buffer, 21);
        }

        return Optional.empty();
    }
}
