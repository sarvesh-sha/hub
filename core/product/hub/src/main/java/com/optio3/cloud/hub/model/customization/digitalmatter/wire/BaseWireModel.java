/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import com.optio3.serialization.ConditionalFieldSelector;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.TimeUtils;

public abstract class BaseWireModel implements ConditionalFieldSelector
{
    private static final long c_timeOrigin = ZonedDateTime.of(2013, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
                                                          .toEpochSecond();

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

    //--//

    protected boolean emitString(OutputBuffer buffer,
                                 String value,
                                 int len)
    {
        for (int i = 0; i < len; i++)
        {
            buffer.emit1Byte(i < value.length() ? value.charAt(i) : 0);
        }
        return true;
    }

    protected Optional<Object> readString(InputBuffer buffer,
                                          int len)
    {
        byte[] buf = buffer.readByteArray(len);

        for (int i = 0; i < len; i++)
        {
            if (buf[i] == 0)
            {
                return Optional.of(new String(buf, 0, i));
            }
        }

        return Optional.of(new String(buf));
    }

    protected boolean emitTimestamp(OutputBuffer buffer,
                                    ZonedDateTime value)
    {
        if (value == null)
        {
            buffer.emit4Bytes(0);
        }
        else
        {
            buffer.emit4Bytes((int) (value.toEpochSecond() - c_timeOrigin));
        }

        return true;
    }

    protected Optional<Object> readTimestamp(InputBuffer buffer)
    {
        long timeOffset = buffer.read4BytesUnsigned();
        return Optional.of(TimeUtils.fromSecondsToUtcTime(c_timeOrigin + timeOffset));
    }
}
