/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueDocument;
import com.optio3.cloud.hub.model.PointClassAssignment;

@JsonTypeName("NormalizationEngineExecutionStepPointClassification")
public class NormalizationEngineExecutionStepPointClassification extends NormalizationEngineExecutionStep
{
    public String pointClassId;

    public PointClassAssignment classificationAssignment;

    public NormalizationEngineValueDocument classificationDocument;
}
