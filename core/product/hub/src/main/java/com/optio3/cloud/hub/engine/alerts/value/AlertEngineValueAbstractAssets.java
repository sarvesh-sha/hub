/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.core.value.EngineValueListConcrete;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@JsonSubTypes({ @JsonSubTypes.Type(value = AlertEngineValueAssets.class), @JsonSubTypes.Type(value = AlertEngineValueControlPoints.class) })
public abstract class AlertEngineValueAbstractAssets<T extends AssetRecord> extends EngineValueListConcrete<AlertEngineValueAsset<T>>
{
    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        AlertEngineValueAbstractAssets<?> other = Reflection.as(o, AlertEngineValueAbstractAssets.class);
        if (other != null)
        {
            List<String> left  = toSortedList(this.elements);
            List<String> right = toSortedList(other.elements);

            int pos = 0;
            while (true)
            {
                String leftSysId  = CollectionUtils.getNthElement(left, pos);
                String rightSysId = CollectionUtils.getNthElement(right, pos);

                if (leftSysId == null)
                {
                    return rightSysId == null ? 0 : -1;
                }
                else if (rightSysId == null)
                {
                    return 1;
                }

                int diff = StringUtils.compare(leftSysId, rightSysId);
                if (diff != 0)
                {
                    return diff;
                }

                pos++;
            }
        }

        throw stack.unexpected();
    }

    private static <T extends AssetRecord> List<String> toSortedList(List<AlertEngineValueAsset<T>> input)
    {
        List<String> output = CollectionUtils.transformToList(input, (val) -> val.record.sysId);

        output.sort(String::compareTo);

        return output;
    }
}
