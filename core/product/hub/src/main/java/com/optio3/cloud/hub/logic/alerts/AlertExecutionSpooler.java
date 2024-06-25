/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.alerts;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.persistence.LockTimeoutException;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.engine.EngineExecutionProgram;
import com.optio3.cloud.hub.engine.alerts.AlertDefinitionDetails;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionStep;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphResponse;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionRecord;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionVersionRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.DatabaseActivity;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.persistence.ITableLockProvider;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.persistence.TableLockHolder;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;
import com.optio3.logging.RedirectingLogger;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.ConsumerWithException;
import org.apache.commons.lang3.StringUtils;

public class AlertExecutionSpooler
{
    private static final Function<Object, Object> s_processor = (arg) ->
    {
        if (arg == null)
        {
            return null;
        }

        if (arg instanceof ZonedDateTime)
        {
            return TimeUtils.DEFAULT_FORMATTER_NO_MILLI.format((ZonedDateTime) arg);
        }

        if (arg instanceof String)
        {
            return arg;
        }

        if (arg instanceof Number)
        {
            return arg;
        }

        if (arg instanceof Enum<?>)
        {
            return arg;
        }

        if (arg instanceof Throwable)
        {
            return arg;
        }

        try
        {
            return ObjectMappers.prettyPrintAsJson(arg);
        }
        catch (Throwable t)
        {
            return arg;
        }
    };

    //--//

    private class Context
    {
        final String                                         sysId;
        final String                                         versionSysId;
        final int                                            version;
        final EngineExecutionProgram<AlertDefinitionDetails> program;

        final String            logId;
        final RedirectingLogger logger;

        AlertEngineExecutionContext.State state;
        MonotonousTime                    nextEvaluation;

        private CompletableFuture<Void> m_worker;
        private ScheduledFuture<?>      m_wakeup;
        private boolean                 m_shutdownAlert;

        private Context(SessionHolder sessionHolder,
                        AlertDefinitionRecord rec_alert)
        {
            sysId = rec_alert.getSysId();
            state = rec_alert.getExecutionState();
            if (state == null)
            {
                state = new AlertEngineExecutionContext.State();
            }

            AlertDefinitionVersionRecord rec_ver = rec_alert.getReleaseVersion();
            if (rec_ver != null)
            {
                versionSysId = rec_ver.getSysId();
                version      = rec_ver.getVersion();
                program      = rec_ver.prepareProgram(sessionHolder);
            }
            else
            {
                versionSysId = null;
                version      = 0;
                program      = null;
            }

            String id = rec_alert.getSysId();

            String title = rec_alert.getTitle();
            if (StringUtils.isNotBlank(title))
            {
                id += " - " + title;
            }

            logId  = id;
            logger = new RedirectingLogger(LoggerInstance)
            {
                @Override
                public String getPrefix()
                {
                    return String.format("[Alert: %s]", logId);
                }

                @Override
                protected Function<Object, Object> getArgumentProcessor()
                {
                    return s_processor;
                }
            };
        }

        synchronized void trigger()
        {
            nextEvaluation = null;

            if (m_wakeup != null)
            {
                m_wakeup.cancel(false);
                m_wakeup = null;
            }

            if (m_worker != null && m_worker.isDone())
            {
                m_worker = null;
            }

            if (m_worker == null)
            {
                m_worker = Executors.getDefaultLongRunningThreadPool()
                                    .queue(this::triggerInner);
            }
        }

        void shutdown()
        {
            m_shutdownAlert = true;
        }

        private void triggerInner()
        {
            while (!m_shutdown && !m_shutdownAlert)
            {
                if (nextEvaluation != null)
                {
                    Duration remainingTime = TimeUtils.remainingTime(nextEvaluation);
                    if (remainingTime != null)
                    {
                        m_wakeup = Executors.scheduleOnDefaultPool(this::trigger, remainingTime.get(ChronoUnit.SECONDS), TimeUnit.SECONDS);
                        return;
                    }
                }

                try
                {
                    this.execute();
                }
                catch (Exception e)
                {
                    if (!m_shutdown)
                    {
                        logger.error("Alert processing failed due to %s", e);
                    }

                    break;
                }
            }
        }

        private void execute() throws
                               Exception
        {
            if (program == null)
            {
                // No release version, nothing to evaluate.
                m_shutdownAlert = true;
                return;
            }

            ITableLockProvider provider = m_sessionProvider.getServiceNonNull(ITableLockProvider.class);

            try (TableLockHolder lockHolder = provider.lockRecord(m_sessionProvider, AlertDefinitionRecord.class, sysId, 2, TimeUnit.SECONDS))
            {
                try (AlertEngineExecutionContext ctx = new AlertEngineExecutionContext(logger, m_sessionProvider, sysId, versionSysId, version, program, null))
                {
                    ctx.importState(state);

                    logger.debugVerbose("Starting evaluation...");

                    Stopwatch  st         = Stopwatch.createStarted();
                    long       nextReport = 30;
                    final long maxRuntime = 3600;

                    for (AssetGraphResponse.Resolved graphTuple : ctx.forEachGraphTuple())
                    {
                        ctx.reset(null);
                        ctx.setGraphTuple(graphTuple);

                        while (!ctx.evaluate(10000, ctx::logEntry))
                        {
                            long runtime = st.elapsed(TimeUnit.SECONDS);
                            if (runtime > nextReport)
                            {
                                logger.debugVerbose("Evaluation lasting %s seconds...", runtime);

                                nextReport += 30;
                            }

                            if (runtime > maxRuntime)
                            {
                                logger.error("Evaluation took too long (%s seconds), quitting...", runtime);
                                return;
                            }

                            if (m_shutdown || m_shutdownAlert)
                            {
                                logger.debugVerbose("Evaluation shutdown...");
                                return;
                            }
                        }

                        logger.debugVerbose("Evaluation completed");

                        for (AlertEngineExecutionStep step : ctx.steps)
                        {
                            if (step.failureDetailed != null)
                            {
                                logger.error("Evaluation failed with %s", step.failureDetailed);
                                break;
                            }

                            step.commit(ctx);
                        }

                        ctx.alertHolder.resetNotificationFlags();
                    }

                    ctx.exportState(state);
                }

                runWithSession(false, false, (sessionHolder) ->
                {
                    AlertDefinitionRecord rec_alert = sessionHolder.getEntityOrNull(AlertDefinitionRecord.class, sysId);
                    rec_alert.setExecutionState(state);
                });

                // Trigger every few minutes, even if nothing happens.
                nextEvaluation = TimeUtils.computeTimeoutExpiration(5, TimeUnit.MINUTES);
            }
            catch (LockTimeoutException t)
            {
                // Table locked, retry in a bit.
                nextEvaluation = TimeUtils.computeTimeoutExpiration(30, TimeUnit.SECONDS);
            }
        }
    }

    //--//

    public static final Logger LoggerInstance = new Logger(AlertExecutionSpooler.class);

    private final SessionProvider      m_sessionProvider;
    private final Object               m_lock   = new Object();
    private final Map<String, Context> m_alerts = Maps.newHashMap();
    private       boolean              m_shutdown;

    private DatabaseActivity.LocalSubscriber m_regDbActivity;

    //--//

    public AlertExecutionSpooler(HubApplication app)
    {
        m_sessionProvider = new SessionProvider(app, null, Optio3DbRateLimiter.Normal);

        app.registerService(AlertExecutionSpooler.class, () -> this);
    }

    public void initialize()
    {
        m_regDbActivity = DatabaseActivity.LocalSubscriber.create(m_sessionProvider.getServiceNonNull(MessageBusBroker.class));

        m_regDbActivity.subscribeToTable(AlertDefinitionRecord.class, (dbEvent) ->
        {
            switch (dbEvent.action)
            {
                case INSERT:
                    loadAlert(dbEvent.context.sysId);
                    break;

                case UPDATE_DIRECT:
                case DELETE:
                    updateAlert(dbEvent.context.sysId);
                    break;
            }
        });

        m_regDbActivity.subscribeToTable(AssetRecord.class, (dbEvent) ->
        {
            switch (dbEvent.action)
            {
                case UPDATE_DIRECT:
                case UPDATE_INDIRECT:
                    triggerAlert(dbEvent.context.sysId);
                    break;
            }
        });

        //
        // Fetch all the alerts
        //
        runWithSession(true, true, (sessionHolder) ->
        {
            RecordHelper<AlertDefinitionRecord> helper = sessionHolder.createHelper(AlertDefinitionRecord.class);

            for (RecordIdentity ri : AlertDefinitionRecord.filter(helper, null))
            {
                loadAlertUnderLock(sessionHolder, ri.sysId);
            }
        });
    }

    public void close()
    {
        m_shutdown = true;

        if (m_regDbActivity != null)
        {
            m_regDbActivity.close();
            m_regDbActivity = null;
        }

        synchronized (m_lock)
        {
            for (Context ctx : m_alerts.values())
            {
                ctx.shutdown();
            }
        }
    }

    //--//

    private void loadAlert(String sysId_alert)
    {
        runWithSession(true, true, (sessionHolder) -> loadAlertUnderLock(sessionHolder, sysId_alert));
    }

    private void loadAlertUnderLock(SessionHolder sessionHolder,
                                    String sysId_alert)
    {
        removeAlertUnderLock(sysId_alert);

        AlertDefinitionRecord rec_alert = sessionHolder.getEntityOrNull(AlertDefinitionRecord.class, sysId_alert);
        if (rec_alert != null && rec_alert.isActive())
        {
            newAlert(sessionHolder, rec_alert);
        }
    }

    private void newAlert(SessionHolder sessionHolder,
                          AlertDefinitionRecord rec_alert)
    {
        Context ctx = new Context(sessionHolder, rec_alert);

        m_alerts.put(ctx.sysId, ctx);

        ctx.trigger();
    }

    private void updateAlert(String sysId_alert)
    {
        runWithSession(true, true, (sessionHolder) ->
        {
            AlertDefinitionRecord rec_alert = sessionHolder.getEntityOrNull(AlertDefinitionRecord.class, sysId_alert);
            if (rec_alert == null || !rec_alert.isActive())
            {
                // Deleted or deactivated, remove.
                removeAlertUnderLock(sysId_alert);
                return;
            }

            Context ctx = m_alerts.get(sysId_alert);
            if (ctx != null)
            {
                boolean reload = false;

                AlertDefinitionVersionRecord rec_ver = rec_alert.getReleaseVersion();
                if (rec_ver == null || rec_ver.getVersion() != ctx.version)
                {
                    reload = true;
                }

                AlertEngineExecutionContext.State newState = rec_alert.getExecutionState();
                if (!StringUtils.equals(ctx.state.version, newState.version))
                {
                    reload = true;
                }

                if (!reload)
                {
                    // Nothing changed, exit.
                    return;
                }

                ctx.shutdown();
            }

            newAlert(sessionHolder, rec_alert);
        });
    }

    private void removeAlertUnderLock(String sysId_alert)
    {
        Context oldCtx = m_alerts.get(sysId_alert);
        if (oldCtx != null)
        {
            m_alerts.remove(sysId_alert);

            oldCtx.shutdown();
        }
    }

    private void triggerAlert(String sysId_asset)
    {
        synchronized (m_lock)
        {
            if (m_shutdown)
            {
                return;
            }

            for (Context ctx : m_alerts.values())
            {
                if (ctx.state.containsDeviceElement(sysId_asset))
                {
                    ctx.logger.debugVerbose("Triggering evaluation due to %s...", sysId_asset);

                    ctx.trigger();
                }
            }
        }
    }

    private boolean runWithSession(boolean readOnly,
                                   boolean acquireLock,
                                   ConsumerWithException<SessionHolder> callback)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithoutTransaction())
        {
            if (!readOnly)
            {
                sessionHolder.beginTransaction();
            }

            if (acquireLock)
            {
                synchronized (m_lock)
                {
                    callback.accept(sessionHolder);
                }
            }
            else
            {
                callback.accept(sessionHolder);
            }

            if (!readOnly)
            {
                sessionHolder.commit();
            }

            return true;
        }
        catch (Exception e)
        {
            if (!m_shutdown)
            {
                LoggerInstance.error("Alert processing failed due to %s", e);
            }

            return false;
        }
    }
}
