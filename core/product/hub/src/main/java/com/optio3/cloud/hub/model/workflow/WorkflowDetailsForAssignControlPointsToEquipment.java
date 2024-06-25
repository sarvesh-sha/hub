/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.RelationshipRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;

@JsonTypeName("WorkflowDetailsForAssignControlPointsToEquipment")
public class WorkflowDetailsForAssignControlPointsToEquipment extends WorkflowDetails implements IWorkflowHandlerForOverrides,
                                                                                                 IWorkflowHandlerForCRE
{
    public WorkflowAsset equipment;

    public List<String> controlPoints;

    //--//

    public void setEquipmentKey(String equipmentKey)
    {
        WorkflowAsset eq = ensureEquipment();
        eq.key = equipmentKey;
    }

    public void setEquipmentSysId(String equipmentSysId)
    {
        WorkflowAsset eq = ensureEquipment();
        eq.sysId = equipmentSysId;
    }

    public void setEquipmentName(String equipmentName)
    {
        WorkflowAsset eq = ensureEquipment();
        eq.name = equipmentName;
    }

    private WorkflowAsset ensureEquipment()
    {
        if (equipment == null)
        {
            equipment = new WorkflowAsset();
        }

        return equipment;
    }

    //--//

    @Override
    public WorkflowType resolveToType()
    {
        return WorkflowType.AssignControlPointsToEquipment;
    }

    @Override
    public void onCreate(SessionHolder sessionHolder)
    {
        WorkflowAsset.populateEquipmentDetails(sessionHolder, equipment);
    }

    @Override
    public boolean postWorkflowCreationForCRE(SessionHolder sessionHolder)
    {
        RecordHelper<AssetRecord> helper = sessionHolder.createHelper(AssetRecord.class);

        AssetRecord       rec_equip = helper.getOrNull(equipment != null ? equipment.sysId : null);
        List<AssetRecord> points    = AssetRecord.getAssetsBatch(helper, controlPoints);

        for (AssetRecord rec_point : points)
        {
            List<String>      parents         = RelationshipRecord.getParents(sessionHolder, rec_point.getSysId(), AssetRelationship.controls);
            List<AssetRecord> existingParents = AssetRecord.getAssetsBatch(helper, parents);
            for (AssetRecord rec_parent : existingParents)
            {
                RelationshipRecord.removeRelation(sessionHolder, rec_parent, rec_point, AssetRelationship.controls);
            }

            if (rec_equip != null)
            {
                RelationshipRecord.addRelation(sessionHolder, rec_equip, rec_point, AssetRelationship.controls);
            }
        }

        return true;
    }

    @Override
    public void getOverride(WorkflowOverrides overrides)
    {
        for (String sysId : controlPoints)
        {
            if (sysId != null)
            {
                overrides.pointParents.put(sysId, equipment != null ? equipment.key : null);
            }
        }
    }
}
