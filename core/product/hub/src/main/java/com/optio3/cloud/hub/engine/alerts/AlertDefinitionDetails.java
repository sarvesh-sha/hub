/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.optio3.cloud.hub.engine.EngineDefinitionDetails;
import com.optio3.cloud.hub.model.asset.graph.AssetGraph;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = AlertDefinitionDetailsForUserProgram.class) })
public abstract class AlertDefinitionDetails extends EngineDefinitionDetails
{
    public boolean temporary;

    public AssetGraph graph;
}
