/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = SensorResultForArgoHytos.class),
                @JsonSubTypes.Type(value = SensorResultForBergstrom.class),
                @JsonSubTypes.Type(value = SensorResultForBluesky.class),
                @JsonSubTypes.Type(value = SensorResultForEpSolar.class),
                @JsonSubTypes.Type(value = SensorResultForGps.class),
                @JsonSubTypes.Type(value = SensorResultForHendricksonWatchman.class),
                @JsonSubTypes.Type(value = SensorResultForHolykell.class),
                @JsonSubTypes.Type(value = SensorResultForI2CHub.class),
                @JsonSubTypes.Type(value = SensorResultForJ1939.class),
                @JsonSubTypes.Type(value = SensorResultForMontageBluetoothGateway.class),
                @JsonSubTypes.Type(value = SensorResultForPalfinger.class),
                @JsonSubTypes.Type(value = SensorResultForRawCANbus.class),
                @JsonSubTypes.Type(value = SensorResultForStealthPower.class),
                @JsonSubTypes.Type(value = SensorResultForTriStar.class),
                @JsonSubTypes.Type(value = SensorResultForVictron.class),
                @JsonSubTypes.Type(value = SensorResultForZeroRPM.class) })
public abstract class SensorResult
{
    public boolean success;
    public boolean portDetected;
    public String  failure;
}
