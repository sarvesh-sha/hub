/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.simulator.generators;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.logic.simulator.SimulatedGateway;
import com.optio3.cloud.hub.model.normalization.DeviceElementClassificationOverrides;
import com.optio3.cloud.hub.model.normalization.NormalizationEquipment;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.util.CollectionUtils;

public class AHUGroup
{
    private AHUController       m_ahu;
    private List<VAVController> m_vavs;

    public AHUGroup(int ahuId)
    {
        m_ahu  = new AHUController(String.format("AHU-%d", ahuId), 111, ahuId);
        m_vavs = Lists.newArrayList();
        for (int i = 1; i <= 3; i++)
        {
            int instanceNumber = 100 * ahuId + i;
            m_vavs.add(new VAVController(String.format("VAV-%d", instanceNumber), "VAV-", 111, instanceNumber, 1));
        }

        m_vavs.add(new VAVController("VLC-" + ahuId, "VAV-", 111, ahuId * 100 + 4, 2));
    }

    public void register(SimulatedGateway gateway)
    {
        gateway.addDevice(m_ahu);
        for (VAVController controller : m_vavs)
        {
            gateway.addDevice(controller);
        }
    }

    public void persist(RecordHelper<AssetRecord> helper,
                        NetworkAssetRecord rec_network,
                        Map<String, DeviceElementClassificationOverrides> overrides) throws
                                                                                     Exception
    {
        m_ahu.persistDevice(helper, rec_network, null, overrides);

        NormalizationEquipment equipment = CollectionUtils.firstElement(m_ahu.getEquipments());

        for (VAVController vav : m_vavs)
        {
            vav.persistDevice(helper, rec_network, equipment, overrides);
        }
    }
}
