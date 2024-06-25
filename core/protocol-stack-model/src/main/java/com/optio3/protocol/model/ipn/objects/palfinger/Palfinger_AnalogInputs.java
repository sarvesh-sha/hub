/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.palfinger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.can.CanMessageType;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Can:Palfinger_AnalogInputs")
@CanMessageType(sourceAddress = 0x68A, littleEndian = false)
public class Palfinger_AnalogInputs extends BasePalfingerModel
{
    @FieldModelDescription(description = "B15 Platform Voltage", units = EngineeringUnits.millivolts, minimumDelta = 50.0, debounceSeconds = 3)
    @SerializationTag(number = 0, width = 16, scaling = { @SerializationScaling(scalingFactor = 1, assumeUnsigned = true) })
    public float J41_C_Sensor_B15_Platform_Voltage;

    @FieldModelDescription(description = "B15 Arm Voltage", units = EngineeringUnits.millivolts, minimumDelta = 50.0, debounceSeconds = 3)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 1, assumeUnsigned = true) })
    public float J41_B_Sensor_B15_Arm_Voltage;

    @FieldModelDescription(description = "B15 Platform Angle", units = EngineeringUnits.degrees_angular, minimumDelta = 50.0, debounceSeconds = 3)
    @SerializationTag(number = 2, width = 16, scaling = { @SerializationScaling(scalingFactor = 1, assumeUnsigned = true) })
    public float J41_C_Sensor_B15_Platform_Angle;

    @FieldModelDescription(description = "B15 Arm Angle", units = EngineeringUnits.degrees_angular, minimumDelta = 50.0, debounceSeconds = 3)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(scalingFactor = 1, assumeUnsigned = true) })
    public float J41_B_Sensor_B15_Arm_Angle;

    //--//

    @Override
    public String extractBaseId()
    {
        return "Palfinger_AnalogInputs";
    }
}
