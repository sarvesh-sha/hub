/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineStatement;

@JsonSubTypes({ @JsonSubTypes.Type(value = MetricsEngineStatementSetOutputToScalar.class),
                @JsonSubTypes.Type(value = MetricsEngineStatementSetOutputToSeries.class),
                @JsonSubTypes.Type(value = MetricsEngineStatementSetOutputToSeriesWithName.class) })
public abstract class EngineStatementFromMetrics extends EngineStatement
{
}
