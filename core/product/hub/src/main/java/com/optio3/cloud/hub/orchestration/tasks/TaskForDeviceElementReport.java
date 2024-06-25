/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.formatting.TabularField;
import com.optio3.cloud.formatting.TabularReportAsExcel;
import com.optio3.cloud.hub.logic.normalizations.NormalizationRules;
import com.optio3.cloud.hub.logic.normalizations.PointClass;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.model.asset.DeviceElementFilterRequest;
import com.optio3.cloud.hub.model.asset.DeviceElementReportProgress;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.logic.IBackgroundActivityProgress;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;

public class TaskForDeviceElementReport extends BaseReportTask implements IBackgroundActivityProgress<DeviceElementReportProgress>
{
    private final static int    batchSize   = 10;
    private static final String chunk_ROWS  = "Rows";
    private static final String chunk_EXCEL = "Excel";

    static class RowForReport
    {
        @TabularField(order = 0, title = "Name")
        public String col_name;

        @TabularField(order = 1, title = "Point Class")
        public String col_pointClass;

        @TabularField(order = 2, title = "Location")
        public String col_location;

        @TabularField(order = 3, title = "Instance ID")
        public Integer col_instanceId;

        @TabularField(order = 4, title = "Network ID")
        public Integer col_networkId;

        @TabularField(order = 5, title = "Identifier")
        public String col_identifier;

        @TabularField(order = 6, title = "Is sampling")
        public boolean col_sampling;

        @TabularField(order = 7, title = "Raw Point Name")
        public String col_pointNameRaw;

        @TabularField(order = 8, title = "Point Name Backup")
        public String col_pointNameBackup;

        @TabularField(order = 9, title = "guid")
        public String col_sysId;
    }

    static class RowsForReport
    {
        public final List<RowForReport> rows = Lists.newArrayList();
    }

    //--//

    public String parentSysId;

    public List<String> deviceElementSysIds;

    public Map<String, String> pointClassLookup;

    public int     totalDeviceElements;
    public int     deviceElementsProcessed;
    public boolean generatingFile;

    //--//

    @Override
    public DeviceElementReportProgress fetchProgress(SessionHolder sessionHolder,
                                                     boolean detailed)
    {
        DeviceElementReportProgress results = new DeviceElementReportProgress();
        results.deviceElementsProcessed = deviceElementsProcessed;
        results.totalDeviceElements     = totalDeviceElements;
        results.generatingFile          = generatingFile;

        return results;
    }

    @Override
    public void generateStream() throws
                                 IOException
    {
        try (var holder = new TabularReportAsExcel.Holder())
        {
            TabularReportAsExcel<RowForReport> tr = new TabularReportAsExcel<>(RowForReport.class, "Control Points", holder);

            tr.emit(rowHandler ->
                    {
                        forEachChunkInSequence(chunk_ROWS, RowsForReport.class, (seq, chunk) ->
                        {
                            for (RowForReport row : chunk.rows)
                            {
                                rowHandler.emitRow(row);
                            }
                        });
                    });

            try (OutputStream outputStream = writeAsStream(chunk_EXCEL, 0))
            {
                holder.toStream(outputStream);
            }
        }
    }

    @Override
    public InputStream streamContents() throws
                                        IOException
    {
        return readAsStream(chunk_EXCEL);
    }

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        String parentSysId) throws
                                                                            Exception
    {
        return scheduleActivity(sessionHolder, 0, null, TaskForDeviceElementReport.class, (t) ->
        {
            t.parentSysId = requireNonNull(parentSysId);
        });
    }

    //--//

    @Override
    public String getTitle()
    {
        return "Device Element Report";
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
        if (deviceElementSysIds == null)
        {
            DeviceElementFilterRequest filters = new DeviceElementFilterRequest();
            filters.parentIDs = Lists.newArrayList(parentSysId);

            List<RecordIdentity> records = DeviceElementRecord.filterDeviceElements(sessionHolder.createHelper(DeviceElementRecord.class), filters);

            deviceElementSysIds     = CollectionUtils.transformToList(records, (id) -> id.sysId);
            totalDeviceElements     = deviceElementSysIds.size();
            deviceElementsProcessed = 0;

            TagsEngine         tagsEngine = sessionHolder.getService(TagsEngine.class);
            NormalizationRules rules      = tagsEngine.getActiveNormalizationRules(sessionHolder);
            if (rules != null)
            {
                List<PointClass> classes = rules.pointClasses;
                pointClassLookup = Maps.newHashMap();
                for (PointClass pointClass : classes)
                {
                    pointClassLookup.put(pointClass.idAsString(), pointClass.pointClassName + " - " + pointClass.pointClassDescription);
                }
            }

            flushStateToDatabase(sessionHolder);
        }

        ReportFlusher flusher = new ReportFlusher(1500);

        while (!deviceElementSysIds.isEmpty())
        {
            if (flusher.shouldReport())
            {
                // Reschedule to persist the state.
                flushStateToDatabase(sessionHolder);
            }

            List<String> nextBatch = Lists.newArrayList();
            int          lastIdx   = Math.max(deviceElementSysIds.size() - batchSize, 0);
            for (int i = deviceElementSysIds.size() - 1; i >= lastIdx; i--)
            {
                nextBatch.add(deviceElementSysIds.remove(i));
            }

            try (SessionHolder holder = sessionHolder.spawnNewReadOnlySession())
            {
                List<AssetRecord> deviceElementBatch = AssetRecord.getAssetsBatch(holder.createHelper(AssetRecord.class), nextBatch);

                RowsForReport chunk = new RowsForReport();

                for (AssetRecord asset : deviceElementBatch)
                {
                    DeviceElementRecord controlPoint = Reflection.as(asset, DeviceElementRecord.class);
                    if (controlPoint != null)
                    {
                        RowForReport row = new RowForReport();
                        row.col_name = controlPoint.getName();

                        row.col_pointClass = controlPoint.getPointClassId();

                        LocationRecord loc = controlPoint.getLocation();
                        if (loc != null)
                        {
                            row.col_location = loc.getName();
                        }

                        DeviceRecord parentDevice = controlPoint.getParentAssetOrNull(DeviceRecord.class);
                        if (parentDevice != null)
                        {
                            BaseAssetDescriptor    identityDescriptor = parentDevice.getIdentityDescriptor();
                            BACnetDeviceDescriptor deviceDescriptor   = Reflection.as(identityDescriptor, BACnetDeviceDescriptor.class);
                            if (deviceDescriptor != null)
                            {
                                row.col_instanceId = deviceDescriptor.address.instanceNumber;
                                row.col_networkId  = deviceDescriptor.address.networkNumber;
                            }
                        }

                        row.col_identifier = controlPoint.getIdentifier();

                        row.col_sampling = controlPoint.hasSamplingSettings();

                        try
                        {
                            BACnetObjectModel model        = controlPoint.getTypedContents(BACnetObjectModel.getObjectMapper(), BACnetObjectModel.class);
                            Object            pointNameObj = model.getValue(BACnetPropertyIdentifier.object_name, null);
                            if (pointNameObj != null)
                            {
                                row.col_pointNameRaw = pointNameObj.toString();
                            }
                        }
                        catch (Exception e)
                        {
                            // no 'object_name' - no raw point name
                        }

                        row.col_pointNameBackup = controlPoint.getLogicalName();

                        row.col_sysId = controlPoint.getSysId();

                        chunk.rows.add(row);
                    }

                    deviceElementsProcessed++;
                }

                addChunkToSequence(chunk_ROWS, chunk);
            }
        }

        generatingFile = true;

        flushStateToDatabase(sessionHolder);

        generateStream();

        markAsCompleted();
    }
}
