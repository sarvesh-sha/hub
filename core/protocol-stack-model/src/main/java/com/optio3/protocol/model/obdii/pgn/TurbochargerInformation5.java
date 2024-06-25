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
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:TurbochargerInformation5")
@PgnMessageType(pgn = 65175, littleEndian = true, ignoreWhenReceived = false)
public class TurbochargerInformation5 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Turbocharger 1 Turbine Outlet Temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -373.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.03125, postScalingOffset = -273.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Turbocharger1_Turbine_Outlet_Temperature;

    @FieldModelDescription(description = "Engine Turbocharger 2 Turbine Outlet Temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -373.000000)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.03125, postScalingOffset = -273.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Turbocharger2_Turbine_Outlet_Temperature;

    @FieldModelDescription(description = "Engine Turbocharger 3 Turbine Outlet Temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -373.000000)
    @SerializationTag(number = 5, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.03125, postScalingOffset = -273.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Turbocharger3_Turbine_Outlet_Temperature;

    @FieldModelDescription(description = "Engine Turbocharger 4 Turbine Outlet Temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -373.000000)
    @SerializationTag(number = 7, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.03125, postScalingOffset = -273.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Turbocharger4_Turbine_Outlet_Temperature;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "TurbochargerInformation5";
    }
}