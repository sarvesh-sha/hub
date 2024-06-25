/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.EngineInputParameter;
import com.optio3.cloud.hub.engine.EngineValue;

@JsonSubTypes({ @JsonSubTypes.Type(value = EngineExpressionCurrentDateTime.class),
                @JsonSubTypes.Type(value = EngineExpressionFormatText.class),
                @JsonSubTypes.Type(value = EngineExpressionFunctionCall.class),
                @JsonSubTypes.Type(value = EngineExpressionGetVariable.class),
                @JsonSubTypes.Type(value = EngineExpressionMemoize.class),
                @JsonSubTypes.Type(value = EngineExpressionRangeCheck.class),
                @JsonSubTypes.Type(value = EngineInputParameter.class),
                @JsonSubTypes.Type(value = EngineLiteral.class),
                @JsonSubTypes.Type(value = EngineOperatorBinary.class),
                @JsonSubTypes.Type(value = EngineOperatorUnary.class) })
public abstract class EngineExpressionFromCore<T extends EngineValue> extends EngineExpression<T>
{
    protected EngineExpressionFromCore(Class<T> resultType)
    {
        super(resultType);
    }

    protected EngineExpressionFromCore(TypeReference<T> resultType)
    {
        super(resultType);
    }
}
