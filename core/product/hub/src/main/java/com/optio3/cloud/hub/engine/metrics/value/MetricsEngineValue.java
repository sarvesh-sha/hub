/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.value;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineValue;

@JsonSubTypes({ @JsonSubTypes.Type(value = MetricsEngineValueScalar.class), @JsonSubTypes.Type(value = MetricsEngineValueSeries.class) })
public abstract class MetricsEngineValue extends EngineValue
{
}
