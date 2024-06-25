/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.stealthpower;

import java.time.ZonedDateTime;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.ipn.IpnObjectModel;

@JsonSubTypes({ @JsonSubTypes.Type(value = StealthPower_AMR.class),
                @JsonSubTypes.Type(value = StealthPower_CAPMETRO.class),
                @JsonSubTypes.Type(value = StealthPower_FDNY.class),
                @JsonSubTypes.Type(value = StealthPower_MTA.class),
                @JsonSubTypes.Type(value = StealthPower_PEP.class),
                @JsonSubTypes.Type(value = StealthPower_PortAuthority.class),
                @JsonSubTypes.Type(value = StealthPower_PSEG.class) })
public abstract class BaseStealthPowerModel extends IpnObjectModel
{
    public String firmware_version;

    public String event_type;

    //--//

    @Override
    public boolean shouldCommitReachabilityChange(boolean isReachable,
                                                  ZonedDateTime lastReachable)
    {
        // The MCU is not always on. The debouncing on the Hub side should remove the noise.
        return true;
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        detailsForParent.equipmentClass = WellKnownEquipmentClass.NoIdle.asWrapped();
    }

    public void handleFirmwareUpgrade(BiConsumer<Class<? extends BaseStealthPowerModel>, String> callback)
    {
        callback.accept(getClass(), firmware_version);
    }
}
