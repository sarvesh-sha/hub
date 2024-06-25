/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message;

import java.time.ZonedDateTime;
import java.util.Optional;

import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseWireModel;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public class TimeResponsePayload extends BaseWireModel
{
    @SerializationTag(number = 1)
    public ZonedDateTime timestamp;

    //--//

    @Override
    public boolean encodeValue(String fieldName,
                               OutputBuffer buffer,
                               Object value)
    {
        switch (fieldName)
        {
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
        }

        return Optional.empty();
    }
}
