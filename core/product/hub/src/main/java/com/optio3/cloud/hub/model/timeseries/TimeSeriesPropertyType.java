/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.timeseries;

import java.lang.reflect.Type;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.cloud.model.IEnumDescription;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.protocol.model.EngineeringUnitsFamily;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.serialization.TypedBitSet;

public class TimeSeriesPropertyType
{
    public String                          name;
    public String                          displayName;
    public TimeSeries.SampleType           type;
    public TimeSeries.SampleResolution     resolution        = TimeSeries.SampleResolution.Max1Hz;
    public int                             digitsOfPrecision = 0;
    public boolean                         isBoolean;
    public EngineeringUnitsFactors         unitsFactors;
    public List<TimeSeriesEnumeratedValue> values;
    public int                             debounceSeconds;
    public double                          noValueMarker     = -1E100;

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setUnitsFamily(JsonNode node)
    {
        HubApplication.reportPatchCall(node);
    }

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setEquivalentUnits(JsonNode node)
    {
        HubApplication.reportPatchCall(node);
    }

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setPrimaryUnits(EngineeringUnits primaryUnits)
    {
        HubApplication.reportPatchCall(primaryUnits);
    }

    @JsonIgnore
    public Type expectedType;

    @JsonIgnore
    public String targetField;

    @JsonIgnore
    public boolean indexed;

    @JsonIgnore
    public Class<?> getExpectedBoxedType()
    {
        Class<?>       expectedClass = Reflection.getRawType(this.expectedType);
        TypeDescriptor td            = Reflection.getDescriptor(expectedClass);

        if (td != null && td.getBoxedType() != null)
        {
            expectedClass = td.getBoxedType();
        }

        return expectedClass;
    }

    public TimeSeriesPropertyType copy()
    {
        TimeSeriesPropertyType pt = new TimeSeriesPropertyType();
        pt.name              = name;
        pt.displayName       = displayName;
        pt.type              = type;
        pt.resolution        = resolution;
        pt.digitsOfPrecision = digitsOfPrecision;
        pt.isBoolean         = isBoolean;
        pt.unitsFactors      = unitsFactors;
        pt.values            = values;
        pt.debounceSeconds   = debounceSeconds;
        pt.noValueMarker     = noValueMarker;

        pt.expectedType = expectedType;
        pt.targetField  = targetField;
        pt.indexed      = indexed;
        return pt;
    }

    //--//

    public void setUnitsFactors(EngineeringUnitsFactors unitsFactors)
    {
        this.unitsFactors = unitsFactors;
    }

    public void setUnits(EngineeringUnits units)
    {
        if (units != EngineeringUnits.enumerated)
        {
            values = null;
        }

        this.unitsFactors = EngineeringUnitsFactors.get(units);
    }

    public boolean canConvert(EngineeringUnitsFactors unitsFactorsTo)
    {
        if (unitsFactorsTo == null)
        {
            return false;
        }

        return unitsFactorsTo.isEquivalent(unitsFactors);
    }

    @JsonIgnore
    public boolean couldInterpolate()
    {
        EngineeringUnits primaryUnits = unitsFactors != null ? unitsFactors.getPrimary() : null;
        if (primaryUnits != null)
        {
            EngineeringUnitsFamily family = primaryUnits.getFamily();
            if (family != null)
            {
                switch (family)
                {
                    case Enumerated:
                    case Binary:
                        return false;
                }
            }
        }

        return true;
    }

    public TimeSeriesEnumeratedValue addEnumValue()
    {
        if (values == null)
        {
            values = Lists.newArrayList();
        }

        TimeSeriesEnumeratedValue val = new TimeSeriesEnumeratedValue();
        values.add(val);
        return val;
    }

    public boolean tryToExtractEnumValues(Type type)
    {
        Class<? extends Enum<?>> clz = Reflection.getRawType(type);
        if (clz != null)
        {
            Enum<?>[] enumValues = clz.getEnumConstants();
            if (enumValues != null)
            {
                for (Enum<?> enumValue : enumValues)
                {
                    TimeSeriesEnumeratedValue newEnumValue = addEnumValue();
                    newEnumValue.typedValue = enumValue;

                    IEnumDescription enumDesc = Reflection.as(enumValue, IEnumDescription.class);
                    if (enumDesc != null)
                    {
                        newEnumValue.name = enumDesc.getDisplayName();
                    }
                    else
                    {
                        newEnumValue.name = enumValue.name();
                    }

                    TypedBitSet.ValueGetter enumValue2 = Reflection.as(enumValue, TypedBitSet.ValueGetter.class);
                    if (enumValue2 != null)
                    {
                        newEnumValue.value = enumValue2.getEncodingValue();
                    }
                    else
                    {
                        newEnumValue.value = enumValue.ordinal();
                    }
                }

                return true;
            }
        }

        return false;
    }

    public Object tryToResolveEnumValue(Object val)
    {
        if (values != null)
        {
            try
            {
                int valInt = Reflection.coerceNumber(val, Integer.class);

                for (TimeSeriesEnumeratedValue enumeratedValue : values)
                {
                    if (enumeratedValue.typedValue != null && enumeratedValue.value == valInt)
                    {
                        return enumeratedValue.typedValue;
                    }
                }
            }
            catch (Throwable t)
            {
                // Just in case the conversion is incompatible.
            }
        }

        return null;
    }
}
