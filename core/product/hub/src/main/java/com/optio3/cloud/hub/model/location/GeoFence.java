/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.location;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = GeoFenceByRadius.class), @JsonSubTypes.Type(value = GeoFenceByPolygon.class) })
public abstract class GeoFence
{
    public String uniqueId;

    //--//

    public abstract void validate();
}
