/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.timeseries;

import java.util.Objects;

public class TimeSeriesExtractSample
{
    public double timestamp;
    public double numericValue = Double.NaN;
    public Object genericValue;

    public boolean isValid()
    {
        return genericValue != null || !Double.isNaN(numericValue);
    }

    public boolean sameValue(TimeSeriesExtractSample other)
    {
        boolean thisValid  = isValid();
        boolean otherValid = other != null && other.isValid();

        if (!thisValid || !otherValid)
        {
            return thisValid == otherValid;
        }

        if (genericValue != null || other.genericValue != null)
        {
            return Objects.equals(genericValue, other.genericValue);
        }
        else
        {
            return numericValue == other.numericValue;
        }
    }
}
