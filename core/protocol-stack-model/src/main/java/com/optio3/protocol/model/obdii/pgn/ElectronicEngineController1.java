/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.obdii.PgnMessageType;
import com.optio3.protocol.model.obdii.pgn.enums.PgnEngineStarterMode;
import com.optio3.protocol.model.obdii.pgn.enums.PgnTorqueMode;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:ElectronicEngineController1")
@PgnMessageType(pgn = 61444, littleEndian = true, ignoreWhenReceived = false)
public class ElectronicEngineController1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Torque Mode", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 4, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnTorqueMode Engine_Torque_Mode;

    @FieldModelDescription(description = "Actual Engine - Percent Torque (Fractional)", units = EngineeringUnits.percent, debounceSeconds = 15)
    @SerializationTag(number = 1, width = 4, bitOffset = 4, scaling = { @SerializationScaling(scalingFactor = 0.125, assumeUnsigned = true) })
    public float Actual_Engine_Percent_Torque_Fractional;

    @FieldModelDescription(description = "Driver's Demand Engine - Percent Torque", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -225.000000)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(postScalingOffset = -125.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Driver_Demand_Engine_Percent_Torque;

    @FieldModelDescription(description = "Actual Engine - Percent Torque", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -225.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(postScalingOffset = -125.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Actual_Engine_Percent_Torque;

    @FieldModelDescription(description = "Engine Speed", units = EngineeringUnits.revolutions_per_minute, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.125, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Speed;

    @FieldModelDescription(description = "Source Address of Controlling Device for Engine Control", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 6, width = 8, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 255)
    public Optional<Unsigned8> Source_Address_of_Controlling_Device_for_Engine_Control;

    @FieldModelDescription(description = "Engine Starter Mode", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 4, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnEngineStarterMode Engine_Starter_Mode;

    @FieldModelDescription(description = "Engine Demand Ð Percent Torque", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -225.000000)
    @SerializationTag(number = 8, width = 8, scaling = { @SerializationScaling(postScalingOffset = -125.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Demand_Percent_Torque;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "ElectronicEngineController1";
    }
}