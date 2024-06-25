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
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:TurbochargerWastegate")
@PgnMessageType(pgn = 65174, littleEndian = true, ignoreWhenReceived = false)
public class TurbochargerWastegate extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Turbocharger Wastegate Actuator 1 Position", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Turbocharger_Wastegate_Actuator1_Position;

    @FieldModelDescription(description = "Engine Turbocharger Wastegate Actuator 2 Position", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Turbocharger_Wastegate_Actuator2_Position;

    @FieldModelDescription(description = "Engine Turbocharger Wastegate Actuator 3 Position", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Turbocharger_Wastegate_Actuator3_Position;

    @FieldModelDescription(description = "Engine Turbocharger Wastegate Actuator 4 Position", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Turbocharger_Wastegate_Actuator4_Position;

    @FieldModelDescription(description = "Engine Turbocharger Wastegate Actuator Control Air Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 8, scaling = { @SerializationScaling(scalingFactor = 4.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Turbocharger_Wastegate_Actuator_Control_Air_Pressure;

    @FieldModelDescription(description = "Engine Turbocharger Wastegate Actuator 1 Desired Position", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 6, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Turbocharger_Wastegate_Actuator1_Desired_Position;

    @FieldModelDescription(description = "Engine Turbocharger Wastegate Actuator 1 Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 5, bitOffset = 0, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Engine_Turbocharger_Wastegate_Actuator1_Preliminary_FMI;

    @FieldModelDescription(description = "Engine Turbocharger Wastegate Actuator 1 Temperature Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 3, bitOffset = 5, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnTemperatureRange Engine_Turbocharger_Wastegate_Actuator1_Temperature_Status;

    @FieldModelDescription(description = "Engine Turbocharger Wastegate Actuator 2 Desired Position", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 8, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Turbocharger_Wastegate_Actuator2_Desired_Position;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "TurbochargerWastegate";
    }
}