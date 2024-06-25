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
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:Aftertreatment1OutletGas2")
@PgnMessageType(pgn = 64947, littleEndian = true, ignoreWhenReceived = false)
public class Aftertreatment1OutletGas2 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Aftertreatment 1 Exhaust Temperature 3", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -373.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.03125, postScalingOffset = -273.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Aftertreatment1_Exhaust_Temperature3;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Particulate Filter Outlet Temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -373.000000)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.03125, postScalingOffset = -273.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Aftertreatment1_Diesel_Particulate_Filter_Outlet_Temperature;

    @FieldModelDescription(description = "Aftertreatment 1 Exhaust Temperature 3 Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 5, bitOffset = 0, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Aftertreatment1_Exhaust_Temperature3_Preliminary_FMI;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Particulate Filter Outlet Exhaust Temperature Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 6, width = 5, bitOffset = 0, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Aftertreatment1_Diesel_Particulate_Filter_Outlet_Exhaust_Temperature_Preliminary_FMI;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "Aftertreatment1OutletGas2";
    }
}