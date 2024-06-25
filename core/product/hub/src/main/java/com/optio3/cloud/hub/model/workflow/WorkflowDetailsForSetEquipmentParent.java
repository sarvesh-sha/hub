/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.RelationshipRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;

@JsonTypeName("WorkflowDetailsForSetEquipmentParent")
public class WorkflowDetailsForSetEquipmentParent extends WorkflowDetails implements IWorkflowHandlerForCRE,
                                                                                     IWorkflowHandlerForOverrides
{
    public WorkflowAsset       parentEquipment;
    public List<WorkflowAsset> childEquipments = Lists.newArrayList();

    //--//

    public void setEquipmentParentKey(String equipmentKey)
    {
        WorkflowAsset eq = ensureParentEquipment();
        eq.key = equipmentKey;
    }

    public void setEquipmentParentSysId(String equipmentSysId)
    {
        WorkflowAsset eq = ensureParentEquipment();
        eq.sysId = equipmentSysId;
    }

    public void setEquipmentParentName(String equipmentName)
    {
        WorkflowAsset eq = ensureParentEquipment();
        eq.name = equipmentName;
    }

    public void setEquipmentChildKey(String equipmentKey)
    {
        WorkflowAsset eq = ensureChildEquipment();
        eq.key = equipmentKey;
    }

    public void setEquipmentChildSysId(String equipmentSysId)
    {
        WorkflowAsset eq = ensureChildEquipment();
        eq.sysId = equipmentSysId;
    }

    public void setEquipmentChildName(String equipmentName)
    {
        WorkflowAsset eq = ensureChildEquipment();
        eq.name = equipmentName;
    }

    private WorkflowAsset ensureParentEquipment()
    {
        if (parentEquipment == null)
        {
            parentEquipment = new WorkflowAsset();
        }

        return parentEquipment;
    }

    private WorkflowAsset ensureChildEquipment()
    {
        if (childEquipments.size() == 1)
        {
            return childEquipments.get(0);
        }

        WorkflowAsset equipment = new WorkflowAsset();
        childEquipments.add(equipment);
        return equipment;
    }

    //--//

    @Override
    public WorkflowType resolveToType()
    {
        return WorkflowType.SetEquipmentParent;
    }

    @Override
    public void onCreate(SessionHolder sessionHolder)
    {
        WorkflowAsset.populateEquipmentDetails(sessionHolder, parentEquipment);
        WorkflowAsset.populateEquipmentDetails(sessionHolder, childEquipments);
    }

    @Override
    public boolean postWorkflowCreationForCRE(SessionHolder sessionHolder)
    {
        RecordHelper<AssetRecord> helper = sessionHolder.createHelper(AssetRecord.class);

        AssetRecord rec_equipParent = parentEquipment != null ? helper.get(parentEquipment.sysId) : null;

        for (WorkflowAsset child : childEquipments)
        {
            AssetRecord rec_equipChild = helper.get(child.sysId);

            List<String>      parents         = RelationshipRecord.getParents(sessionHolder, child.sysId, AssetRelationship.controls);
            List<AssetRecord> existingParents = AssetRecord.getAssetsBatch(helper, parents);
            for (AssetRecord rec_parent : existingParents)
            {
                RelationshipRecord.removeRelation(sessionHolder, rec_parent, rec_equipChild, AssetRelationship.controls);
            }

            if (rec_equipParent != null)
            {
                RelationshipRecord.addRelation(sessionHolder, rec_equipParent, rec_equipChild, AssetRelationship.controls);
            }
        }

        return true;
    }

    @Override
    public void getOverride(WorkflowOverrides overrides)
    {
        for (WorkflowAsset child : childEquipments)
        {
            if (parentEquipment != null)
            {
                overrides.equipmentParents.put(child.key, parentEquipment.key);
            }
            else
            {
                overrides.equipmentParents.remove(child.key);
            }
        }
    }
}
