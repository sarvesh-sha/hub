/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks.recurring;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3RecurringProcessor;
import com.optio3.cloud.hub.model.report.ReportReason;
import com.optio3.cloud.hub.model.visualization.TimeRange;
import com.optio3.cloud.hub.orchestration.tasks.TaskForReportGeneration;
import com.optio3.cloud.hub.persistence.report.ReportDefinitionRecord;
import com.optio3.cloud.hub.persistence.report.ReportDefinitionVersionRecord;
import com.optio3.cloud.hub.persistence.report.ReportRecord;
import com.optio3.cloud.logic.BackgroundActivityScheduler;
import com.optio3.cloud.logic.RecurringActivityHandlerForTable;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.DbEvent;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.logging.ILogger;
import com.optio3.logging.Logger;
import com.optio3.util.TimeUtils;

@Optio3RecurringProcessor
public class RecurringReportGeneration extends RecurringActivityHandlerForTable<ReportDefinitionRecord>
{
    public static final Logger LoggerInstance = BackgroundActivityScheduler.LoggerInstance.createSubLogger(RecurringReportGeneration.class);

    public RecurringReportGeneration()
    {
        super(ReportDefinitionRecord.class);
    }

    @Override
    protected ILogger getLogger()
    {
        return LoggerInstance;
    }

    @Override
    public Duration startupDelay()
    {
        // Delay first report by a few minutes after startup.
        return Duration.of(3, ChronoUnit.MINUTES);
    }

    @Override
    public boolean shouldTrigger(DbEvent event)
    {
        return true;
    }

    @Override
    public void process(SessionHolder sessionHolder,
                        ReportDefinitionRecord rec) throws
                                                    Exception
    {
        ZonedDateTime nextActivation = rec.getNextActivation();

        if (shouldAutoDelete(rec))
        {
            RecordHelper<ReportDefinitionRecord> reportDefinitionHelper = sessionHolder.createHelper(ReportDefinitionRecord.class);

            try (ValidationResultsHolder validation = new ValidationResultsHolder(sessionHolder, false, false))
            {
                rec.remove(validation, reportDefinitionHelper);
            }

            return;
        }

        if (nextActivation != null && nextActivation.isBefore(TimeUtils.now()) && rec.getActive())
        {
            RecordHelper<ReportRecord> reportHelper = sessionHolder.createHelper(ReportRecord.class);

            ReportRecord rec_report = createReport(reportHelper, rec);
            TaskForReportGeneration.scheduleTask(sessionHolder, rec_report, rec_report.getCreatedOn());
        }

        for (ReportRecord rec_report : Lists.newArrayList(rec.getReports()))
        {
            switch (rec_report.getStatus())
            {
                case Finished:
                    if (!TimeUtils.wasUpdatedRecently(rec_report.getCreatedOn(), 30, TimeUnit.DAYS))
                    {
                        sessionHolder.deleteEntity(rec_report);
                    }
                    break;

                case Failed:
                    if (!TimeUtils.wasUpdatedRecently(rec_report.getCreatedOn(), 3, TimeUnit.DAYS))
                    {
                        sessionHolder.deleteEntity(rec_report);
                    }
                    break;
            }
        }

        rec.refreshNextActivation();
    }

    @Override
    public void shutdown()
    {
        // Nothing to do.
    }

    private ReportRecord createReport(RecordHelper<ReportRecord> reportHelper,
                                      ReportDefinitionRecord rec_reportDefinition)
    {
        ReportDefinitionVersionRecord rec_reportDefinitionVersion = rec_reportDefinition.getReleaseVersion();
        ReportRecord                  rec_report                  = ReportRecord.newInstance(rec_reportDefinition, rec_reportDefinitionVersion);

        rec_report.setReason(ReportReason.Scheduled);
        setReportRange(rec_report, rec_reportDefinition, rec_reportDefinitionVersion);

        reportHelper.persist(rec_report);

        return rec_report;
    }

    private void setReportRange(ReportRecord rec_report,
                                ReportDefinitionRecord rec_reportDefinition,
                                ReportDefinitionVersionRecord rec_reportDefinitionVersion)
    {

        ZonedDateTime nextReport = rec_reportDefinition.getNextActivation();
        TimeRange range = rec_reportDefinitionVersion.getDetails()
                                                     .getReportRange(nextReport);

        if (range != null)
        {
            rec_report.setRangeEnd(range.end);
            rec_report.setRangeStart(range.start);
        }
    }

    private boolean shouldAutoDelete(ReportDefinitionRecord rec)
    {
        ZonedDateTime when = rec.getAutoDelete();
        return when != null && when.isBefore(TimeUtils.now());
    }
}
