/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.metrics;

import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.serialization.Reflection;

public class MetricsBindingForSeries
{
    public TypedRecordIdentity<DeviceElementRecord> record;

    //--//

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        MetricsBindingForSeries that = Reflection.as(o, MetricsBindingForSeries.class);
        if (that == null)
        {
            return false;
        }

        return TypedRecordIdentity.sameRecord(record, that.record);
    }

    @Override
    public int hashCode()
    {
        if (record == null)
        {
            return 0;
        }

        return record.hashCode();
    }
}
