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
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;

@JsonTypeName("WorkflowDetailsForMergeEquipments")
public class WorkflowDetailsForMergeEquipments extends WorkflowDetails implements IWorkflowHandlerForCRE,
                                                                                  IWorkflowHandlerForOverrides
{
    public WorkflowAsset equipment1;
    public WorkflowAsset equipment2;

    //--//

    public void setEquipment1Key(String equipmentKey)
    {
        WorkflowAsset eq = ensureEquipment1();
        eq.key = equipmentKey;
    }

    public void setEquipment1SysId(String equipmentSysId)
    {
        WorkflowAsset eq = ensureEquipment1();
        eq.sysId = equipmentSysId;
    }

    public void setEquipment1Name(String equipmentName)
    {
        WorkflowAsset eq = ensureEquipment1();
        eq.name = equipmentName;
    }

    public void setEquipment2Key(String equipmentKey)
    {
        WorkflowAsset eq = ensureEquipment2();
        eq.key = equipmentKey;
    }

    public void setEquipment2SysId(String equipmentSysId)
    {
        WorkflowAsset eq = ensureEquipment2();
        eq.sysId = equipmentSysId;
    }

    public void setEquipment2Name(String equipmentName)
    {
        WorkflowAsset eq = ensureEquipment2();
        eq.name = equipmentName;
    }

    private WorkflowAsset ensureEquipment1()
    {
        if (equipment1 == null)
        {
            equipment1 = new WorkflowAsset();
        }

        return equipment1;
    }

    private WorkflowAsset ensureEquipment2()
    {
        if (equipment2 == null)
        {
            equipment2 = new WorkflowAsset();
        }

        return equipment2;
    }
    //--//

    @Override
    public WorkflowType resolveToType()
    {
        return WorkflowType.MergeEquipments;
    }

    @Override
    public void onCreate(SessionHolder sessionHolder)
    {
        WorkflowAsset.populateEquipmentDetails(sessionHolder, equipment1);
        WorkflowAsset.populateEquipmentDetails(sessionHolder, equipment2);
    }

    @Override
    public boolean postWorkflowCreationForCRE(SessionHolder sessionHolder)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(sessionHolder, false, true))
        {
            RecordHelper<AssetRecord> helper         = sessionHolder.createHelper(AssetRecord.class);
            AssetRecord               rec_equipment1 = helper.get(equipment1.sysId);
            AssetRecord               rec_equipment2 = helper.get(equipment2.sysId);

            List<String>      pointIds = RelationshipRecord.getChildren(sessionHolder, rec_equipment2.getSysId(), AssetRelationship.controls);
            List<AssetRecord> points   = AssetRecord.getAssetsBatch(helper, pointIds);

            for (AssetRecord rec_point : points)
            {
                RelationshipRecord.addRelation(sessionHolder, rec_equipment1, rec_point, AssetRelationship.controls);
            }

            rec_equipment2.remove(validation, helper);
        }
        catch (Throwable t)
        {
            return false;
        }

        return true;
    }

    @Override
    public void getOverride(WorkflowOverrides overrides)
    {
        overrides.equipmentMerge.put(equipment2.key, equipment1.key);
    }
}
