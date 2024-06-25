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
import com.optio3.protocol.model.obdii.pgn.enums.PgnActivationMode;
import com.optio3.protocol.model.obdii.pgn.enums.PgnCommandOverride;
import com.optio3.protocol.model.obdii.pgn.enums.PgnLaunchGear;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:TransmissionControl1")
@PgnMessageType(pgn = 256, littleEndian = true, ignoreWhenReceived = false)
public class TransmissionControl1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Transmission Gear Shift Inhibit Request", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnCommandOverride Transmission_Gear_Shift_Inhibit_Request;

    @FieldModelDescription(description = "Transmission Torque Converter Lockup Request", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnCommandOverride Transmission_Torque_Converter_Lockup_Request;

    @FieldModelDescription(description = "Disengage Driveline Request", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnCommandOverride Disengage_Driveline_Request;

    @FieldModelDescription(description = "Transmission Reverse Gear Shift Inhibit Request", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 6)
    public PgnActivationMode Transmission_Reverse_Gear_Shift_Inhibit_Request;

    @FieldModelDescription(description = "Requested Percent Clutch Slip", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Requested_Percent_Clutch_Slip;

    @FieldModelDescription(description = "Transmission Requested Gear", units = EngineeringUnits.counts, debounceSeconds = 15, noValueMarker = -225.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(postScalingOffset = -125.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Transmission_Requested_Gear;

    @FieldModelDescription(description = "Disengage Differential Lock Request - Front Axle 1", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 0)
    public PgnActivationMode Disengage_Differential_Lock_Request_Front_Axle1;

    @FieldModelDescription(description = "Disengage Differential Lock Request - Front Axle 2", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 2)
    public PgnActivationMode Disengage_Differential_Lock_Request_Front_Axle2;

    @FieldModelDescription(description = "Disengage Differential Lock Request - Rear Axle 1", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 4)
    public PgnActivationMode Disengage_Differential_Lock_Request_Rear_Axle1;

    @FieldModelDescription(description = "Disengage Differential Lock Request - Rear Axle 2", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 6)
    public PgnActivationMode Disengage_Differential_Lock_Request_Rear_Axle2;

    @FieldModelDescription(description = "Disengage Differential Lock Request - Central", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 0)
    public PgnActivationMode Disengage_Differential_Lock_Request_Central;

    @FieldModelDescription(description = "Disengage Differential Lock Request - Central Front", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 2)
    public PgnActivationMode Disengage_Differential_Lock_Request_Central_Front;

    @FieldModelDescription(description = "Disengage Differential Lock Request - Central Rear", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 4)
    public PgnActivationMode Disengage_Differential_Lock_Request_Central_Rear;

    @FieldModelDescription(description = "Transmission Load Reduction Inhibit Request", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 6)
    public PgnActivationMode Transmission_Load_Reduction_Inhibit_Request;

    @FieldModelDescription(description = "Transmission Mode 1", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 0)
    public PgnActivationMode Transmission_Mode1;

    @FieldModelDescription(description = "Transmission Mode 2", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 2)
    public PgnActivationMode Transmission_Mode2;

    @FieldModelDescription(description = "Transmission Mode 3", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 4)
    public PgnActivationMode Transmission_Mode3;

    @FieldModelDescription(description = "Transmission Mode 4", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 6)
    public PgnActivationMode Transmission_Mode4;

    @FieldModelDescription(description = "Transmission Auto-Neutral (Manual Return) Request", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 0)
    public PgnActivationMode Transmission_Auto_Neutral_Manual_Return_Request;

    @FieldModelDescription(description = "Transmission Requested Launch Gear", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 4, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnLaunchGear Transmission_Requested_Launch_Gear;

    @FieldModelDescription(description = "Transmission Shift Selector Display Mode Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Shift_Selector_Display_Mode_Switch;

    @FieldModelDescription(description = "Transmission Mode 5", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 0)
    public PgnActivationMode Transmission_Mode5;

    @FieldModelDescription(description = "Transmission Mode 6", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 2)
    public PgnActivationMode Transmission_Mode6;

    @FieldModelDescription(description = "Transmission Mode 7", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 4)
    public PgnActivationMode Transmission_Mode7;

    @FieldModelDescription(description = "Transmission Mode 8", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 6)
    public PgnActivationMode Transmission_Mode8;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "TransmissionControl1";
    }
}