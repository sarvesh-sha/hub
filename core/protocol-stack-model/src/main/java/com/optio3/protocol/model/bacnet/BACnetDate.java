/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet;

import com.optio3.serialization.SerializationTag;

public final class BACnetDate
{
    @SerializationTag(number = 0, width = 8)
    public int year; // Since 1900

    @SerializationTag(number = 1, width = 8)
    public int month; // January = 1

    @SerializationTag(number = 2, width = 8)
    public int day; // January = 1

    @SerializationTag(number = 3, width = 8)
    public int dayOfWeek; // Monday = 1

    @Override
    public String toString()
    {
        return String.format("Date:%s:%s:%s:%s", toPrint(year), toPrint(month), toPrint(day), toPrint(dayOfWeek));
    }

    private Object toPrint(int val)
    {
        return val == 255 ? "Any" : val;
    }
}
