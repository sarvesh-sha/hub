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
import com.optio3.protocol.model.obdii.pgn.enums.PgnDieselExhaustFluid;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:Aftertreatment1DieselExhaustFluidInformation1")
@PgnMessageType(pgn = 64923, littleEndian = true, ignoreWhenReceived = false)
public class Aftertreatment1DieselExhaustFluidInformation1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Temperature 2", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -140.000000)
    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(postScalingOffset = -40.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_Diesel_Exhaust_Fluid_Temperature2;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Concentration", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.25, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_Diesel_Exhaust_Fluid_Concentration;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Conductivity", units = EngineeringUnits.microsiemens_per_millimeter, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(scalingFactor = 5.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_Diesel_Exhaust_Fluid_Conductivity;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Temperature 2 Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 5, bitOffset = 0, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Aftertreatment1_Diesel_Exhaust_Fluid_Temperature2_Preliminary_FMI;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Properties Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 5, bitOffset = 0, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Aftertreatment1_Diesel_Exhaust_Fluid_Properties_Preliminary_FMI;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Property", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 4, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnDieselExhaustFluid Aftertreatment1_Diesel_Exhaust_Fluid_Property;

    @FieldModelDescription(description = "Diesel Exhaust Fluid Speed of Sound", units = EngineeringUnits.meters_per_second, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Diesel_Exhaust_Fluid_Speed_of_Sound;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "Aftertreatment1DieselExhaustFluidInformation1";
    }
}