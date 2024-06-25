/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.model.asset.GatewayFilterRequest;
import com.optio3.cloud.hub.model.asset.NetworkFilterRequest;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.protocol.model.WellKnownEquipmentClass;

@JsonTypeName("WorkflowDetailsForRenameEquipment")
public class WorkflowDetailsForRenameEquipment extends WorkflowDetails implements IWorkflowHandlerForCRE,
                                                                                  IWorkflowHandlerForOverrides,
                                                                                  IWorkflowHandlerForTransportation
{
    public List<WorkflowAsset> equipments = Lists.newArrayList();

    public String equipmentNewName;

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
        return WorkflowType.RenameEquipment;
    }

    @Override
    public void onCreate(SessionHolder sessionHolder)
    {
        WorkflowAsset.populateEquipmentDetails(sessionHolder, equipments);
    }

    @Override
    public boolean postWorkflowCreationForCRE(SessionHolder sessionHolder)
    {
        RecordHelper<AssetRecord> helper = sessionHolder.createHelper(AssetRecord.class);

        for (WorkflowAsset equipment : equipments)
        {
            AssetRecord rec_equipment = helper.get(equipment.sysId);
            rec_equipment.setPhysicalName(equipmentNewName);
        }

        return true;
    }

    //--//

    @Override
    public void getOverride(WorkflowOverrides overrides)
    {
        for (WorkflowAsset equipment : equipments)
        {
            overrides.equipmentNames.put(equipment.key, equipmentNewName);
        }
    }

    //--//

    @Override
    public boolean postWorkflowCreationForTransportation(SessionHolder sessionHolder)
    {
        for (WorkflowAsset equipment : equipments)
        {
            AssetRecord rec_asset = sessionHolder.getEntity(AssetRecord.class, equipment.sysId);
            if (rec_asset.getWellKnownEquipmentClass() == WellKnownEquipmentClass.Deployment)
            {
                rec_asset.setDisplayName(equipmentNewName);

                final LocationRecord rec_location = rec_asset.getLocation();
                if (rec_location != null)
                {
                    rec_location.setDisplayName(equipmentNewName);

                    GatewayFilterRequest gatewayFilters = new GatewayFilterRequest();
                    gatewayFilters.locationIDs = Lists.newArrayList(rec_location.getSysId());
                    for (RecordIdentity ri_gateway : GatewayAssetRecord.filterGateways(sessionHolder.createHelper(GatewayAssetRecord.class), gatewayFilters))
                    {
                        GatewayAssetRecord rec_gateway = sessionHolder.getEntity(GatewayAssetRecord.class, ri_gateway.sysId);
                        rec_gateway.setDisplayName("Gateway for " + equipmentNewName);
                    }

                    NetworkFilterRequest networkFilters = new NetworkFilterRequest();
                    networkFilters.locationIDs = Lists.newArrayList(rec_location.getSysId());
                    for (RecordIdentity ri_network : NetworkAssetRecord.filterNetworks(sessionHolder.createHelper(NetworkAssetRecord.class), networkFilters))
                    {
                        NetworkAssetRecord rec_network = sessionHolder.getEntity(NetworkAssetRecord.class, ri_network.sysId);
                        rec_network.setDisplayName("Network for " + equipmentNewName);
                    }
                }
            }
        }

        return true;
    }
}
