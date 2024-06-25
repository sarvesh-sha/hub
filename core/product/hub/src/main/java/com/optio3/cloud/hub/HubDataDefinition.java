/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HubDataDefinition
{
    public String  resource;
    public boolean isDemo;
    public boolean loadIfMissing;

    //--//

    @JsonCreator
    public HubDataDefinition(@JsonProperty(value = "resource", required = true) String resource,
                             @JsonProperty(value = "isDemo", required = false) boolean isDemo,
                             @JsonProperty(value = "loadIfMissing", required = false) boolean loadIfMissing)
    {
        this.resource = resource;
        this.isDemo = isDemo;
        this.loadIfMissing = loadIfMissing;
    }
}
