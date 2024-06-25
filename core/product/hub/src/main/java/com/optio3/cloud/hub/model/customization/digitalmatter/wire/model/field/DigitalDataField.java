/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field;

import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseDataFieldModel;
import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned32;
import com.optio3.serialization.SerializationTag;

public class DigitalDataField extends BaseDataFieldModel
{
    @SerializationTag(number = 0)
    public Unsigned32 inputs;

    @SerializationTag(number = 4)
    public Unsigned16 outputs;

    @SerializationTag(number = 6, width = 1, bitOffset = 0)
    public boolean inTrip;

    @SerializationTag(number = 6, width = 1, bitOffset = 1)
    public boolean internalBatteryGood;

    @SerializationTag(number = 6, width = 1, bitOffset = 2)
    public boolean externalPowerGood;

    @SerializationTag(number = 6, width = 1, bitOffset = 3)
    public boolean connectedToGSM;

    @SerializationTag(number = 6, width = 1, bitOffset = 4)
    public boolean shuntingPowerFromBattery;

    @SerializationTag(number = 6, width = 1, bitOffset = 5)
    public boolean externalPowerEnabled;

    @SerializationTag(number = 6, width = 1, bitOffset = 6)
    public boolean tamperAlert;

    @SerializationTag(number = 6, width = 1, bitOffset = 7)
    public boolean recoveryModeActive;

    @SerializationTag(number = 6, width = 8, bitOffset = 8)
    public int flags;
}
