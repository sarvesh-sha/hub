/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.alerts.block.EngineStatementFromAlerts;
import com.optio3.cloud.hub.engine.core.block.EngineStatementFromCore;
import com.optio3.cloud.hub.engine.metrics.block.EngineStatementFromMetrics;
import com.optio3.cloud.hub.engine.normalizations.block.EngineStatementFromNormalization;

@JsonSubTypes({ @JsonSubTypes.Type(value = EngineStatementFromCore.class),
                @JsonSubTypes.Type(value = EngineStatementFromAlerts.class),
                @JsonSubTypes.Type(value = EngineStatementFromMetrics.class),
                @JsonSubTypes.Type(value = EngineStatementFromNormalization.class) })
public abstract class EngineStatement extends EngineBlock
{
}
