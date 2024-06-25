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
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:DirectLampControlData2")
@PgnMessageType(pgn = 64772, littleEndian = true, ignoreWhenReceived = false)
public class DirectLampControlData2 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Vehicle Battery Voltage Low Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Vehicle_Battery_Voltage_Low_Lamp_Data;

    @FieldModelDescription(description = "Vehicle Fuel Level Low Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Vehicle_Fuel_Level_Low_Lamp_Data;

    @FieldModelDescription(description = "Vehicle Air Pressure Low Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Vehicle_Air_Pressure_Low_Lamp_Data;

    @FieldModelDescription(description = "Vehicle HVAC Recirculation Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Vehicle_HVAC_Recirculation_Lamp_Data;

    @FieldModelDescription(description = "Vehicle Battery Charging Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Vehicle_Battery_Charging_Lamp_Data;

    @FieldModelDescription(description = "Hill Holder Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Hill_Holder_Lamp_Data;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "DirectLampControlData2";
    }
}