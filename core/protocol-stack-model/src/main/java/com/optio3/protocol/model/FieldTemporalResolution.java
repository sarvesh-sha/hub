/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

public enum FieldTemporalResolution
{
    Max1Hz(1),
    Max100Hz(100),
    Max1000Hz(1000);

    public final double scalingFactor;
    public final double inverseScalingFactor;

    FieldTemporalResolution(int scalingFactor)
    {
        this.scalingFactor = scalingFactor;
        this.inverseScalingFactor = 1.0 / scalingFactor;
    }

    public FieldTemporalResolution maxResolution(FieldTemporalResolution other)
    {
        return scalingFactor > other.scalingFactor ? this : other;
    }

    public double adjustTimestamp(double previousTimestamp,
                                  double newTimestamp)
    {
        double minNewTimestamp = previousTimestamp + inverseScalingFactor;

        return Math.max(minNewTimestamp, newTimestamp);
    }

    public double truncateTimestamp(double timestamp)
    {
        long wholeNumber = (long) Math.floor(timestamp * scalingFactor);

        return wholeNumber * inverseScalingFactor;
    }
}
