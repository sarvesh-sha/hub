package com.optio3.protocol.model.config;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.WellKnownPointClassOrCustom;

@JsonTypeName("I2CSensor_MCP3428")
public class I2CSensor_MCP3428 extends I2CSensor
{
    public int channel;
    public int gain;

    public float                       conversionScale;
    public float                       conversionOffsetPre;
    public float                       conversionOffsetPost;
    public WellKnownPointClassOrCustom pointClass;
}
