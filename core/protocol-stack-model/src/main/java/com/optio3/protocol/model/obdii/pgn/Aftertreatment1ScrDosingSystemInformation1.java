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
import com.optio3.protocol.model.obdii.pgn.enums.PgnScrState;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:Aftertreatment1ScrDosingSystemInformation1")
@PgnMessageType(pgn = 61475, littleEndian = true, ignoreWhenReceived = false)
public class Aftertreatment1ScrDosingSystemInformation1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Actual Dosing Quantity", units = EngineeringUnits.us_gallons_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.3, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Aftertreatment1_Diesel_Exhaust_Fluid_Actual_Dosing_Quantity;

    @FieldModelDescription(description = "Aftertreatment 1 SCR System 1 State", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 4, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnScrState Aftertreatment1_SCR_System1_State;

    @FieldModelDescription(description = "Aftertreatment 1 SCR System 2 State", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 4, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnScrState Aftertreatment1_SCR_System2_State;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Actual Quantity of Integrator", units = EngineeringUnits.us_gallons, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Aftertreatment1_Diesel_Exhaust_Fluid_Actual_Quantity_of_Integrator;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Doser 1 Absolute Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 6, width = 8, scaling = { @SerializationScaling(scalingFactor = 8.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_Diesel_Exhaust_Fluid_Doser1_Absolute_Pressure;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Actual Dosing Quantity (High Range)", units = EngineeringUnits.us_gallons_per_minute, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.05, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Aftertreatment1_Diesel_Exhaust_Fluid_Actual_Dosing_Quantity_High_Range;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "Aftertreatment1ScrDosingSystemInformation1";
    }
}