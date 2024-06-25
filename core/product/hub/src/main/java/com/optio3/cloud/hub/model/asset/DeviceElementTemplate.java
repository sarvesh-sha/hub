/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.optio3.protocol.model.EngineeringUnits;

public class DeviceElementTemplate
{
    public String           name;
    public EngineeringUnits units;

    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    public List<String> enumeratedValues;
}

