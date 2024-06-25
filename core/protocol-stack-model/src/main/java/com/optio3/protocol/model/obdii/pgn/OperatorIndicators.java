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
import com.optio3.protocol.model.obdii.pgn.enums.PgnInducementAnomaly;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.protocol.model.obdii.pgn.enums.PgnWarningStatus;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:OperatorIndicators")
@PgnMessageType(pgn = 65279, littleEndian = true, ignoreWhenReceived = false)
public class OperatorIndicators extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Water In Fuel Indicator 1", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Water_In_Fuel_Indicator1;

    @FieldModelDescription(description = "Operator Shift Prompt", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 2, bitOffset = 2, scaling = { @SerializationScaling(scalingFactor = 4.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public Optional<Unsigned8> Operator_Shift_Prompt;

    @FieldModelDescription(description = "Water in Fuel Indicator 2", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Water_in_Fuel_Indicator2;

    @FieldModelDescription(description = "Engine Overloaded Indicator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Overloaded_Indicator;

    @FieldModelDescription(description = "Driver Warning System Indicator Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 3, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnWarningStatus Driver_Warning_System_Indicator_Status;

    @FieldModelDescription(description = "Emission Control System Operator Inducement Severity", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 3, bitOffset = 3, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnInducementAnomaly Emission_Control_System_Operator_Inducement_Severity;

    @FieldModelDescription(description = "Water In Charge Air Duct After Charge Air Cooler Indicator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Water_In_Charge_Air_Duct_After_Charge_Air_Cooler_Indicator;

    @FieldModelDescription(description = "Fuel Supply Estimated Remaining Distance", units = EngineeringUnits.kilometers, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Fuel_Supply_Estimated_Remaining_Distance;

    @FieldModelDescription(description = "Automatic Start Request Inhibit Indicator", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 2, bitOffset = 0, scaling = { @SerializationScaling(scalingFactor = 4.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public Optional<Unsigned8> Automatic_Start_Request_Inhibit_Indicator;

    @FieldModelDescription(description = "Engine Oil Life Remaining", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 6, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Oil_Life_Remaining;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "OperatorIndicators";
    }
}