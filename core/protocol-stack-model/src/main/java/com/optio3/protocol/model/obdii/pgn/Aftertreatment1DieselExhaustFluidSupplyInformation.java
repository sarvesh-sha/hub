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

@JsonTypeName("Ipn:Obdii:Pgn:Aftertreatment1DieselExhaustFluidSupplyInformation")
@PgnMessageType(pgn = 64828, littleEndian = true, ignoreWhenReceived = false)
public class Aftertreatment1DieselExhaustFluidSupplyInformation extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Pump 1 Motor Speed", units = EngineeringUnits.revolutions_per_minute, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.5, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Aftertreatment1_Diesel_Exhaust_Fluid_Pump1_Motor_Speed;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Pump Drive Percentage", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_Diesel_Exhaust_Fluid_Pump_Drive_Percentage;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Dosing Unit 1 Diverter Valve", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_Diesel_Exhaust_Fluid_Dosing_Unit1_Diverter_Valve;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Tank Fill Valve Command", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_Diesel_Exhaust_Fluid_Tank_Fill_Valve_Command;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Pump State", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Diesel_Exhaust_Fluid_Pump_State;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Pump Power Relay", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Diesel_Exhaust_Fluid_Pump_Power_Relay;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Pump", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Diesel_Exhaust_Fluid_Pump;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Tank Drain Valve Command", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_Diesel_Exhaust_Fluid_Tank_Drain_Valve_Command;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Dosing Unit 2 Diverter Valve", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 8, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_Diesel_Exhaust_Fluid_Dosing_Unit2_Diverter_Valve;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "Aftertreatment1DieselExhaustFluidSupplyInformation";
    }
}