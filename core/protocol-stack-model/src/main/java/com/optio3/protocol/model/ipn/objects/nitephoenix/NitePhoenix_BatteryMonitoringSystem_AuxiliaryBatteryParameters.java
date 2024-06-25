/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.nitephoenix;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.can.CanExtendedMessageType;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Can:NitePhoenix_BatteryMonitoringSystem_AuxiliaryBatteryParameters")
@CanExtendedMessageType(priority = 6, dataPage = false, pduFormat = 0xFF, destinationAddress = 0x00, sourceAddress = 0x47, littleEndian = true)
@CanExtendedMessageType(priority = 6, dataPage = false, pduFormat = 0xFF, destinationAddress = 0x00, sourceAddress = 0x48, littleEndian = true)
@CanExtendedMessageType(priority = 6, dataPage = false, pduFormat = 0xFF, destinationAddress = 0x00, sourceAddress = 0x49, littleEndian = true)
@CanExtendedMessageType(priority = 6, dataPage = false, pduFormat = 0xFF, destinationAddress = 0x00, sourceAddress = 0x4A, littleEndian = true)
public class NitePhoenix_BatteryMonitoringSystem_AuxiliaryBatteryParameters extends BaseBatteryNitePhoenixModel
{
    @FieldModelDescription(description = "Aux Battery Current", units = EngineeringUnits.amperes, debounceSeconds = 15, pointClass = WellKnownPointClass.BatteryCurrent)
    @SerializationTag(number = 0, width = 32, scaling = { @SerializationScaling(scalingFactor = 0.1, postScalingOffset = -300, assumeUnsigned = true) })
    public float current;

    @FieldModelDescription(description = "Aux Battery Voltage", units = EngineeringUnits.volts, debounceSeconds = 15, pointClass = WellKnownPointClass.BatteryVoltage)
    @SerializationTag(number = 4, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.01, assumeUnsigned = true) })
    public float voltage;

    @FieldModelDescription(description = "Aux Battery Temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, pointClass = WellKnownPointClass.BatteryTemperature)
    @SerializationTag(number = 6, width = 16, scaling = { @SerializationScaling(preScalingOffset = -40, assumeUnsigned = true) })
    public float temperature;

    //--//

    @Override
    public String extractBaseId()
    {
        return "NitePhoenix_BMS_AuxiliaryBatteryParameters";
    }
}
