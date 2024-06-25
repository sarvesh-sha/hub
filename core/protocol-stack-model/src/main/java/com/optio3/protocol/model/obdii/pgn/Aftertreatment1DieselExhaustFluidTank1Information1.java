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
import com.optio3.protocol.model.obdii.pgn.enums.PgnWarningStatus;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:Aftertreatment1DieselExhaustFluidTank1Information1")
@PgnMessageType(pgn = 65110, littleEndian = true, ignoreWhenReceived = false)
public class Aftertreatment1DieselExhaustFluidTank1Information1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Tank Volume", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_Diesel_Exhaust_Fluid_Tank_Volume;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Tank Temperature 1", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -140.000000)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(postScalingOffset = -40.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_Diesel_Exhaust_Fluid_Tank_Temperature1;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Tank Level", units = EngineeringUnits.millimeters, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Aftertreatment1_Diesel_Exhaust_Fluid_Tank_Level;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Tank Level/Volume Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 5, bitOffset = 0, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Aftertreatment1_Diesel_Exhaust_Fluid_Tank_Level_Volume_Preliminary_FMI;

    @FieldModelDescription(description = "Aftertreatment Diesel Exhaust Fluid Tank Low Level Indicator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 3, bitOffset = 5, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnWarningStatus Aftertreatment_Diesel_Exhaust_Fluid_Tank_Low_Level_Indicator;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Tank 1 Temperature Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 6, width = 5, bitOffset = 0, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Aftertreatment1_Diesel_Exhaust_Fluid_Tank1_Temperature_Preliminary_FMI;

    @FieldModelDescription(description = "Aftertreatment SCR Operator Inducement Severity", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 3, bitOffset = 5, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnInducementAnomaly Aftertreatment_SCR_Operator_Inducement_Severity;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Tank Heater", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_Diesel_Exhaust_Fluid_Tank_Heater;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Tank 1 Heater Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 8, width = 5, bitOffset = 0, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Aftertreatment1_Diesel_Exhaust_Fluid_Tank1_Heater_Preliminary_FMI;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "Aftertreatment1DieselExhaustFluidTank1Information1";
    }
}