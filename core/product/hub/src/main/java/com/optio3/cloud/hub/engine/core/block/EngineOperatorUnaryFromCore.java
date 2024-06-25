/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineValue;

@JsonSubTypes({ @JsonSubTypes.Type(value = EngineOperatorUnaryAsList.class),
                @JsonSubTypes.Type(value = EngineOperatorUnaryDateTimeGetField.class),
                @JsonSubTypes.Type(value = EngineOperatorUnaryDateTimeRangeFromCurrentTime.class),
                @JsonSubTypes.Type(value = EngineOperatorUnaryIsNotNull.class),
                @JsonSubTypes.Type(value = EngineOperatorUnaryIsNotValidNumber.class),
                @JsonSubTypes.Type(value = EngineOperatorUnaryIsNull.class),
                @JsonSubTypes.Type(value = EngineOperatorUnaryIsEmpty.class),
                @JsonSubTypes.Type(value = EngineOperatorUnaryIsNotEmpty.class),
                @JsonSubTypes.Type(value = EngineOperatorUnaryIsValidNumber.class),
                @JsonSubTypes.Type(value = EngineOperatorUnaryListLength.class),
                @JsonSubTypes.Type(value = EngineOperatorUnaryLogicNot.class),
                @JsonSubTypes.Type(value = EngineOperatorUnaryRegexGetGroup.class),
                @JsonSubTypes.Type(value = EngineOperatorUnaryStringToLowerCase.class),
                @JsonSubTypes.Type(value = EngineOperatorUnaryStringToNumber.class),
                @JsonSubTypes.Type(value = EngineOperatorUnaryStringToUpperCase.class) })
public abstract class EngineOperatorUnaryFromCore<To extends EngineValue, Ti extends EngineValue> extends EngineOperatorUnary<To, Ti>
{
    protected EngineOperatorUnaryFromCore(Class<To> resultType)
    {
        super(resultType);
    }
}
