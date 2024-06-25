/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValueListIterator;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.util.CollectionUtils;

@JsonTypeName("AlertEngineValueSamples")
public class AlertEngineValueSamples extends EngineValueList<AlertEngineValueSample>
{
    public static class Iterator extends EngineValueListIterator<AlertEngineValueSample>
    {
        public AlertEngineValueSamples source;
        public int                     cursor;

        @Override
        public boolean hasNext()
        {
            return cursor < CollectionUtils.size(source.timestamps);
        }

        @Override
        public AlertEngineValueSample next()
        {
            return source.getNthElement(cursor++);
        }
    }

    //--//

    public TypedRecordIdentity<DeviceElementRecord> controlPoint;
    public List<ZonedDateTime>                      timestamps;

    @Override
    public int getLength()
    {
        return CollectionUtils.size(timestamps);
    }

    @Override
    public EngineValueListIterator<AlertEngineValueSample> createIterator()
    {
        Iterator it = new Iterator();
        it.source = this;
        return it;
    }

    @Override
    public AlertEngineValueSample getNthElement(int pos)
    {
        ZonedDateTime timestamp = CollectionUtils.getNthElement(timestamps, pos);
        if (timestamp == null)
        {
            return null;
        }

        AlertEngineValueSample res = new AlertEngineValueSample();
        res.controlPoint = controlPoint;
        res.timestamp    = timestamp;
        return res;
    }
}
