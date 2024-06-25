/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model.prober;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({ @JsonSubTypes.Type(value = ProberOperationForIpnToDecodedRead.class), @JsonSubTypes.Type(value = ProberOperationForIpnToObdiiRead.class) })
public abstract class ProberOperationForIpn extends ProberOperation
{
    @JsonSubTypes({ @JsonSubTypes.Type(value = ProberOperationForIpnToDecodedRead.Results.class), @JsonSubTypes.Type(value = ProberOperationForIpnToObdiiRead.Results.class) })
    public static abstract class BaseResults extends ProberOperation.BaseResults
    {
    }

    //--//

    public float accelerometerFrequency;
    public float accelerometerRange;
    public float accelerometerThreshold;

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

    public int samplingSeconds;
}
