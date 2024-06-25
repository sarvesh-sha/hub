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
import com.optio3.protocol.model.ipn.objects.nitephoenix.enums.HeaterOption;
import com.optio3.protocol.model.ipn.objects.nitephoenix.enums.OperatingMode;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Can:NitePhoenix_HVAC_Unit")
@CanExtendedMessageType(priority = 6, dataPage = false, pduFormat = 0xFF, destinationAddress = 0x3D, sourceAddress = 0x44, littleEndian = false)
public class NitePhoenix_HVAC_Unit extends BaseNitePhoenixModel
{
    @FieldModelDescription(description = "Relay Outputs", units = EngineeringUnits.ticks)
    @SerializationTag(number = 0, width = 8)
    public int relayOutputs;

    @FieldModelDescription(description = "Control Mode", pointClass = WellKnownPointClass.HvacOperatingMode, units = EngineeringUnits.enumerated)
    @SerializationTag(number = 1, width = 2, bitOffset = 0)
    public OperatingMode controlMode;

    @FieldModelDescription(description = "Heater Option", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 1, width = 2, bitOffset = 2)
    public HeaterOption heaterOption;

    @FieldModelDescription(description = "Evap sensor shorted high or missing", units = EngineeringUnits.activeInactive)
    @SerializationTag(number = 2, width = 1, bitOffset = 0)
    public boolean evapSensorShortedHighOrMissing;

    @FieldModelDescription(description = "Evap sensor shorted low", units = EngineeringUnits.activeInactive)
    @SerializationTag(number = 2, width = 1, bitOffset = 1)
    public boolean evapSensorShortedLow;

    @FieldModelDescription(description = "High pressure switch open", units = EngineeringUnits.activeInactive)
    @SerializationTag(number = 2, width = 1, bitOffset = 2)
    public boolean highPressureSwitchOpen;

    @FieldModelDescription(description = "Sleeper temperature", pointClass = WellKnownPointClass.HvacTemperature, units = EngineeringUnits.degrees_celsius)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1, postScalingOffset = -26.2, assumeUnsigned = true) })
    public float sleeperTemperature;

    @FieldModelDescription(description = "Set temperature", units = EngineeringUnits.degrees_celsius)
    @SerializationTag(number = 5, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1, postScalingOffset = -26.3, assumeUnsigned = true) })
    public float setTemperature;

    @FieldModelDescription(description = "Compressor Speed", pointClass = WellKnownPointClass.HvacCompressorSpeed, units = EngineeringUnits.ticks)
    @SerializationTag(number = 7, width = 8, scaling = { @SerializationScaling(scalingFactor = 1.0, assumeUnsigned = true) })
    public float compressorSpeed;

    //--//

    @Override
    public String extractBaseId()
    {
        return "NitePhoenix_HVAC_Unit";
    }
}
