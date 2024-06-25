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
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseDataFieldModel;
import com.optio3.serialization.ConditionalFieldSelector;
import com.optio3.serialization.CustomTypeDescriptor;
import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationTag;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.serialization.TypeDescriptorFactory;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public class ProfilingCountersField extends BaseDataFieldModel
{
    public enum Id
    {
        // @formatter:off
        InternalBatteryVoltage        (  0), // 1 mV
        InternalBatteryCapacity       (  1), // 0.01 %
        EstimatedBatteryCapacityUsed  (  2), // 10 mAh
        MaximumTemperature            (  3), // 0.01 C°
        InitialInternalBatteryVoltage (  4), // 1 mV
        AverageSuccessfulGpsFixTime   (  5), // 1 s per fix
        AverageFailedGpsFixTime       (  6), // 1 s per failed fix
        AverageGpsFreshenTime         (  7), // 1 s per freshen attempt
        AverageWakeupsPerTrip         (  8), // 1 wakeup per trip
        InitialBatteryCapacityUsed    (  9), // 1 mAh
        SuccessfulUploads             (128), // 1 upload
        SuccessfulUploadTime          (129), // 1 s
        FailedUploads                 (130), // 1 upload
        FailedUploadTime              (131), // 1 s
        SuccessfulGpsFixes            (132), // 1 fix
        SuccessfulGpsFixTime          (133), // 1 s
        FailedGpsFixes                (134), // 1 fix
        FailedGpsFixTime              (135), // 1 s
        GpsFreshenAttempts            (136), // 1 attempt
        GpsFreshenTime                (137), // 1 s
        AccelerometerWakeups          (138), // 1 wakeup
        Trips                         (139), // 1 trip
        GpsFixesDueToUploadOnJostle   (140), // 1 fix
        UploadsDueToUploadOnJostle    (141), // 1 upload
        Uptime                        (142), // 1 s
        TxCount                       (143), // 1 tx
        RxCount                       (144), // 1 rx
        SuccessfulWifiScans           (145), // 1
        FailedWifiScans               (146), // 1
        SampleCount                   (147), // 1
        BleModuleUptime               (148), // 1 s
        BleModuleFailures             (149), // 1
        BleScans                      (150); // 1
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

    public static class Value implements ConditionalFieldSelector
    {
        @SerializationTag(number = 0, width = 8)
        public IdOrUnknown id;

        @SerializationTag(number = 1)
        public int value;

        @Override
        public boolean shouldEncode(String fieldName)
        {
            return true;
        }

        @Override
        public boolean shouldDecode(String fieldName)
        {
            return true;
        }

        @Override
        public boolean encodeValue(String fieldName,
                                   OutputBuffer buffer,
                                   Object value)
        {
            switch (fieldName)
            {
                case "value":
                    if (is32bit())
                    {
                        buffer.emit4Bytes(this.value);
                    }
                    else
                    {
                        buffer.emit2Bytes(this.value);
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
                case "value":
                    if (is32bit())
                    {
                        return Optional.of(buffer.read4BytesSigned());
                    }
                    else
                    {
                        return Optional.of(buffer.read2BytesSigned());
                    }
            }

            return Optional.empty();
        }

        //

        private boolean is32bit()
        {
            return (id.asLongValue() & 128) != 0;
        }
    }

    //--//

    @SerializationTag(number = 0)
    public Value[] payload;

    //--//

    public float extractFloat(Id id,
                              double scaling)
    {
        for (Value value : payload)
        {
            if (value.id.value == id)
            {
                return (float) (value.value * scaling);
            }
        }

        return Float.NaN;
    }

    public int extractInteger(Id id,
                              double scaling)
    {
        for (Value value : payload)
        {
            if (value.id.value == id)
            {
                return (int) (value.value * scaling);
            }
        }

        return Integer.MIN_VALUE;
    }
}
