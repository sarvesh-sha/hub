/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineInputParameter;
import com.optio3.cloud.hub.engine.EngineValue;

@JsonSubTypes({ @JsonSubTypes.Type(value = AlertEngineInputParameterControlPoint.class),
                @JsonSubTypes.Type(value = AlertEngineInputParameterControlPointsSelection.class),
                @JsonSubTypes.Type(value = AlertEngineInputParameterDeliveryOptions.class) })
public abstract class EngineInputParameterFromAlerts<T extends EngineValue> extends EngineInputParameter<T>
{
    protected EngineInputParameterFromAlerts(Class<T> resultType)
    {
        super(resultType);
    }
}
