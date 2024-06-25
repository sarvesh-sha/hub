/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseWireModel;
import com.optio3.lang.Unsigned32;
import com.optio3.serialization.SerializationHelper;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public class SendDataRecordPayload extends BaseWireModel
{
    private static final byte c_headerLength = 11;

    @SerializationTag(number = 0, width = 16)
    public int length;

    @SerializationTag(number = 2, width = 32)
    public Unsigned32 sequenceNumber;

    @SerializationTag(number = 6)
    public ZonedDateTime timestamp;

    @SerializationTag(number = 10)
    public byte logReason;

    @SerializationTag(number = 11)
    public List<DataFieldPayload> payload;

    //--//

    @Override
    public boolean encodeValue(String fieldName,
                               OutputBuffer buffer,
                               Object value)
    {
        switch (fieldName)
        {
            case "length":
                try (OutputBuffer nested = new OutputBuffer())
                {
                    nested.littleEndian = true;

                    if (payload != null)
                    {
                        SerializationHelper.write(nested, payload);
                    }

                    length = c_headerLength + nested.size();
                }
                break;

            case "timestamp":
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
            case "timestamp":
                return readTimestamp(buffer);

            case "payload":
                InputBuffer nested = buffer.readNestedBlock(length - c_headerLength);
                List<DataFieldPayload> fields = Lists.newArrayList();
                while (!nested.isEOF())
                {
                    DataFieldPayload field = new DataFieldPayload();
                    SerializationHelper.read(nested, field);
                    fields.add(field);
                }

                return Optional.of(fields);
        }

        return Optional.empty();
    }
}
