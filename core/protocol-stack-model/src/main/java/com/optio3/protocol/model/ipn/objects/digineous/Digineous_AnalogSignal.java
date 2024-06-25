/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.digineous;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.FieldTemporalResolution;

@JsonTypeName("Ipn:Digineous_AnalogSignal")
public class Digineous_AnalogSignal extends BaseDigineousModel
{
    @FieldModelDescription(units = EngineeringUnits.no_units, temporalResolution = FieldTemporalResolution.Max1000Hz)
    public float value;

    //--//

    @Override
    public String extractBaseId()
    {
        return "Digineous_AnalogSignal";
    }

    @Override
    public String overrideIdentifier(String identifier)
    {
        return "value";
    }
}
