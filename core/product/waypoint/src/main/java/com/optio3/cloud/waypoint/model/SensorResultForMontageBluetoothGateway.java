/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SensorResultForMontageBluetoothGateway")
public class SensorResultForMontageBluetoothGateway extends SensorResult
{
    public boolean detectedHeartbeat;
    public boolean detectedPixelTag;
    public boolean detectedSmartLock;
    public boolean detectedTRH;
}
