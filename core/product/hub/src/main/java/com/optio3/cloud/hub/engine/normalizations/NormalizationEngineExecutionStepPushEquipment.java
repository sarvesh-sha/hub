/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueEquipment;

@JsonTypeName("NormalizationEngineExecutionStepPushEquipment")
public class NormalizationEngineExecutionStepPushEquipment extends NormalizationEngineExecutionStep
{
    public NormalizationEngineValueEquipment parentEquipment;

    public NormalizationEngineValueEquipment equipment;
}
