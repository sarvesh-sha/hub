/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueDocument;
import com.optio3.cloud.hub.model.PointClassAssignment;
import com.optio3.protocol.model.EngineeringUnits;

@JsonTypeName("NormalizationEngineExecutionStepUnits")
public class NormalizationEngineExecutionStepUnits extends NormalizationEngineExecutionStep
{
    public EngineeringUnits units;
}
