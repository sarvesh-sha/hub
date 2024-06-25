/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.bluesky;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.ipn.IpnObjectModel;

@JsonSubTypes({ @JsonSubTypes.Type(value = BlueSky_DisplayUnitNeeds.class),
                @JsonSubTypes.Type(value = BlueSky_DisplayUnitRequest.class),
                @JsonSubTypes.Type(value = BlueSky_DuskAndDawnRequest.class),
                @JsonSubTypes.Type(value = BlueSky_DuskAndDawnValues.class),
                @JsonSubTypes.Type(value = BlueSky_MasterRequest.class),
                @JsonSubTypes.Type(value = BlueSky_MasterSetpoints.class),
                @JsonSubTypes.Type(value = BlueSky_MasterSetpointsRequest.class),
                @JsonSubTypes.Type(value = BlueSky_MasterValues.class),
                @JsonSubTypes.Type(value = BlueSky_ProRemoteTransmit.class),
                @JsonSubTypes.Type(value = BlueSky_ProRemoteValues.class),
                @JsonSubTypes.Type(value = BlueSky_UnitValues.class) })
public abstract class BaseBlueSkyObjectModel extends IpnObjectModel
{
    @Override
    public boolean shouldCommitReachabilityChange(boolean isReachable,
                                                  ZonedDateTime lastReachable)
    {
        return true;
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        detailsForParent.equipmentClass = WellKnownEquipmentClass.ChargeController.asWrapped();
    }

    public abstract boolean postDecodingValidation();

    protected static boolean isAcceptableRange(float v,
                                               float min,
                                               float max)
    {
        return min <= v && v <= max;
    }

    protected static float capToAcceptableRange(float v,
                                                float min,
                                                float max)
    {
        if (v < min)
        {
            return min;
        }

        if (v > max)
        {
            return max;
        }

        return v;
    }
}
