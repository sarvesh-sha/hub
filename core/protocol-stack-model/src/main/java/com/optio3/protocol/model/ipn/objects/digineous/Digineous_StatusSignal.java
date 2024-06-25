/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.digineous;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.FieldTemporalResolution;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;

@JsonTypeName("Ipn:Digineous_StatusSignal")
public class Digineous_StatusSignal extends BaseDigineousModel
{
    @FieldModelDescription(units = EngineeringUnits.no_units, temporalResolution = FieldTemporalResolution.Max1000Hz)
    public boolean active;

    //--//

    @Override
    public String extractBaseId()
    {
        return "Digineous_StatusSignal";
    }

    @Override
    public String overrideIdentifier(String identifier)
    {
        return "active";
    }

    @Override
    public boolean isAbleToUpdateState(String identifier)
    {
        return identifier.startsWith("DO");
    }

    @Override
    public boolean updateState(Map<String, JsonNode> state)
    {
        boolean modified = super.updateState(state);

        JsonNode node = state.get(BACnetPropertyIdentifier.present_value.name());
        if (node != null)
        {
            active = node.asBoolean();
            modified = true;
        }

        return modified;
    }
}
