/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.modbus.model.pdu;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.modbus.pdu.ApplicationPDU;
import com.optio3.protocol.model.modbus.error.ModbusDecodingException;
import com.optio3.serialization.ConditionalFieldSelector;
import com.optio3.serialization.SerializationHelper;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;

public class ReadDeviceIdentification extends ApplicationPDU
{
    public static class ObjectValue implements ConditionalFieldSelector
    {
        @SerializationTag(number = 1)
        public Unsigned8 objectId;

        @SerializationTag(number = 2)
        public String text;

        //--//

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
        public Optional<Object> provideValue(String fieldName,
                                             InputBuffer buffer)
        {
            switch (fieldName)
            {
                case "text":
                    int len = buffer.read1ByteUnsigned();
                    return Optional.of(buffer.readBytesAsString(len, Charsets.ISO_8859_1));
            }

            return Optional.empty();
        }
    }

    public static class Response extends ApplicationPDU.Response implements ConditionalFieldSelector
    {
        @SerializationTag(number = 1)
        public Unsigned8 meiType;

        @SerializationTag(number = 2)
        public Unsigned8 readDeviceIdCode;

        @SerializationTag(number = 3, width = 8)
        public int conformityLevel;

        @SerializationTag(number = 4, width = 8)
        public int moreFollows;

        @SerializationTag(number = 5)
        public Unsigned8 nextObjectId;

        @SerializationTag(number = 7)
        public List<ObjectValue> values;

        //--//

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
        public Optional<Object> provideValue(String fieldName,
                                             InputBuffer buffer)
        {
            switch (fieldName)
            {
                case "values":
                    List<ObjectValue> lst = Lists.newArrayList();
                    int numOfObjects = buffer.read1ByteUnsigned();
                    for (int i = 0; i < numOfObjects; i++)
                    {
                        ObjectValue obj = new ObjectValue();
                        SerializationHelper.read(buffer, obj);
                        lst.add(obj);
                    }

                    return Optional.of(lst);
            }

            return Optional.empty();
        }

        @Override
        protected void validateAfterDecoding()
        {
            if (values == null)
            {
                throw ModbusDecodingException.newException("Invalid ReadDeviceIdentification.Response");
            }
        }
    }

    @SerializationTag(number = 1)
    public Unsigned8 meiType = Unsigned8.box(0x0E);

    @SerializationTag(number = 2)
    public Unsigned8 readDeviceIdCode = Unsigned8.box(0x01); // request to get the basic device identification (stream access)

    @SerializationTag(number = 3)
    public Unsigned8 objectId;

    //--//

    @Override
    protected void validateAfterDecoding()
    {
    }
}
