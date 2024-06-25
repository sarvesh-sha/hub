/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message;

import java.time.ZonedDateTime;
import java.util.Optional;

import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseWireModel;
import com.optio3.lang.Unsigned32;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public class VersionDataPayload extends BaseWireModel
{
    @SerializationTag(number = 5)
    public Unsigned32 deviceSerial;

    @SerializationTag(number = 9)
    public Unsigned32 canAddress = Unsigned32.box(0xFFFFFFFF);

    @SerializationTag(number = 13)
    public ZonedDateTime[] versions;

    //--//

    @Override
    public boolean encodeValue(String fieldName,
                               OutputBuffer buffer,
                               Object value)
    {
        switch (fieldName)
        {
            case "versions":
                int len = versions != null ? versions.length : 0;
                buffer.emit1Byte(len);
                for (int i = 0; i < len; i++)
                {
                    emitTimestamp(buffer, versions[i]);
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
            case "versions":
                int len = buffer.read1ByteUnsigned();
                ZonedDateTime[] versions = new ZonedDateTime[len];
                for (int i = 0; i < len; i++)
                {
                    Optional<Object> t = readTimestamp(buffer);
                    versions[i] = (ZonedDateTime) t.get();
                }
                return Optional.of(versions);
        }

        return Optional.empty();
    }
}
