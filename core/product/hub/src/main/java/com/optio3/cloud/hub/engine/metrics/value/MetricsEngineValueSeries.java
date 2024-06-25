/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.value;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesExtract;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyResponse;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsConverter;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.serialization.Reflection;

@JsonTypeName("MetricsEngineValueSeries")
public class MetricsEngineValueSeries extends MetricsEngineValue
{
    public final TimeSeriesPropertyResponse values;

    //--//

    @JsonCreator
    public MetricsEngineValueSeries(@JsonProperty("values") TimeSeriesPropertyResponse values)
    {
        this.values = values;
    }

    //--//

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        throw stack.unexpected();
    }

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        return null;
    }

    //--//

    public MetricsEngineValueSeries copy()
    {
        return new MetricsEngineValueSeries(values.copy());
    }

    @JsonIgnore
    public void setUnitsFactors(EngineeringUnits units)
    {
        values.setUnits(units);
    }

    @JsonIgnore
    public void setUnits(EngineeringUnitsFactors units)
    {
        values.setUnitsFactors(units);
    }

    @JsonIgnore
    public EngineeringUnitsFactors getUnitsFactors()
    {
        return values.getUnitsFactors();
    }

    @JsonIgnore
    public boolean canConvert(EngineeringUnitsFactors unitsFactorsTo)
    {
        return values.propertySchema.canConvert(unitsFactorsTo);
    }

    public MetricsEngineValueSeries convert(EngineeringUnitsFactors unitsFactorsTo)
    {
        EngineeringUnitsFactors units = getUnitsFactors();

        if (unitsFactorsTo != null && unitsFactorsTo != units && unitsFactorsTo.isEquivalent(units))
        {
            MetricsEngineValueSeries converted = copy();
            double[]                 array     = converted.values.values;

            EngineeringUnitsConverter converter = EngineeringUnits.buildConverter(units, unitsFactorsTo);

            for (int i = 0; i < array.length; i++)
            {
                array[i] = converter.convert(array[i]);
            }

            converted.setUnits(unitsFactorsTo);

            return converted;
        }

        return this;
    }

    public void toExtract(TimeSeriesExtract<?> extract)
    {
        double[] timestamps = this.values.timestamps;
        double[] values     = this.values.values;
        int      count      = timestamps.length;

        extract.timeZone = this.values.timeZone;

        extract.prepareForGrowth(count);

        try (var batch = extract.prepareBatch())
        {
            String[] enumLookup = this.values.enumLookup;
            if (enumLookup != null)
            {
                for (int cursor = 0; cursor < count; )
                {
                    int batchSize = Math.min(count - cursor, batch.length);

                    System.arraycopy(timestamps, cursor, batch.tempTimestamps, 0, batchSize);

                    for (int i = 0; i < batchSize; i++)
                    {
                        double value = values[cursor + i];

                        if (!Double.isNaN(value))
                        {
                            int index = (int) value;

                            if (index >= 0 && index < enumLookup.length)
                            {
                                value = extract.addEnumValue(enumLookup[(int) value]);
                            }
                            else
                            {
                                value = Double.NaN;
                            }
                        }

                        batch.tempValues[i] = value;
                    }

                    batch.addRange(0, batchSize);
                    cursor += batchSize;
                }
            }
            else
            {
                for (int cursor = 0; cursor < count; )
                {
                    int batchSize = Math.min(count - cursor, batch.length);

                    System.arraycopy(timestamps, cursor, batch.tempTimestamps, 0, batchSize);
                    System.arraycopy(values, cursor, batch.tempValues, 0, batchSize);

                    batch.addRange(0, batchSize);
                    cursor += batchSize;
                }
            }
        }
    }

    public TimeSeries.NumericValueRanges toRanges()
    {
        try (TimeSeriesExtract<?> extract = new TimeSeriesExtract<>(values.expectedType))
        {
            toExtract(extract);

            TimeSeries.NumericValueRanges summary = new TimeSeries.NumericValueRanges();
            summary.importValues(extract);
            return summary;
        }
    }

    public <T> T fetchNthValue(int pos,
                               Class<T> clz)
    {
        double[] samples = this.values.values;

        if (pos >= 0 && pos < samples.length)
        {
            double value = samples[pos];
            if (clz == Double.class)
            {
                return clz.cast(value);
            }

            return Reflection.coerceNumber(value, clz);
        }

        return null;
    }
}
