/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineInputParameter;
import com.optio3.cloud.hub.engine.EngineValue;

@JsonSubTypes({ @JsonSubTypes.Type(value = EngineInputParameterBoolean.class),
                @JsonSubTypes.Type(value = EngineInputParameterDateTime.class),
                @JsonSubTypes.Type(value = EngineInputParameterDuration.class),
                @JsonSubTypes.Type(value = EngineInputParameterNumber.class),
                @JsonSubTypes.Type(value = EngineInputParameterString.class) })
public abstract class EngineInputParameterFromCore<T extends EngineValue> extends EngineInputParameter<T>
{
    protected EngineInputParameterFromCore(Class<T> resultType)
    {
        super(resultType);
    }
}
