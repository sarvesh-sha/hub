/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message;

import java.util.Optional;

import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseDataFieldModel;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseWireModel;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationHelper;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public class DataFieldPayload extends BaseWireModel
{
    @SerializationTag(number = 1)
    public BaseDataFieldModel.FieldTypeOrUnknown fieldId;

    @SerializationTag(number = 2)
    public int fieldLength;

    @SerializationTag(number = 3)
    public BaseDataFieldModel field;

    //--//

    public void setPayload(BaseDataFieldModel field)
    {
        this.field = field;

        BaseDataFieldModel.FieldType fieldType = BaseDataFieldModel.FieldType.getEnum(field);
        if (fieldType != null)
        {
            fieldId = fieldType.forRequest();
        }
    }

    @Override
    public boolean encodeValue(String fieldName,
                               OutputBuffer buffer,
                               Object value)
    {
        switch (fieldName)
        {
            case "fieldLength":
                if (field != null)
                {
                    try (OutputBuffer nested = new OutputBuffer())
                    {
                        nested.littleEndian = true;

                        SerializationHelper.write(nested, field);
                        fieldLength = nested.size();
                    }
                }
                else
                {
                    fieldLength = 0;
                }

                if (fieldLength <= 254)
                {
                    buffer.emit1Byte(fieldLength);
                }
                else
                {
                    buffer.emit1Byte(255);
                    buffer.emit2Bytes(fieldLength);
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
            case "fieldLength":
                int len = buffer.read1ByteUnsigned();
                if (len == 255)
                {
                    len = buffer.read2BytesUnsigned();
                }
                return Optional.of(len);

            case "field":
                InputBuffer nested = buffer.readNestedBlock(fieldLength);
                nested.littleEndian = true;

                BaseDataFieldModel field = Reflection.newInstance(fieldId.getPayload());
                SerializationHelper.read(nested, field);
                return Optional.of(field);
        }

        return Optional.empty();
    }
}
