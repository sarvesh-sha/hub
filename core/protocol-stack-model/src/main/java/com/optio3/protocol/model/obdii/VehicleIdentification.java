/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;

@JsonTypeName("Ipn:Obdii:VehicleIdentification")
public class VehicleIdentification extends ObdiiObjectModel
{
    @FieldModelDescription(description = "VIN", units = EngineeringUnits.no_units)
    public String VIN;

    //--//

    @Override
    public String extractBaseId()
    {
        return "VehicleIdentification";
    }
}