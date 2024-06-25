/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.hub.model.normalization.EquipmentClassificationMetadata;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.LogicalAssetRecord;
import com.optio3.cloud.hub.persistence.asset.RelationshipRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.IdGenerator;

@JsonTypeName("WorkflowDetailsForNewEquipment")
public class WorkflowDetailsForNewEquipment extends WorkflowDetails implements IWorkflowHandlerForCRE,
                                                                               IWorkflowHandlerForOverrides
{
    public String equipmentKey;
    public String equipmentName;

    public String equipmentClassId;

    public String locationSysId;

    public WorkflowAsset parentEquipment;

    //--//

    @Override
    public WorkflowType resolveToType()
    {
        return WorkflowType.NewEquipment;
    }

    @Override
    public void onCreate(SessionHolder sessionHolder)
    {
        equipmentKey = IdGenerator.newGuid();
        WorkflowAsset.populateEquipmentDetails(sessionHolder, parentEquipment);
    }

    @Override
    public boolean postWorkflowCreationForCRE(SessionHolder holder)
    {
        RecordHelper<LogicalAssetRecord> helper          = holder.createHelper(LogicalAssetRecord.class);
        RecordHelper<LocationRecord>     helper_location = holder.createHelper(LocationRecord.class);

        LocationRecord rec_location = helper_location.getOrNull(locationSysId);

        LogicalAssetRecord rec_equipment = new LogicalAssetRecord();
        rec_equipment.setPhysicalName(equipmentName);
        rec_equipment.setLocation(rec_location);

        MetadataMap                     metadata       = rec_equipment.getMetadata();
        EquipmentClassificationMetadata classification = EquipmentClassificationMetadata.fromMetadata(metadata);
        classification.equipmentKey     = equipmentKey;
        classification.equipmentClassId = equipmentClassId;

        classification.saveToMetadata(metadata);
        rec_equipment.setMetadata(metadata);

        helper.persist(rec_equipment);

        AssetRecord rec_equipParent = parentEquipment != null ? helper.get(parentEquipment.sysId) : null;

        if (rec_equipParent != null)
        {
            RelationshipRecord.addRelation(holder, rec_equipParent, rec_equipment, AssetRelationship.controls);
        }

        return true;
    }

    @Override
    public void getOverride(WorkflowOverrides overrides)
    {
        overrides.createdEquipment.add(equipmentKey);

        if (locationSysId != null)
        {
            overrides.equipmentLocations.put(equipmentKey, locationSysId);
        }

        if (equipmentClassId != null)
        {
            overrides.equipmentClasses.put(equipmentKey, equipmentClassId);
        }

        if (parentEquipment != null)
        {
            overrides.equipmentParents.put(equipmentKey, parentEquipment.key);
        }
    }
}
