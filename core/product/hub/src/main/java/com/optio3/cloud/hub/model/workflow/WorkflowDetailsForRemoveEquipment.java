/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;

@JsonTypeName("WorkflowDetailsForRemoveEquipment")
public class WorkflowDetailsForRemoveEquipment extends WorkflowDetails implements IWorkflowHandlerForCRE,
                                                                                  IWorkflowHandlerForOverrides
{
    public List<WorkflowAsset> equipments = Lists.newArrayList();

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
        if (equipments.size() == 1)
        {
            return equipments.get(0);
        }

        WorkflowAsset equipment = new WorkflowAsset();
        equipments.add(equipment);
        return equipment;
    }

    //--//

    @Override
    public WorkflowType resolveToType()
    {
        return WorkflowType.RemoveEquipment;
    }

    @Override
    public void onCreate(SessionHolder sessionHolder)
    {
        WorkflowAsset.populateEquipmentDetails(sessionHolder, equipments);
    }

    @Override
    public boolean postWorkflowCreationForCRE(SessionHolder sessionHolder)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(sessionHolder, false, true))
        {
            RecordHelper<AssetRecord> helper = sessionHolder.createHelper(AssetRecord.class);
            for (WorkflowAsset equipment : equipments)
            {
                AssetRecord rec_equipment = helper.get(equipment.sysId);
                rec_equipment.remove(validation, helper);
            }
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
        for (WorkflowAsset equipment : equipments)
        {
            overrides.removedEquipment.add(equipment.key);
        }
    }
}
