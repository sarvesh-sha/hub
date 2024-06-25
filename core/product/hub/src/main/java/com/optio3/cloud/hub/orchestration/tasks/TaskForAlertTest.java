/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.exception.NotImplementedException;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionSteps;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionStepsOverRange;
import com.optio3.cloud.hub.model.alert.AlertTestProgress;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphResponse;
import com.optio3.cloud.hub.model.common.LogLine;
import com.optio3.cloud.hub.orchestration.AbstractHubActivityHandler;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionVersionRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.logic.IBackgroundActivityProgress;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public class TaskForAlertTest extends AbstractHubActivityHandler implements IBackgroundActivityProgress<AlertTestProgress>
{
    public RecordLocator<AlertDefinitionVersionRecord> loc_alertDef;
    public int                                         maxSteps;
    public int                                         stepInterval;

    public AlertTestProgress progress;

    //--//

    @Override
    public AlertTestProgress fetchProgress(SessionHolder sessionHolder,
                                           boolean detailed)
    {
        if (!detailed)
        {
            progress.results = null;
            progress.logEntries.clear();
        }

        return progress;
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
                                                        AlertDefinitionVersionRecord rec_alertDef,
                                                        int maxSteps,
                                                        int stepInterval,
                                                        ZonedDateTime start,
                                                        ZonedDateTime end) throws
                                                                           Exception
    {

        return scheduleActivity(sessionHolder, 0, null, TaskForAlertTest.class, (t) ->
        {
            t.loc_alertDef = sessionHolder.createLocator(rec_alertDef);
            t.maxSteps     = maxSteps;
            t.stepInterval = stepInterval;

            t.progress         = new AlertTestProgress();
            t.progress.start   = TimeUtils.truncateTimestampToMultipleOfPeriod(start, stepInterval);
            t.progress.current = t.progress.start;
            t.progress.end     = TimeUtils.truncateTimestampToMultipleOfPeriod(end, stepInterval);
        });
    }

    //--//

    @Override
    public String getTitle()
    {
        return "Alert test";
    }

    @Override
    public RecordLocator<? extends RecordWithCommonFields> getContext()
    {
        return loc_alertDef;
    }

    @BackgroundActivityMethod(needsSession = true)
    public void process(SessionHolder sessionHolder) throws
                                                     Exception
    {
        AlertDefinitionVersionRecord rec = sessionHolder.fromLocatorOrNull(loc_alertDef);

        AlertEngineExecutionStepsOverRange results = new AlertEngineExecutionStepsOverRange();
        ZonedDateTime                      current = progress.start;

        try (AlertEngineExecutionContext ctx = new AlertEngineExecutionContext(loggerInstance, sessionHolder, rec, null))
        {
            ctx.alertHolder.ignoreRealAlerts();
            ctx.alertHolder.setCutoffTimestamp(progress.start);

            MonotonousTime nextRefresh = null;

            while (current.isBefore(progress.end))
            {
                final ZonedDateTime timestampForLog = current;

                for (AssetGraphResponse.Resolved graphTuple : ctx.forEachGraphTuple())
                {
                    ctx.reset(current);
                    ctx.setGraphTuple(graphTuple);

                    ctx.evaluate(maxSteps, (stack, line) ->
                    {
                        LogLine ll = new LogLine();
                        ll.lineNumber = progress.logEntries.size();
                        ll.timestamp  = timestampForLog;
                        ll.line       = line;
                        progress.logEntries.add(ll);
                    });

                    if (!ctx.steps.isEmpty())
                    {
                        AlertEngineExecutionSteps snapshot = new AlertEngineExecutionSteps();
                        snapshot.timestamp = current;
                        snapshot.steps.addAll(ctx.steps);
                        results.evaluationResults.add(snapshot);
                    }

                    ctx.alertHolder.resetNotificationFlags();

                    if (TimeUtils.isTimeoutExpired(nextRefresh))
                    {
                        nextRefresh = TimeUtils.computeTimeoutExpiration(1, TimeUnit.SECONDS);

                        progress.current = current;
                        flushStateToDatabase(sessionHolder);
                    }
                }

                current = current.plusSeconds(stepInterval);
            }
        }

        progress.current = current;
        progress.results = results;

        markAsCompleted();
    }
}
