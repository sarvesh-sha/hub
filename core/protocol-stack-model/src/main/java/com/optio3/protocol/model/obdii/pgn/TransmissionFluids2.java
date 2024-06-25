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
import com.optio3.protocol.model.obdii.pgn.enums.PgnCountdownTimer;
import com.optio3.protocol.model.obdii.pgn.enums.PgnOilLevelIndicator;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.protocol.model.obdii.pgn.enums.PgnTransmissionOverheat;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:TransmissionFluids2")
@PgnMessageType(pgn = 64917, littleEndian = true, ignoreWhenReceived = false)
public class TransmissionFluids2 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Transmission Oil Filter Restriction Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Oil_Filter_Restriction_Switch;

    @FieldModelDescription(description = "Transmission Oil Level Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Oil_Level_Switch;

    @FieldModelDescription(description = "Transmission Overheat Indicator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnTransmissionOverheat Transmission_Overheat_Indicator;

    @FieldModelDescription(description = "Transmission Torque Converter Oil Outlet Temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -373.000000)
    @SerializationTag(number = 2, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.03125, postScalingOffset = -273.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Transmission_Torque_Converter_Oil_Outlet_Temperature;

    @FieldModelDescription(description = "Transmission Oil Life Remaining", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Transmission_Oil_Life_Remaining;

    @FieldModelDescription(description = "Transmission Oil Temperature 2", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -373.000000)
    @SerializationTag(number = 5, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.03125, postScalingOffset = -273.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Transmission_Oil_Temperature2;

    @FieldModelDescription(description = "Transmission Oil Level 2 High / Low", units = EngineeringUnits.liters, debounceSeconds = 15, noValueMarker = -163.000000)
    @SerializationTag(number = 7, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.5, postScalingOffset = -62.5, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Transmission_Oil_Level2_High_Low;

    @FieldModelDescription(description = "Transmission Oil Level 2 Countdown Timer", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 4, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnCountdownTimer Transmission_Oil_Level2_Countdown_Timer;

    @FieldModelDescription(description = "Transmission Oil Level 2 Measurement Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 4, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnOilLevelIndicator Transmission_Oil_Level2_Measurement_Status;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "TransmissionFluids2";
    }
}