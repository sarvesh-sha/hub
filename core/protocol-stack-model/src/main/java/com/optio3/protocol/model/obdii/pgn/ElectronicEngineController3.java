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

@JsonTypeName("Ipn:Obdii:Pgn:ElectronicEngineController3")
@PgnMessageType(pgn = 65247, littleEndian = true, ignoreWhenReceived = false)
public class ElectronicEngineController3 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Nominal Friction - Percent Torque", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -225.000000)
    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(postScalingOffset = -125.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Nominal_Friction_Percent_Torque;

    @FieldModelDescription(description = "Engine's Desired Operating Speed", units = EngineeringUnits.revolutions_per_minute, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.125, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Desired_Operating_Speed;

    @FieldModelDescription(description = "Engine's Desired Operating Speed Asymmetry Adjustment", units = EngineeringUnits.counts, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Desired_Operating_Speed_Asymmetry_Adjustment;

    @FieldModelDescription(description = "Estimated Engine Parasitic Losses - Percent Torque", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -225.000000)
    @SerializationTag(number = 5, width = 8, scaling = { @SerializationScaling(postScalingOffset = -125.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Estimated_Engine_Parasitic_Losses_Percent_Torque;

    @FieldModelDescription(description = "Aftertreatment 1 Exhaust Gas Mass Flow Rate", units = EngineeringUnits.kilograms_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 6, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.2, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Aftertreatment1_Exhaust_Gas_Mass_Flow_Rate;

    @FieldModelDescription(description = "Aftertreatment 1 Intake Dew Point", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Intake_Dew_Point;

    @FieldModelDescription(description = "Aftertreatment 1 Exhaust Dew Point", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Exhaust_Dew_Point;

    @FieldModelDescription(description = "Aftertreatment 2 Intake Dew Point", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment2_Intake_Dew_Point;

    @FieldModelDescription(description = "Aftertreatment 2 Exhaust Dew Point", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment2_Exhaust_Dew_Point;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "ElectronicEngineController3";
    }
}