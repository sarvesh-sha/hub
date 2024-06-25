/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.nitephoenix;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.can.CanExtendedMessageType;
import com.optio3.protocol.model.ipn.objects.nitephoenix.enums.SeparatorRelayStatus;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Can:NitePhoenix_BatteryMonitoringSystem_StartingBatteryParameters")
@CanExtendedMessageType(priority = 6, dataPage = false, pduFormat = 0xFF, destinationAddress = 0x01, sourceAddress = 0x47, littleEndian = true)
@CanExtendedMessageType(priority = 6, dataPage = false, pduFormat = 0xFF, destinationAddress = 0x01, sourceAddress = 0x48, littleEndian = true)
@CanExtendedMessageType(priority = 6, dataPage = false, pduFormat = 0xFF, destinationAddress = 0x01, sourceAddress = 0x49, littleEndian = true)
@CanExtendedMessageType(priority = 6, dataPage = false, pduFormat = 0xFF, destinationAddress = 0x01, sourceAddress = 0x4A, littleEndian = true)
public class NitePhoenix_BatteryMonitoringSystem_StartingBatteryParameters extends BaseBatteryNitePhoenixModel
{
    @FieldModelDescription(description = "Separator Relay Status", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 0, width = 1)
    public SeparatorRelayStatus status;

    @JsonIgnore
    @SerializationTag(number = 1, width = 24)
    public int reserved1;

    @FieldModelDescription(description = "Starting Battery Voltage", units = EngineeringUnits.volts, debounceSeconds = 15)
    @SerializationTag(number = 4, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.01, assumeUnsigned = true) })
    public float voltage;

    //--//

    @Override
    public String extractBaseId()
    {
        return "NitePhoenix_BMS_StartingBatteryParameters";
    }
}
