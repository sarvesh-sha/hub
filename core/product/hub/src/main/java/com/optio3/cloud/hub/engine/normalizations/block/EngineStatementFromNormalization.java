/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineStatement;

@JsonSubTypes({ @JsonSubTypes.Type(value = NormalizationEngineStatementClearEquipment.class),
                @JsonSubTypes.Type(value = NormalizationEngineStatementPushEquipmentWithClass.class),
                @JsonSubTypes.Type(value = NormalizationEngineStatementPushEquipmentLocation.class),
                @JsonSubTypes.Type(value = NormalizationEngineStatementPushLocation.class),
                @JsonSubTypes.Type(value = NormalizationEngineStatementSetEngineeringUnits.class),
                @JsonSubTypes.Type(value = NormalizationEngineStatementSetEquipment.class),
                @JsonSubTypes.Type(value = NormalizationEngineStatementSetEquipmentAndClassHint.class),
                @JsonSubTypes.Type(value = NormalizationEngineStatementSetEquipmentClassTable.class),
                @JsonSubTypes.Type(value = NormalizationEngineStatementSetMetadata.class),
                @JsonSubTypes.Type(value = NormalizationEngineStatementSetOutputValue.class),
                @JsonSubTypes.Type(value = NormalizationEngineStatementSetPointClass.class),
                @JsonSubTypes.Type(value = NormalizationEngineStatementSetPointClassFromDocument.class),
                @JsonSubTypes.Type(value = NormalizationEngineStatementSetPointClassFromTermScoring.class),
                @JsonSubTypes.Type(value = NormalizationEngineStatementSetPointClassTable.class),
                @JsonSubTypes.Type(value = NormalizationEngineStatementSetSamplingPeriod.class),
                @JsonSubTypes.Type(value = NormalizationEngineStatementSetTags.class) })
public abstract class EngineStatementFromNormalization extends EngineStatement
{
}
