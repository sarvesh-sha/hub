/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field.AnalogData16Field;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field.AnalogData32Field;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field.DebugEventField;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field.DeviceTripField;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field.DigitalDataField;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field.GpsDataField;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field.ProfilingCountersField;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field.UnknownDataField;
import com.optio3.serialization.CustomTypeDescriptor;
import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.serialization.TypeDescriptorFactory;

public abstract class BaseDataFieldModel extends BaseWireModel
{
    public enum FieldType
    {
        // @formatter:off
        GpsData                 (0 , GpsDataField.class),
        DebugEvent              (1 , DebugEventField.class),
        DigitalData             (2 , DigitalDataField.class),
//      DriverId                (3 , DriverIdField.class),
//      DeviceId                (4 , DeviceIdField.class),
//      DeviceMeasurement       (5 , DeviceMeasurementField.class),
        AnalogData16            (6 , AnalogData16Field.class),
        AnalogData32            (7 , AnalogData32Field.class),
        DeviceTrip              (15, DeviceTripField.class),
//      AccidentData            (17, AccidentDataField.class),
        ProfilingCounters       (21, ProfilingCountersField.class)/*,
        TimeResponse            (31, TimeResponsePayload.class)*/;
        // @formatter:on

        private final byte                                m_encoding;
        private final Class<? extends BaseDataFieldModel> m_clz;
        private final FieldTypeOrUnknown                  m_singleton;

        FieldType(int encoding,
                  Class<? extends BaseDataFieldModel> clz)
        {
            m_encoding  = (byte) encoding;
            m_clz       = clz;
            m_singleton = new FieldTypeOrUnknown(this);
        }

        public static FieldType getEnum(BaseDataFieldModel obj)
        {
            Class<? extends BaseWireModel> clz = obj.getClass();

            for (FieldType t : FieldType.values())
            {
                if (t.m_clz == clz)
                {
                    return t;
                }
            }

            return null;
        }

        public static FieldType parse(String value)
        {
            for (FieldType t : FieldType.values())
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
        public static FieldType parse(byte value)
        {
            for (FieldType t : FieldType.values())
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

        public Class<? extends BaseDataFieldModel> getPayload()
        {
            return m_clz;
        }

        public FieldTypeOrUnknown forRequest()
        {
            return m_singleton;
        }
    }

    @CustomTypeDescriptor(factory = FieldTypeOrUnknown.Factory.class)
    public static class FieldTypeOrUnknown
    {
        private static final int c_unknownMarker = -1;

        public static class Factory extends TypeDescriptorFactory
        {
            @Override
            public TypeDescriptor create(Class<?> clz)
            {
                TypeDescriptor td = Reflection.getDescriptor(FieldType.class);

                return new TypeDescriptor(clz, td.size, td.kind)
                {
                    @Override
                    public Object fromLongValue(long value)
                    {
                        FieldType objectType = FieldType.parse((byte) value);

                        if (objectType != null)
                        {
                            return objectType.forRequest();
                        }

                        return new FieldTypeOrUnknown(null, value);
                    }

                    @Override
                    public long asLongValue(Object value)
                    {
                        FieldTypeOrUnknown v = (FieldTypeOrUnknown) value;

                        return v.asLongValue();
                    }
                };
            }
        }

        //--//

        public final FieldType value;
        public final long      unknown;

        private FieldTypeOrUnknown(FieldType value,
                                   long unknown)
        {
            this.value   = value;
            this.unknown = unknown;
        }

        FieldTypeOrUnknown(FieldType value)
        {
            this.value   = value;
            this.unknown = c_unknownMarker;
        }

        @JsonCreator
        private FieldTypeOrUnknown(String value)
        {
            this.value   = FieldType.parse(value);
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

        public static FieldTypeOrUnknown parse(String value)
        {
            return new FieldTypeOrUnknown(value);
        }

        public Class<? extends BaseDataFieldModel> getPayload()
        {
            if (value != null)
            {
                return value.getPayload();
            }

            return UnknownDataField.class;
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
}
