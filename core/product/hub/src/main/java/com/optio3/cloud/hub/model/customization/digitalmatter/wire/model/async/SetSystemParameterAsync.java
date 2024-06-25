/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.async;

import java.time.ZonedDateTime;
import java.util.Optional;

import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseAsyncModel;
import com.optio3.lang.Unsigned16;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public class SetSystemParameterAsync extends BaseAsyncModel
{
    @SerializationTag(number = 0)
    public ZonedDateTime version;

    @SerializationTag(number = 4)
    public Unsigned16 id;

    @SerializationTag(number = 6)
    public byte[] value;

    //--//

    @Override
    public boolean encodeValue(String fieldName,
                               OutputBuffer buffer,
                               Object value)
    {
        switch (fieldName)
        {
            case "version":
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
            case "version":
                return readTimestamp(buffer);
        }

        return Optional.empty();
    }
}
