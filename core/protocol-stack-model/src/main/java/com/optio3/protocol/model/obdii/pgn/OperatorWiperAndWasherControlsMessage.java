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
import com.optio3.protocol.model.obdii.pgn.enums.PgnWasherSwitch;
import com.optio3.protocol.model.obdii.pgn.enums.PgnWiperState;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:OperatorWiperAndWasherControlsMessage")
@PgnMessageType(pgn = 64973, littleEndian = true, ignoreWhenReceived = false)
public class OperatorWiperAndWasherControlsMessage extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Front Non-operator Wiper Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 4, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnWiperState Front_Non_operator_Wiper_Switch;

    @FieldModelDescription(description = "Front Operator Wiper Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 4, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnWiperState Front_Operator_Wiper_Switch;

    @FieldModelDescription(description = "Rear Wiper Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 4, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnWiperState Rear_Wiper_Switch;

    @FieldModelDescription(description = "Front Operator Wiper Delay Control", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Front_Operator_Wiper_Delay_Control;

    @FieldModelDescription(description = "Front Non-operator Wiper Delay Control", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Front_Non_operator_Wiper_Delay_Control;

    @FieldModelDescription(description = "Rear Wiper Delay Control", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Rear_Wiper_Delay_Control;

    @FieldModelDescription(description = "Front Non-operator Washer Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 3, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnWasherSwitch Front_Non_operator_Washer_Switch;

    @FieldModelDescription(description = "Front Operator Washer Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 3, bitOffset = 5, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnWasherSwitch Front_Operator_Washer_Switch;

    @FieldModelDescription(description = "Rear Washer Function", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 3, bitOffset = 5, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnWasherSwitch Rear_Washer_Function;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "OperatorWiperAndWasherControlsMessage";
    }
}