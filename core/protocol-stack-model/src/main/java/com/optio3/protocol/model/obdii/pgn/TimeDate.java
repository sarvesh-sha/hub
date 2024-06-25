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

@JsonTypeName("Ipn:Obdii:Pgn:TimeDate")
@PgnMessageType(pgn = 65254, littleEndian = true, ignoreWhenReceived = true)
public class TimeDate extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Seconds", units = EngineeringUnits.seconds, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.25, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Seconds;

    @FieldModelDescription(description = "Minutes", units = EngineeringUnits.minutes, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Minutes;

    @FieldModelDescription(description = "Hours", units = EngineeringUnits.hours, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Hours;

    @FieldModelDescription(description = "Month", units = EngineeringUnits.months, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Month;

    @FieldModelDescription(description = "Day", units = EngineeringUnits.days, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.25, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Day;

    @FieldModelDescription(description = "Year", units = EngineeringUnits.years, debounceSeconds = 15, noValueMarker = 1885.000000)
    @SerializationTag(number = 6, width = 8, scaling = { @SerializationScaling(postScalingOffset = 1985.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Year;

    @FieldModelDescription(description = "Local minute offset", units = EngineeringUnits.minutes, debounceSeconds = 15, noValueMarker = -225.000000)
    @SerializationTag(number = 7, width = 8, scaling = { @SerializationScaling(postScalingOffset = -125.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Local_minute_offset;

    @FieldModelDescription(description = "Local hour offset", units = EngineeringUnits.hours, debounceSeconds = 15, noValueMarker = -225.000000)
    @SerializationTag(number = 8, width = 8, scaling = { @SerializationScaling(postScalingOffset = -125.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Local_hour_offset;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "TimeDate";
    }
}