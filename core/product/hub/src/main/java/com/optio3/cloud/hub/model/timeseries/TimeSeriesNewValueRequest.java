/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.timeseries;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.logic.samples.SamplesCache;
import com.optio3.protocol.model.EngineeringUnitsFactors;

public class TimeSeriesNewValueRequest
{
    public static class Value
    {
        public ZonedDateTime           timestamp;
        public JsonNode                value;
        public EngineeringUnitsFactors convertFrom;
    }

    public       String      sysId;
    public       String      prop;
    public final List<Value> values = Lists.newArrayList();

    //--//

    public TimeSeriesNewValueResponse execute(SamplesCache samplesCache,
                                              SamplesCache.InjectorValidator validator) throws
                                                                                        Exception
    {
        TimeSeriesNewValueResponse res = new TimeSeriesNewValueResponse();

        var injector = samplesCache.buildSampleInjector(validator, sysId, prop);
        for (Value value : values)
        {
            if (injector != null && injector.inject(value.timestamp, value.value, value.convertFrom))
            {
                res.accepted++;
            }
            else
            {
                res.rejected++;
            }
        }

        if (injector != null)
        {
            injector.flush();
        }

        return res;
    }
}
