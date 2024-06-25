/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlert;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlertSeverity;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlertStatus;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueControlPoint;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueControlPoints;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueDeliveryOptions;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueSample;
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTime;
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTimeRange;
import com.optio3.cloud.hub.engine.core.value.EngineValueDuration;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveBoolean;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveNumber;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.core.value.EngineValueRegexMatch;

public class EngineVariable extends EngineVariableReference
{
    public String type;

    // TODO: UPGRADE PATCH: Remove after deployed to production.
    public void setType(String type)
    {
        if (type != null)
        {
            switch (type)
            {
                case "Boolean":
                    type = getName(EngineValuePrimitiveBoolean.class);
                    break;

                case "Number,":
                    type = getName(EngineValuePrimitiveNumber.class);
                    break;

                case "String,":
                    type = getName(EngineValuePrimitiveString.class);
                    break;

                case "List":
                    type = getName(EngineValueList.class);
                    break;

                case "RegexMatch":
                    type = getName(EngineValueRegexMatch.class);
                    break;

                case "DateTime":
                    type = getName(EngineValueDateTime.class);
                    break;

                case "DateTimeRange":
                    type = getName(EngineValueDateTimeRange.class);
                    break;

                case "Duration":
                    type = getName(EngineValueDuration.class);
                    break;

                case "Alert":
                    type = getName(AlertEngineValueAlert.class);
                    break;

                case "AlertSeverity":
                    type = getName(AlertEngineValueAlertSeverity.class);
                    break;

                case "AlertStatus":
                    type = getName(AlertEngineValueAlertStatus.class);
                    break;

                case "DeliveryOptions":
                    type = getName(AlertEngineValueDeliveryOptions.class);
                    break;

                case "ControlPoint":
                    type = getName(AlertEngineValueControlPoint.class);
                    break;

                case "ControlPointSelection":
                    type = getName(AlertEngineValueControlPoints.class);
                    break;

                case "Sample":
                    type = getName(AlertEngineValueSample.class);
                    break;

                default:
                    this.type = type;
                    break;
            }
        }

        HubApplication.reportPatchCall(type);

        this.type = type;
    }

    private static String getName(Class<?> clz)
    {
        return clz.getAnnotation(JsonTypeName.class)
                  .value();
    }
}
