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
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;

@JsonTypeName("WorkflowDetailsForRenameDevice")
public class WorkflowDetailsForRenameDevice extends WorkflowDetails implements IWorkflowHandlerForCRE,
                                                                               IWorkflowHandlerForOverrides
{
    public List<WorkflowAsset> devices = Lists.newArrayList();

    public String deviceNewName;

    //--//

    public void setDeviceSysId(String deviceSysId)
    {
        WorkflowAsset eq = ensureDevice();
        eq.sysId = deviceSysId;
    }

    public void setDeviceName(String deviceName)
    {
        WorkflowAsset eq = ensureDevice();
        eq.name = deviceName;
    }

    private WorkflowAsset ensureDevice()
    {
        if (devices.size() == 1)
        {
            return devices.get(0);
        }

        WorkflowAsset device = new WorkflowAsset();
        devices.add(device);
        return device;
    }

    //--//

    @Override
    public WorkflowType resolveToType()
    {
        return WorkflowType.RenameDevice;
    }

    @Override
    public void onCreate(SessionHolder sessionHolder)
    {
    }

    @Override
    public boolean postWorkflowCreationForCRE(SessionHolder sessionHolder)
    {
        RecordHelper<AssetRecord> helper = sessionHolder.createHelper(AssetRecord.class);

        for (WorkflowAsset device : devices)
        {
            AssetRecord rec_device = helper.get(device.sysId);
            rec_device.setDisplayName(deviceNewName);
        }

        return true;
    }

    @Override
    public void getOverride(WorkflowOverrides overrides)
    {
        for (WorkflowAsset device : devices)
        {
            overrides.deviceNames.put(device.sysId, deviceNewName);
        }
    }
}
