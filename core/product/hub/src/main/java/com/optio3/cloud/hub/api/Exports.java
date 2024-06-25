/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.BiFunction;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.formatting.TabularReport;
import com.optio3.cloud.formatting.TabularReportAsExcel;
import com.optio3.cloud.hub.orchestration.tasks.BaseReportTask;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.logic.IBackgroundActivityProgress;
import com.optio3.cloud.model.exports.ExportCell;
import com.optio3.cloud.model.exports.ExportColumn;
import com.optio3.cloud.model.exports.ExportHeader;
import com.optio3.cloud.model.scheduler.BaseBackgroundActivityProgress;
import com.optio3.cloud.persistence.RecordForBackgroundActivity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "Exports" }) // For Swagger
@Optio3RestEndpoint(name = "Exports") // For Optio3 Shell
@Path("/v1/exports")
public class Exports
{
    public static class GeneratorProgress extends BaseBackgroundActivityProgress
    {
    }

    public static class Generator extends BaseReportTask implements IBackgroundActivityProgress<GeneratorProgress>
    {
        private static final String chunk_ROWS  = "Rows";
        private static final String chunk_EXCEL = "Excel";

        public static class ChunkOfRows
        {
            public List<List<ExportCell>> rows;
        }

        public ExportHeader header;
        public int          chunks;

        //--//

        public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                            ExportHeader header) throws
                                                                                 Exception
        {
            //
            // If it's not done in 5 minutes, delete.
            //
            return scheduleActivity(sessionHolder, 5, ChronoUnit.MINUTES, Generator.class, (t) ->
            {
                t.header = header;
            });
        }

        public boolean push(List<List<ExportCell>> rows)
        {
            for (List<ExportCell> row : rows)
            {
                int numCols = header.columns.size();
                if (row == null || row.size() != numCols)
                {
                    return false;
                }
            }

            ChunkOfRows c = new ChunkOfRows();
            c.rows = rows;
            addChunkToSequence(chunk_ROWS, c);
            return true;
        }

        @Override
        public GeneratorProgress fetchProgress(SessionHolder sessionHolder,
                                               boolean detailed)
        {
            return null;
        }

        @Override
        public void generateStream() throws
                                     IOException
        {
            List<TabularReport.ColumnDescriptor<List<ExportCell>>> columnDescriptors = Lists.newArrayList();

            for (int col = 0; col < header.columns.size(); col++)
            {
                ExportColumn column = header.columns.get(col);

                BiFunction<List<ExportCell>, TabularReport.ColumnDescriptor<List<ExportCell>>, Object> accessor = (ctx, desc) ->
                {
                    ExportCell cell = ctx.get(desc.order);
                    if (cell.dateTime != null)
                    {
                        return cell.dateTime;
                    }

                    if (cell.text != null)
                    {
                        return cell.text;
                    }

                    return cell.decimal;
                };

                TabularReport.ColumnDescriptor<List<ExportCell>> colDesc = new TabularReport.ColumnDescriptor<>(accessor, col, column.title, column.dateFormatter);
                columnDescriptors.add(colDesc);
            }

            try (var holder = new TabularReportAsExcel.Holder())
            {
                TabularReportAsExcel<List<ExportCell>> tr = new TabularReportAsExcel<>(columnDescriptors, header.sheetName, holder);

                tr.emit(rowHandler ->
                        {
                            forEachChunkInSequence(chunk_ROWS, ChunkOfRows.class, (seq, chunk) ->
                            {
                                for (List<ExportCell> rowValues : chunk.rows)
                                {
                                    rowHandler.emitRow(rowValues);
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

        @Override
        public String getTitle()
        {
            return "Table Data Generator";
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
            // Nothing to do here, just delete
            markAsCompleted();
        }
    }

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("start")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Optio3RequestLogLevel(Severity.Debug)
    public String start(ExportHeader header) throws
                                             Exception
    {
        RecordLocator<BackgroundActivityRecord> loc_task = BackgroundActivityRecord.wrapTask(m_sessionProvider, (holder) -> Generator.scheduleTask(holder, header));

        return loc_task.getIdRaw();
    }

    @POST
    @Path("add/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public boolean add(@PathParam("id") String sysId,
                       List<List<ExportCell>> rows) throws
                                                    Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            BackgroundActivityRecord rec = sessionHolder.getEntityOrNull(BackgroundActivityRecord.class, sysId);
            if (rec != null)
            {
                Generator h = (Generator) rec.getHandler(sessionHolder);

                if (!h.push(rows))
                {
                    return false;
                }

                rec.setHandler(h);

                sessionHolder.commit();
                return true;
            }

            return false;
        }
    }

    @GET
    @Path("generate/{id}/excel")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public boolean generateExcel(@PathParam("id") String sysId) throws
                                                                Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            BackgroundActivityRecord rec = sessionHolder.getEntityOrNull(BackgroundActivityRecord.class, sysId);
            if (rec != null)
            {
                Generator h = (Generator) rec.getHandler(sessionHolder);

                h.generateStream();

                rec.setHandler(h);

                sessionHolder.commit();
                return true;
            }

            return false;
        }
    }

    @GET
    @Path("stream/{id}/excel/{fileName}")
    @Produces("application/octet-stream")
    @Optio3RequestLogLevel(Severity.Debug)
    public InputStream streamExcel(@PathParam("id") String sysId,
                                   @PathParam("fileName") String fileName) throws
                                                                           Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return RecordForBackgroundActivity.streamContents(helper, sysId, Generator.class);
        }
    }
}
