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

@JsonTypeName("Ipn:Obdii:Pgn:Aftertreatment1AirControl1")
@PgnMessageType(pgn = 64927, littleEndian = true, ignoreWhenReceived = false)
public class Aftertreatment1AirControl1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Aftertreatment 1 Supply Air Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Aftertreatment1_Supply_Air_Pressure;

    @FieldModelDescription(description = "Aftertreatment 1 Purge Air Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Aftertreatment1_Purge_Air_Pressure;

    @FieldModelDescription(description = "Aftertreatment 1 Air Pressure Control", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.0025, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Aftertreatment1_Air_Pressure_Control;

    @FieldModelDescription(description = "Aftertreatment 1 Air Pressure Actuator Position", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_Air_Pressure_Actuator_Position;

    @FieldModelDescription(description = "Aftertreatment 1 Air System Relay", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Air_System_Relay;

    @FieldModelDescription(description = "Aftertreatment 1 Atomization Air Actuator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Atomization_Air_Actuator;

    @FieldModelDescription(description = "Aftertreatment 1 Purge Air Actuator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Purge_Air_Actuator;

    @FieldModelDescription(description = "Aftertreatment 1 Air Enable Actuator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Air_Enable_Actuator;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "Aftertreatment1AirControl1";
    }
}