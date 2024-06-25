/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet;

import java.io.IOException;
import java.util.BitSet;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public final class BACnetModule extends SimpleModule
{
    @SuppressWarnings("unchecked")
    public BACnetModule()
    {
        addSerializer(BitSet.class, BitSetSerializer.INSTANCE);
        addDeserializer(BitSet.class, BitSetDeserializer.INSTANCE);

        addSerializer(BACnetObjectModel.class, BACnetObjectModel.Serializer.INSTANCE);

        for (Class<? extends BACnetObjectModel> subType : BACnetObjectModel.getSubTypes())
        {
            addDeserializer((Class<BACnetObjectModel>) subType, BACnetObjectModel.Deserializer.getInstance(subType));
        }
    }

    //--//

    static class BitSetSerializer extends StdSerializer<BitSet>
    {
        private static final long serialVersionUID = 1L;

        static final BitSetSerializer INSTANCE = new BitSetSerializer();

        private BitSetSerializer()
        {
            super(BitSet.class);
        }

        @Override
        public boolean isEmpty(SerializerProvider provider,
                               BitSet value)
        {
            return value == null;
        }

        @Override
        public void serialize(BitSet value,
                              JsonGenerator jgen,
                              SerializerProvider provider) throws
                                                           IOException
        {
            jgen.writeBinary(value.toByteArray());
        }
    }

    static class BitSetDeserializer extends StdDeserializer<BitSet>
    {
        private static final long serialVersionUID = 1L;

        static final BitSetDeserializer INSTANCE = new BitSetDeserializer();

        private BitSetDeserializer()
        {
            super(BitSet.class);
        }

        @Override
        public BitSet getNullValue(DeserializationContext ctxt)
        {
            return null;
        }

        @Override
        public BitSet deserialize(JsonParser jp,
                                  DeserializationContext ctxt) throws
                                                               IOException
        {
            return BitSet.valueOf(jp.getBinaryValue());
        }
    }
}
