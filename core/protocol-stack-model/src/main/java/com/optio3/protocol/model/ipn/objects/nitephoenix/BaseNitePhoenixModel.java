/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.nitephoenix;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.can.CanObjectModel;
import com.optio3.protocol.model.ipn.IpnObjectPostProcess;
import com.optio3.protocol.model.ipn.IpnObjectsState;

@JsonSubTypes({ @JsonSubTypes.Type(value = BaseBatteryNitePhoenixModel.class), @JsonSubTypes.Type(value = NitePhoenix_ControlPanel.class), @JsonSubTypes.Type(value = NitePhoenix_HVAC_Unit.class) })
public abstract class BaseNitePhoenixModel extends CanObjectModel implements IpnObjectPostProcess<BaseNitePhoenixModel>
{
    @Override
    public boolean shouldCommitReachabilityChange(boolean isReachable,
                                                  ZonedDateTime lastReachable)
    {
        // When the system is not in use, it stops broadcasting. So ignore unreachable state.
        return false;
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        detailsForParent.equipmentClass = WellKnownEquipmentClass.HVAC.asWrapped();
    }

    @Override
    public void postProcess(IpnObjectsState state,
                            BaseNitePhoenixModel previousValue)
    {
        // A bit of a hack, because the HVAC unit only reports when it's on.
        // We still want to discover it when we see a NitePhoenix system.
        state.ensure(NitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs.class);
        state.ensure(NitePhoenix_BatteryMonitoringSystem_AuxiliaryBatteryParameters.class);
        state.ensure(NitePhoenix_BatteryMonitoringSystem_StartingBatteryParameters.class);
        state.ensure(NitePhoenix_ControlPanel.class);
        state.ensure(NitePhoenix_HVAC_Unit.class);
    }
}
