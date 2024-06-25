/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.optio3.cloud.hub.model.asset.DeviceElementFilterRequest;
import com.optio3.cloud.hub.model.asset.DeviceElementSampling;
import com.optio3.cloud.hub.orchestration.AbstractHubActivityHandler;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;

public class TaskForSamplingPeriod extends AbstractHubActivityHandler
{
    public RecordLocator<NetworkAssetRecord> loc_network;

    public int previousPeriod;
    public int currentPeriod;

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        NetworkAssetRecord rec_network,
                                                        int previousPeriod,
                                                        int currentPeriod) throws
                                                                           Exception
    {
        return scheduleActivity(sessionHolder, 0, null, TaskForSamplingPeriod.class, (newHandler) ->
        {
            newHandler.loc_network = sessionHolder.createLocator(rec_network);
            newHandler.previousPeriod = previousPeriod;
            newHandler.currentPeriod = currentPeriod;
        });
    }

    //--//

    @Override
    public String getTitle()
    {
        return "Bulk update of sampling period";
    }

    @Override
    public RecordLocator<? extends RecordWithCommonFields> getContext()
    {
        return loc_network;
    }

    @BackgroundActivityMethod(needsSession = true)
    public void process(SessionHolder sessionHolder) throws
                                                     Exception
    {
        NetworkAssetRecord rec_network = sessionHolder.fromLocatorOrNull(loc_network);
        if (rec_network != null)
        {
            RecordHelper<DeviceRecord>        helper_device  = sessionHolder.createHelper(DeviceRecord.class);
            RecordHelper<DeviceElementRecord> helper_element = sessionHolder.createHelper(DeviceElementRecord.class);
            AtomicBoolean                     changed        = new AtomicBoolean();

            rec_network.enumerateChildren(helper_device, true, -1, null, (rec_device) ->
            {
                DeviceElementFilterRequest filters = DeviceElementFilterRequest.createFilterForParent(rec_device);
                filters.hasAnySampling = true;

                DeviceElementRecord.enumerate(helper_element, true, filters, (rec_object) ->
                {
                    List<DeviceElementSampling> config = rec_object.getSamplingSettings();
                    if (config != null)
                    {
                        for (DeviceElementSampling elementSampling : config)
                        {
                            if (elementSampling.samplingPeriod == previousPeriod)
                            {
                                elementSampling.samplingPeriod = currentPeriod;
                            }
                        }

                        if (rec_object.setSamplingSettings(config))
                        {
                            changed.set(true);

                            return StreamHelperNextAction.Continue_Flush_Evict;
                        }
                    }

                    return StreamHelperNextAction.Continue_Evict;
                });

                return StreamHelperNextAction.Continue_Evict;
            });

            if (changed.get())
            {
                rec_network.reconfigureSampling(sessionHolder);
            }
        }

        markAsCompleted();
    }
}
