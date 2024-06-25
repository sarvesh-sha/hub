/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.hub.engine.EngineValue;

@JsonSubTypes({ @JsonSubTypes.Type(value = EngineLiteralBoolean.class),
                @JsonSubTypes.Type(value = EngineLiteralDateTime.class),
                @JsonSubTypes.Type(value = EngineLiteralDuration.class),
                @JsonSubTypes.Type(value = EngineLiteralEngineeringUnits.class),
                @JsonSubTypes.Type(value = EngineLiteralList.class),
                @JsonSubTypes.Type(value = EngineLiteralLookupTable.class),
                @JsonSubTypes.Type(value = EngineLiteralNull.class),
                @JsonSubTypes.Type(value = EngineLiteralNumber.class),
                @JsonSubTypes.Type(value = EngineLiteralRegexReplaceTable.class),
                @JsonSubTypes.Type(value = EngineLiteralString.class),
                @JsonSubTypes.Type(value = EngineLiteralStringSet.class),
                @JsonSubTypes.Type(value = EngineLiteralTimeZone.class),
                @JsonSubTypes.Type(value = EngineLiteralWeeklySchedule.class) })
public abstract class EngineLiteralFromCore<T extends EngineValue> extends EngineLiteral<T>
{
    protected EngineLiteralFromCore(Class<T> resultType)
    {
        super(resultType);
    }

    protected EngineLiteralFromCore(TypeReference<T> resultType)
    {
        super(resultType);
    }
}
