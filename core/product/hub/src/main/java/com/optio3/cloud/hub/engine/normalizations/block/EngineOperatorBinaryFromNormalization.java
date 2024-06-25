/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.core.block.EngineOperatorBinary;

@JsonSubTypes({ @JsonSubTypes.Type(value = NormalizationEngineOperatorBinaryGetJaccardIndex.class),
                @JsonSubTypes.Type(value = NormalizationEngineOperatorBinaryFilterDocumentSet.class),
                @JsonSubTypes.Type(value = NormalizationEngineOperatorBinaryScoreDocuments.class),
                @JsonSubTypes.Type(value = NormalizationEngineOperatorBinaryScoreTopDocument.class) })
public abstract class EngineOperatorBinaryFromNormalization<To extends EngineValue, Ta extends EngineValue, Tb extends EngineValue> extends EngineOperatorBinary<To, Ta, Tb>
{
    protected EngineOperatorBinaryFromNormalization(Class<To> resultType)
    {
        super(resultType);
    }

    protected EngineOperatorBinaryFromNormalization(TypeReference<To> resultType)
    {
        super(resultType);
    }
}
