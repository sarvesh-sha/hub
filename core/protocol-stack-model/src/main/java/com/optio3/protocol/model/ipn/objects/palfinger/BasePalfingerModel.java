/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.palfinger;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.can.CanObjectModel;

@JsonSubTypes({ @JsonSubTypes.Type(value = Palfinger_AnalogInputs.class),
                @JsonSubTypes.Type(value = Palfinger_ApplicationSW_name.class),
                @JsonSubTypes.Type(value = Palfinger_ApplicationSW_version.class),
                @JsonSubTypes.Type(value = Palfinger_Counters.class),
                @JsonSubTypes.Type(value = Palfinger_DigitalStatusInputs.class),
                @JsonSubTypes.Type(value = Palfinger_DigitalStatusOutputs.class),
                @JsonSubTypes.Type(value = Palfinger_ManufacturerIdent1.class),
                @JsonSubTypes.Type(value = Palfinger_ManufacturerIdent2.class),
                @JsonSubTypes.Type(value = Palfinger_SupplyVoltage.class) })
public abstract class BasePalfingerModel extends CanObjectModel
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
        detailsForParent.equipmentClass = WellKnownEquipmentClass.Liftgate.asWrapped();
    }
}
