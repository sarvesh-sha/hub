/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.victron;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.ipn.IpnObjectModel;

@JsonSubTypes({ @JsonSubTypes.Type(value = Victron_RealTimeData.class) })
public abstract class BaseVictronModel extends IpnObjectModel
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
}
