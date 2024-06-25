/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionStep;

@JsonSubTypes({ @JsonSubTypes.Type(value = NormalizationEngineExecutionStepEquipmentClassification.class),
                @JsonSubTypes.Type(value = NormalizationEngineExecutionStepPointClassification.class),
                @JsonSubTypes.Type(value = NormalizationEngineExecutionStepPushEquipment.class),
                @JsonSubTypes.Type(value = NormalizationEngineExecutionStepUnits.class) })
@JsonTypeName("NormalizationEngineExecutionStep")
public class NormalizationEngineExecutionStep extends EngineExecutionStep
{
}
