/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.core.block.EngineLiteral;

@JsonSubTypes({ @JsonSubTypes.Type(value = AlertEngineLiteralAlertSeverity.class),
                @JsonSubTypes.Type(value = AlertEngineLiteralAlertStatus.class),
                @JsonSubTypes.Type(value = AlertEngineLiteralAssetQueryEquipmentClass.class),
                @JsonSubTypes.Type(value = AlertEngineLiteralAssetQueryPointClass.class),
                @JsonSubTypes.Type(value = AlertEngineLiteralAssetQueryTag.class),
                @JsonSubTypes.Type(value = AlertEngineLiteralControlPoint.class),
                @JsonSubTypes.Type(value = AlertEngineLiteralControlPointsSelection.class),
                @JsonSubTypes.Type(value = AlertEngineLiteralDeliveryOptions.class) })
public abstract class EngineLiteralFromAlerts<T extends EngineValue> extends EngineLiteral<T>
{
    protected EngineLiteralFromAlerts(Class<T> resultType)
    {
        super(resultType);
    }
}
