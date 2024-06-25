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
import com.optio3.protocol.model.obdii.pgn.enums.PgnHeaterStatus;
import com.optio3.protocol.model.obdii.pgn.enums.PgnNOxSensor;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:Aftertreatment1OutletGas1")
@PgnMessageType(pgn = 61455, littleEndian = true, ignoreWhenReceived = false)
public class Aftertreatment1OutletGas1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Aftertreatment 1 Outlet NOx 1", units = EngineeringUnits.parts_per_million, debounceSeconds = 15, noValueMarker = -300.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.05, postScalingOffset = -200.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Aftertreatment1_Outlet_NOx1;

    @FieldModelDescription(description = "Aftertreatment 1 Outlet Percent Oxygen 1", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -112.000000)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(scalingFactor = 5.14E-4, postScalingOffset = -12.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Aftertreatment1_Outlet_Percent_Oxygen1;

    @FieldModelDescription(description = "Aftertreatment 1 Outlet Gas Sensor 1 Power In Range", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Outlet_Gas_Sensor1_Power_In_Range;

    @FieldModelDescription(description = "Aftertreatment 1 Outlet Gas Sensor 1 at Temperature", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Outlet_Gas_Sensor1_at_Temperature;

    @FieldModelDescription(description = "Aftertreatment 1 Outlet NOx 1 Reading Stable", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Outlet_NOx1_Reading_Stable;

    @FieldModelDescription(description = "Aftertreatment 1 Outlet Wide-Range Percent Oxygen 1 Reading Stable", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Outlet_Wide_Range_Percent_Oxygen1_Reading_Stable;

    @FieldModelDescription(description = "Aftertreatment 1 Outlet Gas Sensor 1 Heater Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 6, width = 5, bitOffset = 0, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Aftertreatment1_Outlet_Gas_Sensor1_Heater_Preliminary_FMI;

    @FieldModelDescription(description = "Aftertreatment 1 Outlet Gas Sensor 1 Heater Control", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 5)
    public PgnHeaterStatus Aftertreatment1_Outlet_Gas_Sensor1_Heater_Control;

    @FieldModelDescription(description = "Aftertreatment 1 Outlet NOx Sensor 1 Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 5, bitOffset = 0, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Aftertreatment1_Outlet_NOx_Sensor1_Preliminary_FMI;

    @FieldModelDescription(description = "Aftertreatment 1 Outlet NOx Sensor 1 Self-diagnosis Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 3, bitOffset = 5, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnNOxSensor Aftertreatment1_Outlet_NOx_Sensor1_Self_diagnosis_Status;

    @FieldModelDescription(description = "Aftertreatment 1 Outlet Oxygen Sensor 1 Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 8, width = 5, bitOffset = 0, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Aftertreatment1_Outlet_Oxygen_Sensor1_Preliminary_FMI;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "Aftertreatment1OutletGas1";
    }
}