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
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:Aftertreatment1Service1")
@PgnMessageType(pgn = 64891, littleEndian = true, ignoreWhenReceived = false)
public class Aftertreatment1Service1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Aftertreatment 1 Diesel Particulate Filter Soot Load Percent", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_Diesel_Particulate_Filter_Soot_Load_Percent;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Particulate Filter Ash Load Percent", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_Diesel_Particulate_Filter_Ash_Load_Percent;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Particulate Filter Time Since Last Active Regeneration", units = EngineeringUnits.seconds, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 32, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 2147483647)
    public float Aftertreatment1_Diesel_Particulate_Filter_Time_Since_Last_Active_Regeneration;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Particulate Filter Soot Load Regeneration Threshold", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.0025, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Aftertreatment1_Diesel_Particulate_Filter_Soot_Load_Regeneration_Threshold;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "Aftertreatment1Service1";
    }
}