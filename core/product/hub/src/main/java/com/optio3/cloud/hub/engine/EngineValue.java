/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAction;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlert;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlertSeverity;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlertStatus;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAsset;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAssetQueryCondition;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueControlPointCoordinates;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueDeliveryOptions;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueLocation;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueSample;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueTravelEntry;
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTime;
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTimeRange;
import com.optio3.cloud.hub.engine.core.value.EngineValueDuration;
import com.optio3.cloud.hub.engine.core.value.EngineValueEngineeringUnits;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValueListIterator;
import com.optio3.cloud.hub.engine.core.value.EngineValueLookupTable;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitive;
import com.optio3.cloud.hub.engine.core.value.EngineValueRegexMatch;
import com.optio3.cloud.hub.engine.core.value.EngineValueRegexReplaceTable;
import com.optio3.cloud.hub.engine.core.value.EngineValueTimeZone;
import com.optio3.cloud.hub.engine.core.value.EngineValueWeeklySchedule;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineSelectValue;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValue;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueController;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueDocument;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueEquipment;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueLocation;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValuePoint;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = AlertEngineValueAction.class),
                @JsonSubTypes.Type(value = AlertEngineValueAlert.class),
                @JsonSubTypes.Type(value = AlertEngineValueAlertSeverity.class),
                @JsonSubTypes.Type(value = AlertEngineValueAlertStatus.class),
                @JsonSubTypes.Type(value = AlertEngineValueAsset.class),
                @JsonSubTypes.Type(value = AlertEngineValueAssetQueryCondition.class),
                @JsonSubTypes.Type(value = AlertEngineValueControlPointCoordinates.class),
                @JsonSubTypes.Type(value = AlertEngineValueDeliveryOptions.class),
                @JsonSubTypes.Type(value = AlertEngineValueLocation.class),
                @JsonSubTypes.Type(value = AlertEngineValueSample.class),
                @JsonSubTypes.Type(value = AlertEngineValueTravelEntry.class),
                @JsonSubTypes.Type(value = EngineValueDateTime.class),
                @JsonSubTypes.Type(value = EngineValueDateTimeRange.class),
                @JsonSubTypes.Type(value = EngineValueDuration.class),
                @JsonSubTypes.Type(value = EngineValueEngineeringUnits.class),
                @JsonSubTypes.Type(value = EngineValueList.class),
                @JsonSubTypes.Type(value = EngineValueListIterator.class),
                @JsonSubTypes.Type(value = EngineValueLookupTable.class),
                @JsonSubTypes.Type(value = EngineValuePrimitive.class),
                @JsonSubTypes.Type(value = EngineValueRegexMatch.class),
                @JsonSubTypes.Type(value = EngineValueRegexReplaceTable.class),
                @JsonSubTypes.Type(value = EngineValueTimeZone.class),
                @JsonSubTypes.Type(value = EngineValueWeeklySchedule.class),
                @JsonSubTypes.Type(value = MetricsEngineSelectValue.class),
                @JsonSubTypes.Type(value = MetricsEngineValue.class),
                @JsonSubTypes.Type(value = NormalizationEngineValueController.class),
                @JsonSubTypes.Type(value = NormalizationEngineValueDocument.class),
                @JsonSubTypes.Type(value = NormalizationEngineValueEquipment.class),
                @JsonSubTypes.Type(value = NormalizationEngineValueLocation.class),
                @JsonSubTypes.Type(value = NormalizationEngineValuePoint.class) })
public abstract class EngineValue
{
    public abstract int compareTo(EngineExecutionContext<?, ?> ctx,
                                  EngineExecutionStack stack,
                                  EngineValue o);

    public abstract String format(EngineExecutionContext<?, ?> ctx,
                                  EngineExecutionStack stack,
                                  Map<String, String> modifiers);

    public static boolean equals(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack,
                                 EngineValue val1,
                                 EngineValue val2)
    {
        if (val1 == null || val2 == null)
        {
            return val1 == val2;
        }

        return val1.compareTo(ctx, stack, val2) == 0;
    }

    public static Boolean isEmpty(EngineValue val)
    {
        if (val == null)
        {
            return true;
        }

        EngineValueList<?> lst = Reflection.as(val, EngineValueList.class);
        if (lst != null)
        {
            return lst.getLength() == 0;
        }

        EngineValuePrimitive prim = Reflection.as(val, EngineValuePrimitive.class);
        if (prim != null)
        {
            return StringUtils.isEmpty(prim.asString());
        }

        return null;
    }
}
