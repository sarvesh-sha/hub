/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.timeseries;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.TypeDescriptor;

public class TimeSeriesPropertyResponse
{
    public double[]   values;
    public String[]   enumLookup;
    public String[][] enumSetLookup;

    public ZonedDateTime nextTimestamp;

    @JsonIgnore
    public ZoneId timeZone;

    //--//

    @JsonIgnore
    public TimeSeriesPropertyType propertySchema = new TimeSeriesPropertyType();

    @JsonIgnore
    public Class<?> expectedType;

    @JsonIgnore
    public double[] timestamps;

    private Boolean m_shouldInterpolate;

    //--//

    public static void deltaEncode(double[] values)
    {
        double previousValue = Double.NaN;

        for (int i = 0; i < values.length; i++)
        {
            double value = values[i];

            if (!Double.isNaN(previousValue) && !Double.isNaN(value))
            {
                values[i] = value - previousValue;
            }
            else
            {
                values[i] = value;
            }

            previousValue = value;
        }
    }

    public static void deltaDecode(double[] values)
    {
        double previousValue = Double.NaN;

        for (int i = 0; i < values.length; i++)
        {
            double delta = values[i];
            double currentValue;

            if (!Double.isNaN(previousValue) && !Double.isNaN(delta))
            {
                currentValue = previousValue + delta;
            }
            else
            {
                currentValue = delta;
            }

            values[i] = currentValue;

            previousValue = currentValue;
        }
    }

    //--//

    public TimeSeriesPropertyResponse copy()
    {
        TimeSeriesPropertyResponse res = new TimeSeriesPropertyResponse();

        res.values        = Arrays.copyOf(values, values.length);
        res.enumLookup    = enumLookup;
        res.enumSetLookup = enumSetLookup;

        res.nextTimestamp = nextTimestamp;
        res.timeZone      = timeZone;

        res.propertySchema = propertySchema;

        res.expectedType = expectedType;
        res.timestamps   = timestamps;

        res.m_shouldInterpolate = m_shouldInterpolate;

        return res;
    }

    @JsonIgnore
    public boolean shouldInterpolate()
    {
        if (m_shouldInterpolate == null)
        {
            // No interpolation of non-numeric values.
            if (enumLookup != null || enumSetLookup != null)
            {
                m_shouldInterpolate = false;
            }
            else if (expectedType == null)
            {
                // No info, skip interpolation.
                m_shouldInterpolate = false;
            }
            else if (!propertySchema.couldInterpolate())
            {
                m_shouldInterpolate = false;
            }
            else
            {
                TypeDescriptor td = Reflection.getDescriptor(expectedType);
                if (td != null && td.isFloatingType())
                {
                    m_shouldInterpolate = true;
                }
                else
                {
                    m_shouldInterpolate = false;
                }
            }
        }

        return m_shouldInterpolate;
    }

    @JsonIgnore
    public void setUnits(EngineeringUnits newUnits)
    {
        if (newUnits != EngineeringUnits.enumerated)
        {
            enumLookup    = null;
            enumSetLookup = null;
        }

        propertySchema = propertySchema.copy();
        propertySchema.setUnits(newUnits);
    }

    @JsonIgnore
    public void setUnitsFactors(EngineeringUnitsFactors newUnitsFactors)
    {
        if (newUnitsFactors.getPrimary() != EngineeringUnits.enumerated)
        {
            enumLookup    = null;
            enumSetLookup = null;
        }

        propertySchema = propertySchema.copy();
        propertySchema.setUnitsFactors(newUnitsFactors);
    }

    @JsonIgnore
    public EngineeringUnitsFactors getUnitsFactors()
    {
        EngineeringUnitsFactors unitsFactors = propertySchema.unitsFactors;
        return unitsFactors != null ? unitsFactors : EngineeringUnitsFactors.Dimensionless;
    }
}
