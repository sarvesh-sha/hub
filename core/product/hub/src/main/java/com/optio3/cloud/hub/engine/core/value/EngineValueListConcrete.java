/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.value;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAbstractAssets;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueTravelLog;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSetOfSeries;
import com.optio3.util.CollectionUtils;

@JsonTypeName("EngineValueListConcrete")
@JsonSubTypes({ @JsonSubTypes.Type(value = AlertEngineValueAbstractAssets.class),
                @JsonSubTypes.Type(value = AlertEngineValueTravelLog.class),
                @JsonSubTypes.Type(value = EngineValueDateTimeList.class),
                @JsonSubTypes.Type(value = MetricsEngineValueSetOfSeries.class) })
public class EngineValueListConcrete<T extends EngineValue> extends EngineValueList<T>
{
    public static class Iterator<T extends EngineValue> extends EngineValueListIterator<T>
    {
        public List<T> elements;
        public int     cursor;

        @Override
        public boolean hasNext()
        {
            return cursor < CollectionUtils.size(elements);
        }

        @Override
        public T next()
        {
            return CollectionUtils.getNthElement(elements, cursor++);
        }
    }

    //--//

    public List<T> elements = Lists.newArrayList();

    @Override
    public int getLength()
    {
        return CollectionUtils.size(elements);
    }

    @Override
    public EngineValueListIterator<T> createIterator()
    {
        Iterator<T> it = new Iterator<T>();
        it.elements = elements;
        return it;
    }

    @Override
    public T getNthElement(int pos)
    {
        return CollectionUtils.getNthElement(elements, pos);
    }
}
