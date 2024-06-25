/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.core.block.EngineOperatorUnary;

@JsonSubTypes({ @JsonSubTypes.Type(value = NormalizationEngineOperatorUnaryGetControllerProperty.class),
                @JsonSubTypes.Type(value = NormalizationEngineOperatorUnaryGetControllerPoints.class),
                @JsonSubTypes.Type(value = NormalizationEngineOperatorUnaryGetDocumentScore.class),
                @JsonSubTypes.Type(value = NormalizationEngineOperatorUnaryGetEquipmentClassId.class),
                @JsonSubTypes.Type(value = NormalizationEngineOperatorUnaryGetEquipmentClassName.class),
                @JsonSubTypes.Type(value = NormalizationEngineOperatorUnaryGetPointClassDescription.class),
                @JsonSubTypes.Type(value = NormalizationEngineOperatorUnaryGetPointClassName.class),
                @JsonSubTypes.Type(value = NormalizationEngineOperatorUnaryGetPointProperty.class),
                @JsonSubTypes.Type(value = NormalizationEngineOperatorUnaryNormalize.class),
                @JsonSubTypes.Type(value = NormalizationEngineOperatorUnaryTokenizeString.class) })
public abstract class EngineOperatorUnaryFromNormalization<To extends EngineValue, Ti extends EngineValue> extends EngineOperatorUnary<To, Ti>
{
    protected EngineOperatorUnaryFromNormalization(Class<To> resultType)
    {
        super(resultType);
    }

    protected EngineOperatorUnaryFromNormalization(TypeReference<To> resultType)
    {
        super(resultType);
    }
}
