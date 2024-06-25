/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.serialization.Reflection;

@JsonTypeName("ProtocolConfigForIpn")
public class ProtocolConfigForIpn extends ProtocolConfig
{
    public float accelerometerFrequency;
    public float accelerometerRange;
    public float accelerometerThreshold;

    public final List<I2CSensor> i2cSensors = Lists.newArrayList();

    public String  canPort;
    public int     canFrequency;
    public boolean canNoTermination;
    public boolean canInvert;

    public String  epsolarPort;
    public boolean epsolarInvert;

    public String gpsPort;

    public String  holykellPort;
    public boolean holykellInvert;

    public String  ipnPort;
    public int     ipnBaudrate;
    public boolean ipnInvert;

    public String  obdiiPort;
    public int     obdiiFrequency;
    public boolean obdiiInvert;

    public String argohytosPort;

    public String stealthpowerPort;

    public String tristarPort;

    public String victronPort;

    public String montageBluetoothGatewayPort;

    //--//

    public boolean simulate;

    public void setFlushThreshold(int val)
    {
        // Ignore legacy property.
    }

    // legacy property.
    public void setSerialPort(String val)
    {
        ipnPort = val;
    }

    //--//

    @Override
    public boolean equals(ProtocolConfig other)
    {
        ProtocolConfigForIpn o = Reflection.as(other, ProtocolConfigForIpn.class);
        if (o == null)
        {
            return false;
        }

        return equalsThroughJson(other);
    }
}
