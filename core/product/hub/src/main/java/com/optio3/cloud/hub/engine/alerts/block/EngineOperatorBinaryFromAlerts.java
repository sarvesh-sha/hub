/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.core.block.EngineOperatorBinary;

@JsonSubTypes({ @JsonSubTypes.Type(value = AlertEngineExpressionBinaryControlPointSample.class),
                @JsonSubTypes.Type(value = AlertEngineExpressionBinaryControlPointSampleAggregate.class),
                @JsonSubTypes.Type(value = AlertEngineExpressionBinaryControlPointSampleRange.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorBinaryAssetQueryAnd.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorBinaryAssetQueryOr.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorBinaryAssetQueryRelation.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorBinaryAssetQueryRelationSingle.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorBinaryForDeliveryOptions.class) })
public abstract class EngineOperatorBinaryFromAlerts<To extends EngineValue, Ta extends EngineValue, Tb extends EngineValue> extends EngineOperatorBinary<To, Ta, Tb>
{
    protected EngineOperatorBinaryFromAlerts(Class<To> resultType)
    {
        super(resultType);
    }
}
