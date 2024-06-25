/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.cloud.hub.model.asset.DeviceElementSampling;
import com.optio3.cloud.hub.orchestration.tasks.TaskForSamplingSettings;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;

@JsonTypeName("WorkflowDetailsForSamplingPeriod")
public class WorkflowDetailsForSamplingPeriod extends WorkflowDetails implements IWorkflowHandlerForCRE,
                                                                                 IWorkflowHandlerForOverrides
{
    public List<String> controlPoints = Lists.newArrayList();

    public int samplingPeriod;

    //--//

    @Override
    public WorkflowType resolveToType()
    {
        return WorkflowType.SamplingPeriod;
    }

    @Override
    public void onCreate(SessionHolder sessionHolder)
    {
    }

    @Override
    public boolean postWorkflowCreationForCRE(SessionHolder sessionHolder)
    {
        RecordHelper<DeviceElementRecord> helper = sessionHolder.createHelper(DeviceElementRecord.class);

        Set<NetworkAssetRecord> touchedNetworks = Sets.newHashSet();

        try
        {
            if (samplingPeriod > 0)
            {
                for (String pointSysId : controlPoints)
                {
                    DeviceElementRecord rec_point = helper.getOrNull(pointSysId);
                    if (rec_point != null)
                    {
                        NetworkAssetRecord rec_network = rec_point.findParentAssetRecursively(NetworkAssetRecord.class);
                        touchedNetworks.add(rec_network);

                        List<DeviceElementSampling> config = rec_point.getSamplingSettings();
                        if (config != null)
                        {
                            for (DeviceElementSampling sampling : config)
                            {
                                sampling.samplingPeriod = samplingPeriod;
                            }

                            rec_point.setSamplingSettings(config);
                        }
                    }
                }

                for (NetworkAssetRecord rec_network : touchedNetworks)
                {
                    TaskForSamplingSettings.scheduleTask(sessionHolder, rec_network, false, false, false, true);
                }
            }

            return true;
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    @Override
    public void getOverride(WorkflowOverrides overrides)
    {
        for (String point : controlPoints)
        {
            overrides.pointSamplingPeriods.put(point, samplingPeriod);
        }
    }
}
