/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.optio3.serialization.Reflection;

public class WhoIsRange
{
    public final int low;
    public final int high;

    @JsonCreator
    public WhoIsRange(@JsonProperty("low") int low,
                      @JsonProperty("high") int high)
    {
        this.low  = low;
        this.high = high;
    }

    @Override
    public boolean equals(Object o)
    {
        WhoIsRange that = Reflection.as(o, WhoIsRange.class);
        if (that == null)
        {
            return false;
        }

        return low == that.low && high == that.high;
    }

    @Override
    public int hashCode()
    {
        int result = 1;

        result = 31 * result + Integer.hashCode(low);
        result = 31 * result + Integer.hashCode(high);

        return result;
    }
}
