/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.optio3.protocol.model.WellKnownEquipmentClassOrCustom;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = I2CSensor_SHT3x.class), @JsonSubTypes.Type(value = I2CSensor_MCP3428.class) })
public abstract class I2CSensor
{
    public int   bus;
    public float samplingPeriod;
    public int   averagingSamples;

    public WellKnownEquipmentClassOrCustom equipmentClass;
    public String                          instanceSelector;
}
