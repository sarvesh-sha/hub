/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionStep;
import com.optio3.cloud.hub.engine.metrics.MetricsEngineExecutionStep;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionStep;

@Optio3IncludeInApiDefinitions
@JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = AlertEngineExecutionStep.class),
                @JsonSubTypes.Type(value = MetricsEngineExecutionStep.class),
                @JsonSubTypes.Type(value = NormalizationEngineExecutionStep.class) })
@JsonTypeName("EngineExecutionStep")
public class EngineExecutionStep
{
    @JsonIgnore
    public int sequenceNumber;

    public String enteringBlockId;

    public String leavingBlockId;

    public EngineExecutionAssignment assignment;

    public String notImplemented;

    public String failure;

    @JsonIgnore
    public Throwable failureDetailed;
}
