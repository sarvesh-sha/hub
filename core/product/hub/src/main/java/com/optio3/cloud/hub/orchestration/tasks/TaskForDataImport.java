/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.optio3.cloud.exception.NotImplementedException;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.model.asset.DeviceElementFilterRequest;
import com.optio3.cloud.hub.model.dataImports.DataImportProgress;
import com.optio3.cloud.hub.orchestration.AbstractHubActivityHandler;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.BACnetDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.normalization.ImportedMetadataRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.logic.IBackgroundActivityProgress;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.metadata.normalization.BACnetBulkRenamingData;
import com.optio3.metadata.normalization.BACnetImportExportData;
import com.optio3.metadata.normalization.ImportExportData;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class TaskForDataImport extends AbstractHubActivityHandler implements IBackgroundActivityProgress<DataImportProgress>
{
    public RecordLocator<ImportedMetadataRecord> loc_source;

    public List<RecordLocator<DeviceRecord>> targets;

    public DataImportProgress results;

    //--//

    @Override
    public DataImportProgress fetchProgress(SessionHolder sessionHolder,
                                            boolean detailed)
    {
        return results;
    }

    @Override
    public void generateStream() throws
                                 IOException
    {
        // Nothing to do.
    }

    @Override
    public InputStream streamContents()
    {
        throw new NotImplementedException("Not supported");
    }

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        ImportedMetadataRecord source,
                                                        List<RecordLocator<DeviceRecord>> locators) throws
                                                                                                    Exception
    {
        return scheduleActivity(sessionHolder, 0, null, TaskForDataImport.class, (t) ->
        {
            t.loc_source = sessionHolder.createLocator(source);
            t.targets = locators;

            t.results = new DataImportProgress();
            t.results.devicesToProcess = locators.size();
        });
    }

    //--//

    @Override
    public String getTitle()
    {
        return "Import BAS information";
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
        MonotonousTime batchTimeout = TimeUtils.computeTimeoutExpiration(5, TimeUnit.SECONDS);

        ImportedMetadataRecord rec_source = sessionHolder.fromLocator(loc_source);

        LocationsEngine          locationsEngine   = sessionHolder.getServiceNonNull(LocationsEngine.class);
        LocationsEngine.Snapshot locationsSnapshot = locationsEngine.acquireSnapshot(true);

        while (!targets.isEmpty())
        {
            if (TimeUtils.isTimeoutExpired(batchTimeout))
            {
                // Reschedule to persist the state.
                flushStateToDatabase(sessionHolder);
            }

            RecordLocator<DeviceRecord> loc_target = targets.remove(targets.size() - 1);

            DeviceRecord rec_target = sessionHolder.fromLocator(loc_target);

            results.devicesProcessed++;

            Multimap<ImportExportData, ImportExportData> multimap      = TreeMultimap.create((ImportExportData o1, ImportExportData o2) -> o1.compareTo(o2, false), Ordering.arbitrary());
            Multimap<ImportExportData, ImportExportData> multimapFuzzy = TreeMultimap.create((ImportExportData o1, ImportExportData o2) -> o1.compareTo(o2, true), Ordering.arbitrary());

            for (ImportExportData importExportData : rec_source.getMetadata())
            {
                multimap.put(importExportData, importExportData);
                multimapFuzzy.put(importExportData, importExportData);
            }

            HubApplication.LoggerInstance.info("Processing %s: %s (%d/%d)", rec_target.getSysId(), rec_target.getName(), results.devicesProcessed, results.devicesToProcess);

            final DeviceElementFilterRequest filters = DeviceElementFilterRequest.createFilterForParent(rec_target);
            DeviceElementRecord.enumerateNoNesting(sessionHolder.createHelper(DeviceElementRecord.class), filters, (rec_object) ->
            {
                results.elementsProcessed++;

                boolean modified = processObjects(locationsSnapshot, multimap, multimapFuzzy, rec_object);
                if (modified)
                {
                    results.elementsModified++;
                    return StreamHelperNextAction.Continue_Flush_Evict;
                }

                return StreamHelperNextAction.Continue_Evict;
            });
        }

        markAsCompleted();
    }

    private boolean processObjects(LocationsEngine.Snapshot locationsSnapshot,
                                   Multimap<ImportExportData, ImportExportData> map,
                                   Multimap<ImportExportData, ImportExportData> mapFuzzy,
                                   DeviceElementRecord rec_object)
    {
        boolean modified = false;

        DeviceRecord       rec_device        = rec_object.getParentAsset(DeviceRecord.class);
        BACnetDeviceRecord rec_device_bacnet = Reflection.as(rec_device, BACnetDeviceRecord.class);
        if (rec_device_bacnet != null)
        {
            ImportExportData item = rec_device_bacnet.extractImportExportData(locationsSnapshot, rec_object);

            ImportExportData importExportData = findImportExportData(map.get(item), item);
            if (importExportData == null)
            {
                importExportData = findImportExportData(mapFuzzy.get(item), item);
            }

            BACnetBulkRenamingData renamingData = Reflection.as(importExportData, BACnetBulkRenamingData.class);
            if (renamingData != null)
            {
                rec_object.setIdentifier(renamingData.objectIdNew.toJsonValue());
                modified = true;
            }
            else if (importExportData instanceof BACnetImportExportData)
            {
                MetadataMap metadata = rec_object.getMetadata();
                if (StringUtils.isNotEmpty(importExportData.dashboardName))
                {
                    AssetRecord.WellKnownMetadata.nameFromLegacyImport.put(metadata, importExportData.dashboardName);
                }

                if (StringUtils.isNotEmpty(importExportData.dashboardEquipmentName))
                {
                    AssetRecord.WellKnownMetadata.equipmentNameFromLegacyImport.put(metadata, importExportData.dashboardEquipmentName);
                }

                if (CollectionUtils.isNotEmpty(importExportData.dashboardStructure))
                {
                    AssetRecord.WellKnownMetadata.structureFromLegacyImport.put(metadata, importExportData.dashboardStructure);
                }

                modified |= rec_object.setMetadata(metadata);
            }
        }

        return modified;
    }

    private ImportExportData findImportExportData(Collection<ImportExportData> candidates,
                                                  ImportExportData target)
    {
        ImportExportData bestCandidate = null;
        for (ImportExportData candidate : candidates)
        {
            BACnetImportExportData target_bacnet    = Reflection.as(target, BACnetImportExportData.class);
            BACnetImportExportData candidate_bacnet = Reflection.as(candidate, BACnetImportExportData.class);
            if (candidate_bacnet != null && target_bacnet != null)
            {
                if (candidate_bacnet.transport != null && target_bacnet.transport != null)
                {
                    if (Objects.equals(candidate_bacnet.transport, target_bacnet.transport))
                    {
                        return candidate;
                    }

                    continue;
                }
            }

            if (bestCandidate == null)
            {
                bestCandidate = candidate;
            }
        }

        return bestCandidate;
    }
}
