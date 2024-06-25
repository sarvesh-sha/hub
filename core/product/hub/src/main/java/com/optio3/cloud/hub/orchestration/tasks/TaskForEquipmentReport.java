/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.formatting.TabularField;
import com.optio3.cloud.formatting.TabularReportAsExcel;
import com.optio3.cloud.hub.logic.normalizations.EquipmentClass;
import com.optio3.cloud.hub.logic.normalizations.NormalizationRules;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.model.asset.AssetFilterRequest;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.hub.model.asset.EquipmentReportProgress;
import com.optio3.cloud.hub.model.tags.TagsConditionIsEquipment;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.LogicalAssetRecord;
import com.optio3.cloud.hub.persistence.asset.RelationshipRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.logic.IBackgroundActivityProgress;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;

public class TaskForEquipmentReport extends BaseReportTask implements IBackgroundActivityProgress<EquipmentReportProgress>
{
    private final static int    batchSize   = 10;
    private static final String chunk_ROWS  = "Rows";
    private static final String chunk_EXCEL = "Excel";

    static class RowForReport
    {
        @TabularField(order = 0, title = "Name")
        public String col_name;

        @TabularField(order = 1, title = "Equipment Class")
        public String col_equipClass;

        @TabularField(order = 2, title = "Location")
        public String col_location;

        @TabularField(order = 3, title = "# Child Equipment")
        public int col_numEquipment;

        @TabularField(order = 4, title = "# Control Points")
        public int col_numControlPoints;

        @TabularField(order = 5, title = "Last Updated", format = "yyyy-MM-dd hh:mm:ss")
        public ZonedDateTime col_lastUpdated;

        @TabularField(order = 6, title = "Creation Date", format = "yyyy-MM-dd hh:mm:ss")
        public ZonedDateTime col_created;

        @TabularField(order = 7, title = "guid")
        public String col_sysId;
    }

    static class RowsForReport
    {
        public final List<RowForReport> rows = Lists.newArrayList();
    }

    //--//

    public String parentEquipmentSysId;

    public List<String> assetIDs;

    public Map<String, String> equipmentClassLookup;

    public int     totalEquipment;
    public int     equipmentProcessed;
    public boolean generatingFile;

    //--//

    @Override
    public EquipmentReportProgress fetchProgress(SessionHolder sessionHolder,
                                                 boolean detailed)
    {
        EquipmentReportProgress results = new EquipmentReportProgress();
        results.totalEquipment     = totalEquipment;
        results.equipmentProcessed = equipmentProcessed;
        results.generatingFile     = generatingFile;

        return results;
    }

    @Override
    public void generateStream() throws
                                 IOException
    {
        try (var holder = new TabularReportAsExcel.Holder())
        {
            TabularReportAsExcel<RowForReport> tr = new TabularReportAsExcel<>(RowForReport.class, "Equipment", holder);

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
                                                        String parentEquipmentSysId) throws
                                                                                     Exception
    {
        return scheduleActivity(sessionHolder, 0, null, TaskForEquipmentReport.class, (t) ->
        {
            t.parentEquipmentSysId = parentEquipmentSysId;
        });
    }

    //--//

    @Override
    public String getTitle()
    {
        return "Equipment Report";
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
        if (assetIDs == null)
        {
            AssetFilterRequest filters = new AssetFilterRequest();
            filters.tagsQuery = new TagsConditionIsEquipment();
            if (parentEquipmentSysId != null)
            {
                List<String> parentIds = Lists.newArrayList();
                parentIds.add(this.parentEquipmentSysId);
                filters.parentIDs = parentIds;
            }
            List<RecordIdentity> records = AssetRecord.filterAssets(sessionHolder.createHelper(AssetRecord.class), filters);

            assetIDs           = CollectionUtils.transformToList(records, (id) -> id.sysId);
            totalEquipment     = assetIDs.size();
            equipmentProcessed = 0;

            TagsEngine         tagsEngine = sessionHolder.getService(TagsEngine.class);
            NormalizationRules rules      = tagsEngine.getActiveNormalizationRules(sessionHolder);
            if (rules != null)
            {
                List<EquipmentClass> classes = rules.equipmentClasses;
                equipmentClassLookup = Maps.newHashMap();
                for (EquipmentClass equipClass : classes)
                {
                    equipmentClassLookup.put(equipClass.idAsString(), equipClass.equipClassName + " - " + equipClass.description);
                }
            }

            flushStateToDatabase(sessionHolder);
        }

        ReportFlusher flusher = new ReportFlusher(1500);

        while (!assetIDs.isEmpty())
        {
            if (flusher.shouldReport())
            {
                flushStateToDatabase(sessionHolder);
            }

            List<String> nextBatch = Lists.newArrayList();
            int          lastIdx   = Math.max(assetIDs.size() - batchSize, 0);
            for (int i = assetIDs.size() - 1; i >= lastIdx; i--)
            {
                nextBatch.add(assetIDs.remove(i));
            }

            try (SessionHolder holder = sessionHolder.spawnNewReadOnlySession())
            {
                RowsForReport chunk = new RowsForReport();

                List<AssetRecord> equipmentBatch = AssetRecord.getAssetsBatch(holder.createHelper(AssetRecord.class), nextBatch);

                for (AssetRecord asset : equipmentBatch)
                {
                    LogicalAssetRecord equip = Reflection.as(asset, LogicalAssetRecord.class);
                    if (equip != null)
                    {
                        RowForReport row = new RowForReport();

                        row.col_name = equip.getName();

                        row.col_equipClass = equipmentClassLookup.getOrDefault(equip.getEquipmentClassId(), null);

                        LocationRecord loc = equip.getLocation();
                        if (loc != null)
                        {
                            row.col_location = loc.getName();
                        }

                        row.col_lastUpdated = equip.getUpdatedOn();
                        row.col_created     = equip.getCreatedOn();

                        row.col_sysId = equip.getSysId();

                        List<String> nestedAssetSysIds = RelationshipRecord.getChildren(sessionHolder, asset.getSysId(), AssetRelationship.controls);
                        for (AssetRecord nestedAsset : AssetRecord.getAssetsBatch(holder.createHelper(AssetRecord.class), nestedAssetSysIds))
                        {
                            if (SessionHolder.isEntityOfClass(nestedAsset, LogicalAssetRecord.class))
                            {
                                row.col_numEquipment++;
                            }
                            else if (SessionHolder.isEntityOfClass(nestedAsset, DeviceElementRecord.class))
                            {
                                row.col_numControlPoints++;
                            }
                        }

                        chunk.rows.add(row);
                    }

                    equipmentProcessed++;
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
