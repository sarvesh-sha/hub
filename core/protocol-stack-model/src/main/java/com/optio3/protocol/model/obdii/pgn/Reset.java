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
import com.optio3.protocol.model.obdii.pgn.enums.PgnCommandSignal;
import com.optio3.protocol.model.obdii.pgn.enums.PgnResetRequest;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:Reset")
@PgnMessageType(pgn = 56832, littleEndian = true, ignoreWhenReceived = false)
public class Reset extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Trip Group 1", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnCommandSignal Trip_Group1;

    @FieldModelDescription(description = "Trip Group 2 - Proprietary", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnCommandSignal Trip_Group2_Proprietary;

    @FieldModelDescription(description = "Service Component Identification", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Service_Component_Identification;

    @FieldModelDescription(description = "Engine Build Hours Reset", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 0)
    public PgnResetRequest Engine_Build_Hours_Reset;

    @FieldModelDescription(description = "Steering Straight Ahead Position Reset", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnCommandSignal Steering_Straight_Ahead_Position_Reset;

    @FieldModelDescription(description = "Engine Spark Plug Secondary Voltage Tracking Reset", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 4)
    public PgnResetRequest Engine_Spark_Plug_Secondary_Voltage_Tracking_Reset;

    @FieldModelDescription(description = "Engine Ignition Control Maintenance Hours Reset", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 6)
    public PgnResetRequest Engine_Ignition_Control_Maintenance_Hours_Reset;

    @FieldModelDescription(description = "Bin Lift Count Reset", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 0)
    public PgnResetRequest Bin_Lift_Count_Reset;

    @FieldModelDescription(description = "Tire Configuration Information", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnCommandSignal Tire_Configuration_Information;

    @FieldModelDescription(description = "Tire Sensor Information", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnCommandSignal Tire_Sensor_Information;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "Reset";
    }
}