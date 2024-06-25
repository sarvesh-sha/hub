/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.formatting.TabularField;
import com.optio3.cloud.formatting.TabularReportAsExcel;
import com.optio3.cloud.hub.model.asset.DeviceFilterRequest;
import com.optio3.cloud.hub.model.asset.DevicesReportProgress;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.logic.IBackgroundActivityProgress;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;

public class TaskForDevicesReport extends BaseReportTask implements IBackgroundActivityProgress<DevicesReportProgress>
{
    private static final int    batchSize   = 25;
    private static final String chunk_ROWS  = "Rows";
    private static final String chunk_EXCEL = "Excel";

    static class RowForReport
    {
        @TabularField(order = 0, title = "Name")
        public String col_name;

        @TabularField(order = 1, title = "Identifier")
        public String col_identifier;

        @TabularField(order = 2, title = "Transport")
        public String col_transport;

        @TabularField(order = 3, title = "Address")
        public String col_address;

        @TabularField(order = 4, title = "Product")
        public String col_product;

        @TabularField(order = 5, title = "Model #")
        public String col_modelNum;

        @TabularField(order = 6, title = "Manufacturer")
        public String col_manufacturer;

        @TabularField(order = 7, title = "Location")
        public String col_location;
    }

    static class RowsForReport
    {
        public final List<RowForReport> rows = Lists.newArrayList();
    }

    //--//

    public List<String> assetIDs;

    public int     totalDevices;
    public int     devicesProcessed;
    public boolean generatingFile;

    //--//

    @Override
    public DevicesReportProgress fetchProgress(SessionHolder sessionHolder,
                                               boolean detailed)
    {
        DevicesReportProgress results = new DevicesReportProgress();
        results.totalDevices     = totalDevices;
        results.devicesProcessed = devicesProcessed;
        results.generatingFile   = generatingFile;

        return results;
    }

    @Override
    public void generateStream() throws
                                 IOException
    {
        try (var holder = new TabularReportAsExcel.Holder())
        {
            TabularReportAsExcel<RowForReport> tr = new TabularReportAsExcel<>(RowForReport.class, "Devices", holder);

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

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder) throws
                                                                                     Exception
    {
        return scheduleActivity(sessionHolder, 0, null, TaskForDevicesReport.class, (t) ->
        {
        });
    }

    //--//

    @Override
    public String getTitle()
    {
        return "Devices Report";
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
            List<RecordIdentity> records = DeviceRecord.filterDevices(sessionHolder.createHelper(DeviceRecord.class), new DeviceFilterRequest());

            assetIDs         = CollectionUtils.transformToList(records, (id) -> id.sysId);
            totalDevices     = assetIDs.size();
            devicesProcessed = 0;

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
                RecordHelper<AssetRecord> helper      = holder.createHelper(AssetRecord.class);
                List<AssetRecord>         deviceBatch = AssetRecord.getAssetsBatch(helper, nextBatch);

                RowsForReport chunk = new RowsForReport();

                for (AssetRecord asset : deviceBatch)
                {
                    DeviceRecord device = Reflection.as(asset, DeviceRecord.class);
                    if (device != null)
                    {
                        RowForReport row = new RowForReport();

                        row.col_name = device.getName();

                        BaseAssetDescriptor identifier = device.getIdentityDescriptor();
                        if (identifier != null)
                        {
                            row.col_identifier = identifier.toString();
                        }

                        LocationRecord loc = device.getLocation();
                        if (loc != null)
                        {
                            row.col_location = loc.getName();
                        }

                        BACnetDeviceDescriptor identifierBACnet = Reflection.as(identifier, BACnetDeviceDescriptor.class);
                        if (identifierBACnet != null)
                        {
                            if (identifierBACnet.transport != null)
                            {
                                row.col_transport = identifierBACnet.transport.toString();
                            }

                            if (identifierBACnet.bacnetAddress != null)
                            {
                                row.col_address = identifierBACnet.bacnetAddress.toString();
                            }
                        }

                        row.col_product      = device.getProductName();
                        row.col_modelNum     = device.getModelName();
                        row.col_manufacturer = device.getManufacturerName();

                        chunk.rows.add(row);
                    }
                    devicesProcessed++;
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
