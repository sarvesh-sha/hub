/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet;

import com.optio3.serialization.SerializationTag;

public final class BACnetTime
{
    @SerializationTag(number = 0, width = 8)
    public int hour;

    @SerializationTag(number = 1, width = 8)
    public int minute;

    @SerializationTag(number = 2, width = 8)
    public int second;

    @SerializationTag(number = 3, width = 8)
    public int hundredthOfSecond;

    @Override
    public String toString()
    {
        return String.format("Time:%s:%s:%s:%s", toPrint(hour), toPrint(minute), toPrint(second), toPrint(hundredthOfSecond));
    }

    private Object toPrint(int val)
    {
        return val == 255 ? "Any" : val;
    }
}
