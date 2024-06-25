/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import java.util.BitSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Sets;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValueListIterator;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.protocol.model.EngineeringUnits;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("MetricsEngineOperatorThresholdEnum")
public class MetricsEngineOperatorThresholdEnum extends EngineOperatorBinaryFromMetrics<MetricsEngineValueSeries, MetricsEngineValueSeries, EngineValueList<EngineValuePrimitiveString>>
{
    public MetricsEngineOperatorThresholdEnum()
    {
        super(MetricsEngineValueSeries.class);
    }

    @Override
    protected MetricsEngineValueSeries computeResult(EngineExecutionContext<?, ?> ctx,
                                                     EngineExecutionStack stack,
                                                     MetricsEngineValueSeries series,
                                                     EngineValueList<EngineValuePrimitiveString> enums)
    {
        stack.checkNonNullValue(series, "Missing left parameter");
        stack.checkNonNullValue(enums, "Missing right parameter");

        Set<String> enumValues = Sets.newHashSet();

        for (EngineValueListIterator<EngineValuePrimitiveString> it = enums.createIterator(); it.hasNext(); )
        {
            String val = EngineValuePrimitiveString.extract(it.next());
            if (StringUtils.isNotBlank(val))
            {
                enumValues.add(val);
            }
        }

        MetricsEngineValueSeries res = series.copy();
        res.setUnitsFactors(EngineeringUnits.activeInactive);

        double[] inputs  = series.values.values;
        double[] outputs = res.values.values;

        BitSet bs = new BitSet();

        if (series.values.enumLookup != null)
        {
            for (int index = 0; index < series.values.enumLookup.length; index++)
            {
                String val = series.values.enumLookup[index];

                if (enumValues.contains(val))
                {
                    bs.set(index);
                }
            }
        }

        if (series.values.enumSetLookup != null)
        {
            for (int index = 0; index < series.values.enumSetLookup.length; index++)
            {
                String[] vals = series.values.enumSetLookup[index];

                for (String val : vals)
                {
                    if (enumValues.contains(val))
                    {
                        bs.set(index);
                        break;
                    }
                }
            }
        }

        for (int i = 0; i < inputs.length; i++)
        {
            double value = inputs[i];

            outputs[i] = !Double.isNaN(value) && bs.get((int) value) ? 1.0 : 0.0;
        }

        return res;
    }
}

