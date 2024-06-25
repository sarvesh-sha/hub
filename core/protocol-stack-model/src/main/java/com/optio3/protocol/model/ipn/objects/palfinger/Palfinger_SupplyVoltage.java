/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.palfinger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.can.CanMessageType;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Can:Palfinger_SupplyVoltage")
@CanMessageType(sourceAddress = 0x688, littleEndian = false)
public class Palfinger_SupplyVoltage extends BasePalfingerModel
{
    @FieldModelDescription(description = "Battery Voltage", units = EngineeringUnits.millivolts, pointClass = WellKnownPointClass.BatteryVoltage, debounceSeconds = 5, minimumDelta = 50.0)
    @SerializationTag(number = 0, width = 16, scaling = { @SerializationScaling(scalingFactor = 1, assumeUnsigned = true) })
    public float supplyVoltage_V27;

    @FieldModelDescription(description = "PC Power", units = EngineeringUnits.millivolts, debounceSeconds = 5, minimumDelta = 50.0)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 1, assumeUnsigned = true) })
    public float supplyVoltage_T2;

    @FieldModelDescription(description = "Gate On/Off Voltage", units = EngineeringUnits.millivolts, debounceSeconds = 5, minimumDelta = 50.0)
    @SerializationTag(number = 2, width = 16, scaling = { @SerializationScaling(scalingFactor = 1, assumeUnsigned = true) })
    public float supplyVoltage_V04_T1;

    @FieldModelDescription(description = "PC Board Temperature", units = EngineeringUnits.degrees_celsius, pointClass = WellKnownPointClass.HeatsinkTemperature, debounceSeconds = 30,
                           minimumDelta = 1.0)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(scalingFactor = 1, assumeUnsigned = true) })
    public float plcTemperature;

    //--//

    @Override
    public String extractBaseId()
    {
        return "Palfinger_SupplyVoltage";
    }
}
