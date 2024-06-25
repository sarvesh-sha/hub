/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import java.util.List;
import java.util.Set;

import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.model.asset.DeviceElementFilterRequest;
import com.optio3.cloud.hub.model.asset.DeviceElementSampling;
import com.optio3.cloud.hub.model.tags.TagsConditionTerm;
import com.optio3.cloud.hub.orchestration.AbstractHubActivityHandler;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.BACnetDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;

public class TaskForSamplingSettings extends AbstractHubActivityHandler
{
    public RecordLocator<NetworkAssetRecord> loc_network;
    public boolean                           dryRun;
    public boolean                           startWithClassId;
    public boolean                           stopWithoutClassId;
    public boolean                           triggerConfiguration;

    public int started;
    public int stopped;

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        NetworkAssetRecord rec_network,
                                                        boolean dryRun,
                                                        boolean startWithClassId,
                                                        boolean stopWithoutClassId,
                                                        boolean triggerConfiguration) throws
                                                                                      Exception
    {
        return scheduleActivity(sessionHolder, 0, null, TaskForSamplingSettings.class, (newHandler) ->
        {
            newHandler.loc_network = sessionHolder.createLocator(rec_network);
            newHandler.dryRun = dryRun;
            newHandler.startWithClassId = startWithClassId;
            newHandler.stopWithoutClassId = stopWithoutClassId;
            newHandler.triggerConfiguration = triggerConfiguration;
        });
    }

    //--//

    @Override
    public String getTitle()
    {
        return "Bulk update of sampling settings";
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
            RecordHelper<BACnetDeviceRecord>  helper_device  = sessionHolder.createHelper(BACnetDeviceRecord.class);
            RecordHelper<DeviceElementRecord> helper_element = sessionHolder.createHelper(DeviceElementRecord.class);

            TagsEngine                                      tagsEngine          = getServiceNonNull(TagsEngine.class);
            TagsEngine.Snapshot                             tagsSnapshot        = tagsEngine.acquireSnapshot(true);
            TagsEngine.Snapshot.AssetSet                    withPointClassId    = tagsSnapshot.evaluateCondition(TagsConditionTerm.build(AssetRecord.WellKnownTags.pointClassId));
            Set<TypedRecordIdentity<? extends AssetRecord>> withPointClassIdSet = withPointClassId.resolve();

            if (startWithClassId)
            {
                rec_network.enumerateChildren(helper_device, true, -1, null, (rec_device) ->
                {
                    DeviceElementFilterRequest filters = DeviceElementFilterRequest.createFilterForParent(rec_device);
                    filters.hasNoSampling = true;

                    DeviceElementRecord.enumerate(helper_element, true, filters, (rec_object) ->
                    {
                        TypedRecordIdentity<DeviceElementRecord> ri = TypedRecordIdentity.newTypedInstance(rec_object);
                        if (withPointClassIdSet.contains(ri))
                        {
                            List<DeviceElementSampling> config = rec_device.prepareSamplingConfiguration(sessionHolder, rec_object, false);

                            if (rec_object.setSamplingSettings(config))
                            {
                                started++;

                                if (!dryRun)
                                {
                                    if ((started % 500) == 0)
                                    {
                                        return StreamHelperNextAction.Continue_Flush_Evict_Commit;
                                    }

                                    return StreamHelperNextAction.Continue_Flush_Evict;
                                }
                            }
                        }

                        // Nothing to change.
                        return StreamHelperNextAction.Continue_Evict;
                    });

                    return StreamHelperNextAction.Continue_Evict;
                });
            }

            if (stopWithoutClassId)
            {
                rec_network.enumerateChildren(helper_device, true, -1, null, (rec_device) ->
                {
                    DeviceElementFilterRequest filters = DeviceElementFilterRequest.createFilterForParent(rec_device);
                    filters.hasAnySampling = true;

                    DeviceElementRecord.enumerate(helper_element, true, filters, (rec_object) ->
                    {
                        TypedRecordIdentity<DeviceElementRecord> ri = TypedRecordIdentity.newTypedInstance(rec_object);
                        if (!withPointClassIdSet.contains(ri))
                        {
                            if (rec_object.setSamplingSettings(null))
                            {
                                stopped++;

                                if (!dryRun)
                                {
                                    if ((stopped % 500) == 0)
                                    {
                                        return StreamHelperNextAction.Continue_Flush_Evict_Commit;
                                    }

                                    return StreamHelperNextAction.Continue_Flush_Evict;
                                }
                            }
                        }

                        // Nothing to change.
                        return StreamHelperNextAction.Continue_Evict;
                    });

                    return StreamHelperNextAction.Continue_Evict;
                });
            }

            if (!dryRun)
            {
                boolean trigger = triggerConfiguration || started > 0 || stopped > 0;
                if (trigger)
                {
                    TaskForSamplingConfiguration.scheduleTaskIfNotRunning(sessionHolder, rec_network);
                }
            }
        }

        markAsCompleted();
    }
}
