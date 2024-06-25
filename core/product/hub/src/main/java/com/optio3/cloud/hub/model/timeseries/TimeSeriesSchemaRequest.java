/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.timeseries;

import java.util.Map;

import com.optio3.cloud.hub.logic.samples.SamplesCache;

public class TimeSeriesSchemaRequest
{
    public String sysId;

    //--//

    public TimeSeriesSchemaResponse fetch(SamplesCache cache)
    {
        Map<String, TimeSeriesPropertyType> properties = cache.extractClassification(sysId, true);
        if (properties == null)
        {
            return null;
        }

        TimeSeriesSchemaResponse result = new TimeSeriesSchemaResponse();
        result.properties = properties;

        return result;
    }
}
