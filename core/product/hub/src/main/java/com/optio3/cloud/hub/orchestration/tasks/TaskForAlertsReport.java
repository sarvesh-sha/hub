package com.optio3.cloud.hub.orchestration.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.formatting.TabularField;
import com.optio3.cloud.formatting.TabularReportAsExcel;
import com.optio3.cloud.hub.model.alert.AlertFilterRequest;
import com.optio3.cloud.hub.model.alert.AlertsReportProgress;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.logic.IBackgroundActivityProgress;
import com.optio3.cloud.model.PaginatedRecordIdentityList;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.CollectionUtils;

public class TaskForAlertsReport extends BaseReportTask implements IBackgroundActivityProgress<AlertsReportProgress>
{
    private final static int    batchSize   = 20;
    private static final String chunk_ROWS  = "Rows";
    private static final String chunk_EXCEL = "Excel";

    static class RowForReport
    {
        @TabularField(order = 0, title = "Title")
        public String col_title;

        @TabularField(order = 1, title = "Type")
        public String col_type;

        @TabularField(order = 2, title = "Severity")
        public String col_severity;

        @TabularField(order = 3, title = "Status")
        public String col_status;

        @TabularField(order = 4, title = "First Occurred", format = "yyyy-MM-dd hh:mm:ss")
        public ZonedDateTime col_created;

        @TabularField(order = 5, title = "Last Updated", format = "yyyy-MM-dd hh:mm:ss")
        public ZonedDateTime col_lastUpdated;

        @TabularField(order = 6, title = "Context")
        public String col_context;

        @TabularField(order = 7, title = "Location")
        public String col_location;

        @TabularField(order = 8, title = "guid")
        public String col_sysId;
    }

    static class RowsForReport
    {
        public final List<TaskForAlertsReport.RowForReport> rows = Lists.newArrayList();
    }

    //--//

    public List<String> alertIDs;

    public int     totalAlerts;
    public int     alertsProcessed;
    public boolean generatingFile;

    //--//

    @Override
    public String getTitle()
    {
        return "Alerts Report";
    }

    @Override
    public RecordLocator<? extends RecordWithCommonFields> getContext()
    {
        return null;
    }

    @Override
    public AlertsReportProgress fetchProgress(SessionHolder sessionHolder,
                                              boolean detailed)
    {
        AlertsReportProgress results = new AlertsReportProgress();
        results.totalAlerts     = totalAlerts;
        results.alertsProcessed = alertsProcessed;
        results.generatingFile  = generatingFile;

        return results;
    }

    @Override
    public void generateStream() throws
                                 IOException
    {
        try (var holder = new TabularReportAsExcel.Holder())
        {
            TabularReportAsExcel<TaskForAlertsReport.RowForReport> tr = new TabularReportAsExcel<>(TaskForAlertsReport.RowForReport.class, "Alerts", holder);

            tr.emit(rowHandler ->
                    {
                        forEachChunkInSequence(chunk_ROWS, TaskForAlertsReport.RowsForReport.class, (seq, chunk) ->
                        {
                            for (TaskForAlertsReport.RowForReport row : chunk.rows)
                            {
                                rowHandler.emitRow(row);
                            }
                        });
                    });
        }
    }

    @Override
    public InputStream streamContents() throws
                                        IOException
    {
        return readAsStream(chunk_EXCEL);
    }

    @BackgroundActivityMethod(needsSession = true)
    public void process(SessionHolder sessionHolder) throws
                                                     Exception
    {
        if (alertIDs == null)
        {
            PaginatedRecordIdentityList alertIds = AlertRecord.filter(sessionHolder.createHelper(AlertRecord.class), new AlertFilterRequest());

            alertIDs        = CollectionUtils.transformToList(alertIds.results, id -> id.sysId);
            totalAlerts     = alertIDs.size();
            alertsProcessed = 0;

            flushStateToDatabase(sessionHolder);
        }

        ReportFlusher flusher = new ReportFlusher(1500);

        while (!alertIDs.isEmpty())
        {
            if (flusher.shouldReport())
            {
                flushStateToDatabase(sessionHolder);
            }

            List<String> nextBatch = Lists.newArrayList();
            int          lastIdx   = Math.max(alertIDs.size() - batchSize, 0);
            for (int i = alertIDs.size() - 1; i >= lastIdx; i--)
            {
                nextBatch.add(alertIDs.remove(i));
            }

            try (SessionHolder holder = sessionHolder.spawnNewReadOnlySession())
            {
                RecordHelper<AlertRecord> helper     = holder.createHelper(AlertRecord.class);
                List<AlertRecord>         alertBatch = AlertRecord.getBatch(helper, nextBatch);

                TaskForAlertsReport.RowsForReport chunk = new TaskForAlertsReport.RowsForReport();

                for (AlertRecord alert : alertBatch)
                {
                    if (alert != null)
                    {
                        TaskForAlertsReport.RowForReport row = new TaskForAlertsReport.RowForReport();

                        row.col_title    = alert.getDescription();
                        row.col_type     = alert.getType()
                                                .getDisplayName();
                        row.col_severity = alert.getSeverity()
                                                .getDisplayName();
                        row.col_status   = alert.getStatus()
                                                .getDisplayName();

                        row.col_created     = alert.getCreatedOn();
                        row.col_lastUpdated = alert.getUpdatedOn();

                        AssetRecord context = alert.getAsset();
                        if (context != null)
                        {
                            row.col_context = context.getName();
                        }

                        LocationRecord loc = alert.getLocation();
                        if (loc != null)
                        {
                            row.col_location = loc.getName();
                        }

                        row.col_sysId = alert.getSysId();

                        chunk.rows.add(row);
                    }

                    alertsProcessed++;
                }

                addChunkToSequence(chunk_ROWS, chunk);
            }
        }

        generatingFile = true;

        flushStateToDatabase(sessionHolder);

        generateStream();

        markAsCompleted();
    }

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder) throws
                                                                                     Exception
    {
        return scheduleActivity(sessionHolder, 0, null, TaskForAlertsReport.class, (t) ->
        {
        });
    }
}
