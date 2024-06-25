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
import com.optio3.protocol.model.obdii.pgn.enums.PgnEngineState;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:EngineOperatingInformation")
@PgnMessageType(pgn = 64914, littleEndian = true, ignoreWhenReceived = false)
public class EngineOperatingInformation extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Operating State", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 4, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnEngineState Engine_Operating_State;

    @FieldModelDescription(description = "Fuel Pump Primer Control", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 4)
    public PgnActivationMode Fuel_Pump_Primer_Control;

    @FieldModelDescription(description = "Engine Starter Motor Relay Control", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 6)
    public PgnActivationMode Engine_Starter_Motor_Relay_Control;

    @FieldModelDescription(description = "Time Remaining in Engine Operating State", units = EngineeringUnits.seconds, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Time_Remaining_in_Engine_Operating_State;

    @FieldModelDescription(description = "Engine Fuel Shutoff Vent Control", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 0)
    public PgnActivationMode Engine_Fuel_Shutoff_Vent_Control;

    @FieldModelDescription(description = "Engine Fuel Shutoff 1 Control", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 2)
    public PgnActivationMode Engine_Fuel_Shutoff1_Control;

    @FieldModelDescription(description = "Engine Fuel Shutoff 2 Control", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 4)
    public PgnActivationMode Engine_Fuel_Shutoff2_Control;

    @FieldModelDescription(description = "Engine Fuel Shutoff Valve Leak Test Control", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 6)
    public PgnActivationMode Engine_Fuel_Shutoff_Valve_Leak_Test_Control;

    @FieldModelDescription(description = "Engine Oil Priming Pump Control", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Oil_Priming_Pump_Control;

    @FieldModelDescription(description = "Engine Oil Pre-heater Control", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 2)
    public PgnActivationMode Engine_Oil_Pre_heater_Control;

    @FieldModelDescription(description = "Engine Electrical System Power Conservation Control", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Electrical_System_Power_Conservation_Control;

    @FieldModelDescription(description = "Engine Pre-Heater Control", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Pre_Heater_Control;

    @FieldModelDescription(description = "Engine Coolant Pump Control", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 0)
    public PgnActivationMode Engine_Coolant_Pump_Control;

    @FieldModelDescription(description = "Engine Controlled Shutdown Request", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 2)
    public PgnActivationMode Engine_Controlled_Shutdown_Request;

    @FieldModelDescription(description = "Engine Emergency (Immediate) Shutdown Indication", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 4)
    public PgnActivationMode Engine_Emergency_Immediate_Shutdown_Indication;

    @FieldModelDescription(description = "Engine Cold Ambient Elevated Idle Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Cold_Ambient_Elevated_Idle_Status;

    @FieldModelDescription(description = "Engine Desired Torque Request", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -225.000000)
    @SerializationTag(number = 7, width = 8, scaling = { @SerializationScaling(postScalingOffset = -125.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Desired_Torque_Request;

    @FieldModelDescription(description = "Engine Derate Request", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 8, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Derate_Request;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "EngineOperatingInformation";
    }
}