/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.asset.DeviceElementFilterRequest;
import com.optio3.cloud.hub.model.asset.DeviceElementSampling;
import com.optio3.cloud.hub.model.asset.DeviceTemplate;
import com.optio3.cloud.hub.model.asset.DevicesSamplingTemplate;
import com.optio3.cloud.hub.orchestration.AbstractHubActivityHandler;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.IpnDeviceRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.protocol.model.ipn.IpnObjectModel;

public class TaskForSamplingTemplate extends AbstractHubActivityHandler
{
    public DevicesSamplingTemplate settings;

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        DevicesSamplingTemplate settings) throws
                                                                                          Exception
    {
        return scheduleActivity(sessionHolder, 0, null, TaskForSamplingTemplate.class, (newHandler) ->
        {
            newHandler.settings = settings;
        });
    }

    //--//

    @Override
    public String getTitle()
    {
        return "Bulk update of sampling settings based on template";
    }

    @Override
    public RecordLocator<? extends RecordWithCommonFields> getContext()
    {
        return null;
    }

    @BackgroundActivityMethod(needsSession = true)
    public void process(SessionHolder sessionHolder) throws
                                                     Exception
    {
        RecordHelper<DeviceRecord>        helper_device  = sessionHolder.createHelper(DeviceRecord.class);
        RecordHelper<DeviceElementRecord> helper_element = sessionHolder.createHelper(DeviceElementRecord.class);
        AtomicInteger                     counter        = new AtomicInteger();

        DeviceRecord.enumerate(helper_device, true, -1, null, (rec_device) ->
        {
            if (SessionHolder.isEntityOfClass(rec_device, IpnDeviceRecord.class))
            {
                DeviceElementFilterRequest filters = DeviceElementFilterRequest.createFilterForParent(rec_device);

                DeviceElementRecord.enumerate(helper_element, true, filters, (rec_object) ->
                {
                    try
                    {
                        IpnObjectModel              obj    = rec_object.getTypedContents(IpnObjectModel.getObjectMapper(), IpnObjectModel.class);
                        List<DeviceElementSampling> config = Lists.newArrayList();

                        String deviceId  = DeviceTemplate.extractPath(obj);
                        String elementId = rec_object.getIdentifier();

                        Integer period = settings.lookup(deviceId, elementId);
                        if (period != null)
                        {
                            DeviceElementSampling.add(config, DeviceElementRecord.DEFAULT_PROP_NAME, period);
                        }

                        if (rec_object.setSamplingSettings(config))
                        {
                            HubApplication.LoggerInstance.info("%s sampling for %s %s/%s",
                                                               period != null ? "Enabled" : "Disabled",
                                                               rec_object.getSysId(),
                                                               rec_device.getIdentityDescriptor(),
                                                               rec_object.getIdentifier());

                            return counter.incrementAndGet() % 500 == 0 ? StreamHelperNextAction.Continue_Flush_Evict_Commit : StreamHelperNextAction.Continue_Flush_Evict;
                        }
                    }
                    catch (Exception e)
                    {
                        HubApplication.LoggerInstance.warn("Failed to decode object state for %s/%s, due to %s", rec_device.getIdentityDescriptor(), rec_object.getIdentifier(), e);
                    }

                    // Nothing to change.
                    return StreamHelperNextAction.Continue_Evict;
                });
            }

            return StreamHelperNextAction.Continue_Evict;
        });

        markAsCompleted();
    }
}
