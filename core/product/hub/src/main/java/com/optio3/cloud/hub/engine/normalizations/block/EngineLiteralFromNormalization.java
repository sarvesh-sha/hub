/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.core.block.EngineLiteral;

@JsonSubTypes({ @JsonSubTypes.Type(value = NormalizationEngineLiteralDocumentSet.class),
                @JsonSubTypes.Type(value = NormalizationEngineLiteralEquipmentClass.class) })
public abstract class EngineLiteralFromNormalization<T extends EngineValue> extends EngineLiteral<T>
{
    protected EngineLiteralFromNormalization(Class<T> resultType)
    {
        super(resultType);
    }

    protected EngineLiteralFromNormalization(TypeReference<T> resultType)
    {
        super(resultType);
    }
}
