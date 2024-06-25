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

@JsonTypeName("Ipn:Can:NitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs")
@CanExtendedMessageType(priority = 6, dataPage = false, pduFormat = 0xFF, destinationAddress = 0x02, sourceAddress = 0x47, littleEndian = true)
@CanExtendedMessageType(priority = 6, dataPage = false, pduFormat = 0xFF, destinationAddress = 0x02, sourceAddress = 0x48, littleEndian = true)
@CanExtendedMessageType(priority = 6, dataPage = false, pduFormat = 0xFF, destinationAddress = 0x02, sourceAddress = 0x49, littleEndian = true)
@CanExtendedMessageType(priority = 6, dataPage = false, pduFormat = 0xFF, destinationAddress = 0x02, sourceAddress = 0x4A, littleEndian = true)
public class NitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs extends BaseBatteryNitePhoenixModel
{
    @FieldModelDescription(description = "Time Remaining", units = EngineeringUnits.hours, debounceSeconds = 15)
    @SerializationTag(number = 0, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1, assumeUnsigned = true) })
    public float timeRemaining;

    @FieldModelDescription(description = "State of Charge", pointClass = WellKnownPointClass.HvacStateOfCharge, units = EngineeringUnits.percent, debounceSeconds = 15)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public float stateOfCharge;

    @FieldModelDescription(description = "State of Health", pointClass = WellKnownPointClass.HvacStateOfHealth, units = EngineeringUnits.percent, debounceSeconds = 15)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public float stateOfHealth;

    //--//

    @Override
    public String extractBaseId()
    {
        return "NitePhoenix_BMS_AlgorithmOutputs";
    }
}
