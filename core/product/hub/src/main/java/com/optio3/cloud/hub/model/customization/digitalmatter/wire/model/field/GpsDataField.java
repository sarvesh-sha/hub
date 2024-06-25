/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field;

import java.time.ZonedDateTime;
import java.util.Optional;

import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseDataFieldModel;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public class GpsDataField extends BaseDataFieldModel
{
    @SerializationTag(number = 0)
    public ZonedDateTime timestamp;

    @SerializationTag(number = 4, width = 32, scaling = { @SerializationScaling(scalingFactor = 1E-7) })
    public double latitude;

    @SerializationTag(number = 8, width = 32, scaling = { @SerializationScaling(scalingFactor = 1E-7) })
    public double longitude;

    @SerializationTag(number = 12, width = 16) // Meters
    public int altitude;

    @SerializationTag(number = 14, width = 16) // Cm/s
    public int groundSpeed;

    @SerializationTag(number = 16, width = 8, scaling = { @SerializationScaling(scalingFactor = 10) }) // Cm/s
    public int groundSpeedAccuracy;

    @SerializationTag(number = 17, width = 8, scaling = { @SerializationScaling(scalingFactor = 2) }) // Degree
    public int heading;

    @SerializationTag(number = 18, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float pdop;

    @SerializationTag(number = 19, width = 8) // Meters
    public int positionAccuracy;

    @SerializationTag(number = 20, width = 1, bitOffset = 0)
    public boolean fixValid;

    @SerializationTag(number = 20, width = 1, bitOffset = 1)
    public boolean fix3D;

    @SerializationTag(number = 20, width = 1, bitOffset = 2)
    public boolean oldFix;

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
