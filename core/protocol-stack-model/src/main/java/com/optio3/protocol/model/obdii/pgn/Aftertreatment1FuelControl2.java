/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.obdii.PgnMessageType;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:Aftertreatment1FuelControl2")
@PgnMessageType(pgn = 64869, littleEndian = true, ignoreWhenReceived = false)
public class Aftertreatment1FuelControl2 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Aftertreatment 1 Fuel Pressure 2", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Aftertreatment1_Fuel_Pressure2;

    @FieldModelDescription(description = "Aftertreatment 1 Fuel Pump Relay Control", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Fuel_Pump_Relay_Control;

    @FieldModelDescription(description = "Aftertreatment 1 Fuel Flow Diverter Valve Control", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Fuel_Flow_Diverter_Valve_Control;

    @FieldModelDescription(description = "Aftertreatment 1 Fuel Pressure 2 Actuator Control", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.0025, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Aftertreatment1_Fuel_Pressure2_Actuator_Control;

    @FieldModelDescription(description = "Aftertreatment 1 Hydrocarbon Doser Intake Fuel Temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -140.000000)
    @SerializationTag(number = 6, width = 8, scaling = { @SerializationScaling(postScalingOffset = -40.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_Hydrocarbon_Doser_Intake_Fuel_Temperature;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "Aftertreatment1FuelControl2";
    }
}