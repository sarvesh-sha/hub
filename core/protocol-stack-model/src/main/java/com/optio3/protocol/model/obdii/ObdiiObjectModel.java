/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.can.CanObjectModel;
import com.optio3.protocol.model.obdii.iso15765.BaseIso15765ObjectModel;
import com.optio3.protocol.model.obdii.pgn.BasePgnObjectModel;
import org.apache.commons.lang3.StringUtils;

@JsonSubTypes({ @JsonSubTypes.Type(value = BaseIso15765ObjectModel.class),
                @JsonSubTypes.Type(value = BasePgnObjectModel.class),
                @JsonSubTypes.Type(value = UnknownPdu.class),
                @JsonSubTypes.Type(value = VehicleIdentification.class) })
public abstract class ObdiiObjectModel extends CanObjectModel
{
    @JsonIgnore
    public int sourceAddress;

    @JsonIgnore
    public int destinationAddress;

    //--//

    @Override
    public boolean shouldCommitReachabilityChange(boolean isReachable,
                                                  ZonedDateTime lastReachable)
    {
        // OBD only reports values when vehicle is turned on. Reachability checks add too much noise.
        return false;
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        detailsForParent.equipmentClass   = WellKnownEquipmentClass.OnBoardDiagnostics.asWrapped();
        detailsForParent.instanceSelector = Integer.toString(sourceAddress);
    }

    @Override
    protected BaseObjectModel createEmptyCopy()
    {
        ObdiiObjectModel copy = (ObdiiObjectModel) super.createEmptyCopy();
        copy.sourceAddress      = sourceAddress;
        copy.destinationAddress = destinationAddress;
        return copy;
    }

    @Override
    public String extractUnitId()
    {
        return Integer.toString(sourceAddress);
    }

    @Override
    public boolean parseId(String id)
    {
        final String baseId = extractBaseId();
        if (StringUtils.startsWith(id, baseId))
        {
            String[] parts = StringUtils.split(id, '/');
            if (parts.length == 2 && StringUtils.equals(baseId, parts[0]))
            {
                try
                {
                    sourceAddress = Integer.parseInt(parts[1]);
                    return true;
                }
                catch (NumberFormatException e)
                {
                    // Not a valid id.
                }
            }
        }

        return false;
    }

    public void postDecodeFixup()
    {
        // Nothing to do by default.
    }
}
