/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import java.util.List;

import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.persistence.SessionHolder;

public class WorkflowAsset
{
    public String sysId;
    public String key;
    public String name;

    public static void populateEquipmentDetails(SessionHolder sessionHolder,
                                                List<WorkflowAsset> assets)
    {
        if (assets == null)
        {
            return;
        }

        for (WorkflowAsset asset : assets)
        {
            populateEquipmentDetails(sessionHolder, asset);
        }
    }

    public static void populateEquipmentDetails(SessionHolder sessionHolder,
                                                WorkflowAsset asset)
    {
        if (asset != null)
        {
            AssetRecord rec_equip = sessionHolder.getEntity(AssetRecord.class, asset.sysId);
            asset.key  = rec_equip.getEquipmentKey();
            asset.name = rec_equip.getName();
        }
    }
}
