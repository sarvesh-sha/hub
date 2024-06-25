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
import com.optio3.protocol.model.obdii.pgn.enums.PgnTemperatureRange;
import com.optio3.protocol.model.obdii.pgn.enums.PgnValveState;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:EngineFuelInformation1")
@PgnMessageType(pgn = 64754, littleEndian = true, ignoreWhenReceived = false)
public class EngineFuelInformation1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Desired Throttle Valve 1 Position ", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Desired_Throttle_Valve1_Position;

    @FieldModelDescription(description = "Engine Throttle Valve 1 Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 5, bitOffset = 0, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Engine_Throttle_Valve1_Preliminary_FMI;

    @FieldModelDescription(description = "Engine Throttle Valve 1 Temperature Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 3, bitOffset = 5, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnTemperatureRange Engine_Throttle_Valve1_Temperature_Status;

    @FieldModelDescription(description = "Engine Desired Throttle Valve 2 Position", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Desired_Throttle_Valve2_Position;

    @FieldModelDescription(description = "Engine Throttle Valve 2 Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 5, bitOffset = 0, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Engine_Throttle_Valve2_Preliminary_FMI;

    @FieldModelDescription(description = "Engine Throttle Valve 2 Temperature status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 3, bitOffset = 5, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnTemperatureRange Engine_Throttle_Valve2_Temperature_status;

    @FieldModelDescription(description = "Engine Fuel Valve 1 Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 5, bitOffset = 0, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Engine_Fuel_Valve1_Preliminary_FMI;

    @FieldModelDescription(description = "Engine Fuel Valve 1 Temperature Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 3, bitOffset = 5, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnTemperatureRange Engine_Fuel_Valve1_Temperature_Status;

    @FieldModelDescription(description = "Engine Fuel Valve 2 Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 6, width = 5, bitOffset = 0, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Engine_Fuel_Valve2_Preliminary_FMI;

    @FieldModelDescription(description = "Engine Fuel Valve 2 Temperature Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 3, bitOffset = 5, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnTemperatureRange Engine_Fuel_Valve2_Temperature_Status;

    @FieldModelDescription(description = "Engine Throttle Valve 1 Operation Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 4, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnValveState Engine_Throttle_Valve1_Operation_Status;

    @FieldModelDescription(description = "Engine Throttle Valve 2 Operation Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 4, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnValveState Engine_Throttle_Valve2_Operation_Status;

    @FieldModelDescription(description = "Engine Fuel Valve 1 Operation Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 4, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnValveState Engine_Fuel_Valve1_Operation_Status;

    @FieldModelDescription(description = "Engine Fuel Valve 2 Operation Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 4, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnValveState Engine_Fuel_Valve2_Operation_Status;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "EngineFuelInformation1";
    }
}