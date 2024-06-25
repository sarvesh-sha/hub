/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.spooler;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.LockTimeoutException;

import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.logic.protocol.BatchIngestionContext;
import com.optio3.cloud.hub.logic.protocol.CommonProtocolHandlerForIngestion;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.ResultStagingRecord;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.DatabaseActivity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.TableLockHolder;
import com.optio3.cloud.search.HibernateSearch;
import com.optio3.collection.Memoizer;
import com.optio3.concurrency.AsyncGate;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;
import com.optio3.util.Exceptions;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public class ResultStagingSpooler
{
    public static class Gate extends AbstractApplicationWithDatabase.GateClass
    {
    }

    enum NextAction
    {
        YieldControl,
        ContinueProcessing,
        Done,
    }

    class Holder implements AutoCloseable
    {
        boolean m_readOnly;
        boolean m_acquired;

        Holder(Duration maxWait,
               boolean asWriter)
        {
            if (asWriter)
            {
                acquireAsWriter(maxWait);
            }
            else
            {
                acquireAsReader(maxWait);
            }
        }

        @Override
        public void close()
        {
            release();
        }

        private void ensureAcquired(boolean asWriter)
        {
            if (!m_acquired)
            {
                throw new RuntimeException("Holder disposed!");
            }

            if (asWriter && m_readOnly)
            {
                throw new RuntimeException("Holder not in Writer mode!");
            }
        }

        private void ensureNotAcquired()
        {
            if (m_acquired)
            {
                throw new RuntimeException("Holder already acquired lock!");
            }
        }

        private boolean acquireAsReader(Duration maxWait)
        {
            ensureNotAcquired();

            m_readOnly = true;

            Thread thread = Thread.currentThread();

            MonotonousTime giveUpAfter = maxWait != null ? TimeUtils.computeTimeoutExpiration(maxWait) : null;

            synchronized (m_lock)
            {
                if (m_lockWriter == thread)
                {
                    throw Exceptions.newRuntimeException("INTERNAL ERROR: thread '%s' tried to acquire ResultStagingSpooler lock recursively", thread);
                }

                while (true)
                {
                    if (m_lockWriter == null)
                    {
                        m_lockReaders++;
                        break;
                    }

                    if (!waitOnLock(giveUpAfter))
                    {
                        return false;
                    }
                }

                //--//

                m_acquired = true;
            }

            return true;
        }

        private boolean acquireAsWriter(Duration maxWait)
        {
            ensureNotAcquired();

            MonotonousTime giveUpAfter = maxWait != null ? TimeUtils.computeTimeoutExpiration(maxWait) : null;

            synchronized (m_lock)
            {
                while (true)
                {
                    if (m_lockReaders == 0 && m_lockWriter == null)
                    {
                        m_lockWriter = Thread.currentThread();
                        m_readOnly   = false;
                        m_acquired   = true;

                        if (m_invalidateSummary)
                        {
                            m_invalidateSummary = false;
                            m_state             = new State(m_state);
                        }

                        return true;
                    }

                    if (!waitOnLock(giveUpAfter))
                    {
                        return false;
                    }
                }
            }
        }

        private void release()
        {
            if (m_acquired)
            {
                synchronized (m_lock)
                {
                    if (m_readOnly)
                    {
                        if (m_lockReaders <= 0)
                        {
                            throw Exceptions.newRuntimeException("INTERNAL ERROR: thread '%s' is not the reader of ResultStagingSpooler, because lock is free", Thread.currentThread());
                        }

                        m_lockReaders--;
                    }
                    else
                    {
                        Thread thread = Thread.currentThread();
                        if (thread != m_lockWriter)
                        {
                            if (m_lockWriter == null)
                            {
                                throw Exceptions.newRuntimeException("INTERNAL ERROR: thread '%s' is not the writer of ResultStagingSpooler, thread '%s' is the writer", thread, m_lockWriter);
                            }
                            else
                            {
                                throw Exceptions.newRuntimeException("INTERNAL ERROR: thread '%s' is not the writer of ResultStagingSpooler, because lock is free", thread);
                            }
                        }

                        m_lockWriter = null;
                    }

                    m_acquired = false;
                    m_lock.notifyAll();
                }
            }
        }

        //--//

        boolean wasAcquired()
        {
            return m_acquired;
        }

        <T> T upgradeToWriter(Duration maxWaitForSpooler,
                              Callable<T> callback) throws
                                                    Exception
        {
            ensureAcquired(false);

            boolean wasReadonly = m_readOnly;

            if (wasReadonly)
            {
                //
                // Remove ourselves from the Reader set.
                //
                release();

                if (!acquireAsWriter(maxWaitForSpooler))
                {
                    //
                    // Add ourselves back into the Reader set.
                    //
                    acquireAsReader(null);
                    return null;
                }
            }

            try
            {
                return callback.call();
            }
            finally
            {
                if (wasReadonly)
                {
                    ensureAcquired(true);

                    synchronized (m_lock)
                    {
                        Thread thread = Thread.currentThread();
                        if (m_lockWriter != thread)
                        {
                            throw Exceptions.newRuntimeException("INTERNAL ERROR: ResultStagingSpooler lock has different writer (thread '%s' instead of '%s')", m_lockWriter, thread);
                        }

                        //
                        // Switch to reader.
                        //
                        m_lockReaders = 1;
                        m_lockWriter  = null;
                        m_readOnly    = true;
                    }
                }
            }
        }

        //--//

        void flushState()
        {
            ensureAcquired(true);

            m_state.summaryForStagedResults.close();
            m_state = new State();
        }

        boolean canKeepRunning(boolean asBackgroundWorker,
                               Duration maxWaitForSpooler) throws
                                                           Exception
        {
            if (m_state.summaryForStagedResults.mustFlush())
            {
                // If we must flush the queue, background worker has priority, all others are kicked out.
                return asBackgroundWorker;
            }

            ensureAcquired(false);

            boolean canProceed = callIfNeeded(m_importAssets, maxWaitForSpooler, () ->
            {
                LoggerInstance.debug("Computing Database summary...");

                if (maxWaitForSpooler != null)
                {
                    if (!m_app.waitForGateToOpen(ResultStagingSpooler.Gate.class, maxWaitForSpooler))
                    {
                        return false;
                    }
                }
                else
                {
                    m_app.waitForGateToOpen(ResultStagingSpooler.Gate.class);
                }

                LoggerInstance.debug("Gate open...");

                try (var searchGate = m_app.closeGate(HibernateSearch.Gate.class))
                {
                    try (var tagsGate = m_app.closeGate(TagsEngine.Gate.class))
                    {
                        final State state = requireNonNull(m_state);

                        try (SessionHolder holder = SessionHolder.createWithNewReadOnlySession(m_app, null, Optio3DbRateLimiter.System))
                        {
                            state.summaryForDatabase.analyze(holder, LoggerInstance);
                        }
                    }
                }

                LoggerInstance.debug("Completed Database summary");
                return true;
            });

            if (canProceed)
            {
                canProceed = callIfNeeded(m_importStagedResults, maxWaitForSpooler, () ->
                {
                    LoggerInstance.debug("Importing Results summary...");

                    final State state = requireNonNull(m_state);

                    // Record when we started the import.
                    ZonedDateTime now                       = TimeUtils.now();
                    int           totalPendingObjectsBefore = state.summaryForStagedResults.getTotalPendingObjects();
                    int           totalPendingSamplesBefore = state.summaryForStagedResults.getTotalPendingSamples();

                    // Import records created after the timestamp.
                    try (SessionHolder holder = SessionHolder.createWithNewReadOnlySession(m_app, null, Optio3DbRateLimiter.System))
                    {
                        state.summaryForStagedResults.importRecords(holder);
                    }

                    int totalPendingObjectsAfter = state.summaryForStagedResults.getTotalPendingObjects();
                    int totalPendingSamplesAfter = state.summaryForStagedResults.getTotalPendingSamples();

                    int deltaPendingObjects = totalPendingObjectsAfter - totalPendingObjectsBefore;
                    int deltaPendingSample  = totalPendingSamplesAfter - totalPendingSamplesBefore;

                    if (deltaPendingObjects > 0 || deltaPendingSample > 0)
                    {
                        LoggerInstance.debug("Imported Results summary: %d new object chunks, %d new sample chunks...", deltaPendingObjects, deltaPendingSample);
                    }

                    //--//

                    if (m_emitDatabaseEvents)
                    {
                        state.summaryForStagedResults.emitDatabaseEvents(m_app, state.summaryForDatabase);
                    }

                    // After first import, we generate Database Activity events for pending results.
                    m_emitDatabaseEvents = true;

                    LoggerInstance.debug("Imported Results summary");
                    return true;
                });
            }

            return canProceed && m_keepRunning.get();
        }

        private boolean callIfNeeded(AtomicInteger needProcessing,
                                     Duration maxWaitForSpooler,
                                     Callable<Boolean> callback) throws
                                                                 Exception
        {
            while (m_keepRunning.get() && needProcessing.get() != 0)
            {
                Boolean res = upgradeToWriter(maxWaitForSpooler, () ->
                {
                    //
                    // Now we are the only ones that have permissions to mutate state.
                    // Recheck the flag, in case another thread went through this and processed the request before us.
                    //
                    int level = needProcessing.get();
                    if (level != 0)
                    {
                        boolean canProceed = callback.call();
                        if (!canProceed)
                        {
                            // Timeout on acquiring lock.
                            return null;
                        }

                        // Only after a successful run, reset the flag by the amount we saw.
                        needProcessing.addAndGet(-level);
                    }

                    return true;
                });

                if (res == null)
                {
                    // Timeout on acquiring lock.
                    return false;
                }
            }

            return true;
        }
    }

    class State
    {
        final DiscoveredAssetsSummary summaryForDatabase;
        final StagedResultsSummary    summaryForStagedResults;

        State()
        {
            summaryForDatabase      = new DiscoveredAssetsSummary(m_memoizer);
            summaryForStagedResults = new StagedResultsSummary(m_memoizer);

            m_importAssets.set(1);
            m_importStagedResults.set(1);
        }

        private State(State src) // Used to invalidate database summary
        {
            summaryForDatabase      = new DiscoveredAssetsSummary(m_memoizer);
            summaryForStagedResults = src.summaryForStagedResults;

            m_importAssets.set(1);
        }
    }

    //--//

    // Keep processed records for one day.
    private static final Duration c_storingLength = Duration.of(1, ChronoUnit.DAYS);

    // Flush samples to DB every X hours.
    private static final Duration c_samplesFlushDelay = Duration.of(6, ChronoUnit.HOURS);//2, ChronoUnit.MINUTES);//6, ChronoUnit.HOURS);

    public static final Logger LoggerInstance = new Logger(ResultStagingSpooler.class);

    public final AsyncGate gate;

    private final HubApplication m_app;
    private final Memoizer       m_memoizer;
    private final AtomicBoolean  m_processingInBackground = new AtomicBoolean();
    private final AtomicBoolean  m_keepRunning            = new AtomicBoolean(true);
    private final AtomicBoolean  m_yieldControl           = new AtomicBoolean();
    private final AtomicInteger  m_importAssets           = new AtomicInteger();
    private final AtomicInteger  m_importStagedResults    = new AtomicInteger();
    private final Object         m_lock                   = new Object();
    private       int            m_lockReaders;
    private       Thread         m_lockWriter;

    private DatabaseActivity.LocalSubscriber m_regDbActivity;

    private State         m_state;
    private boolean       m_invalidateSummary;
    private boolean       m_emitDatabaseEvents = false; // The initial import of results will not generate events, too much noise.
    private ZonedDateTime m_lastSamplesFlush   = TimeUtils.now();
    private int           m_failureBackoff;

    //--//

    public ResultStagingSpooler(HubApplication app)
    {
        m_app      = app;
        m_memoizer = app.getServiceNonNull(Memoizer.class);

        HubConfiguration cfg = app.getServiceNonNull(HubConfiguration.class);
        gate = new AsyncGate(cfg.isRunningUnitTests() ? 0 : 10, TimeUnit.SECONDS);

        app.registerService(ResultStagingSpooler.class, () -> this);

        // Initialize *after* the memoizer has been stored.
        m_state = new State();
    }

    public void initialize()
    {
        m_regDbActivity = DatabaseActivity.LocalSubscriber.create(m_app.getServiceNonNull(MessageBusBroker.class));

        m_regDbActivity.subscribeToTable(AssetRecord.class, (dbEvent) ->
        {
            switch (dbEvent.action)
            {
                case INSERT:
                    checkAsset(dbEvent.context.sysId, true);
                    break;

                case DELETE:
                    checkAsset(dbEvent.context.sysId, false);
                    break;
            }
        });

        m_regDbActivity.subscribeToTable(ResultStagingRecord.class, (dbEvent) ->
        {
            switch (dbEvent.action)
            {
                case INSERT:
                    if (m_importStagedResults.getAndIncrement() == 0)
                    {
                        // Reimport data.
                        importInBackground(StagedResultsProcessingMode.OnlyObjects);
                    }
                    break;
            }
        });

        HubConfiguration cfg = m_app.getServiceNonNull(HubConfiguration.class);
        importInBackground(cfg.developerSettings.flushResultsOnStartup ? StagedResultsProcessingMode.Everything : StagedResultsProcessingMode.OnlyObjects);
    }

    private void checkAsset(String sysId,
                            boolean mustBePresent)
    {
        final State state = requireNonNull(m_state);
        if (state.summaryForDatabase.hasAsset(sysId) != mustBePresent)
        {
            // Assets changed outside our control, invalidate summary.
            m_invalidateSummary = true;

            if (m_importAssets.getAndIncrement() == 0)
            {
                // Reimport assets.
                importInBackground(StagedResultsProcessingMode.OnlyObjects);
            }
        }
    }

    public void close() throws
                        Exception
    {
        m_keepRunning.set(false);
        notifyLock();

        if (m_regDbActivity != null)
        {
            m_regDbActivity.close();
            m_regDbActivity = null;
        }

        try (Holder holder = accessState(null, true))
        {
            holder.flushState();
        }
    }

    //--//

    public boolean areThereAnyUnprocessedObjects()
    {
        return m_state.summaryForStagedResults.getTotalPendingObjects() > 0;
    }

    public void queueFlush()
    {
        importInBackground(StagedResultsProcessingMode.Everything);
    }

    public void flushAssets(List<AssetRecord> lst,
                            Duration maxWaitForSpooler)
    {
        yieldControl();

        boolean trigger = false;

        //
        // Attempt to acquire the lock for only a few milliseconds, so we don't hog the long-running thread.
        //
        try (Holder holder = accessState(maxWaitForSpooler, false))
        {
            if (!holder.wasAcquired())
            {
                return;
            }

            if (!holder.canKeepRunning(false, maxWaitForSpooler))
            {
                return;
            }

            State state = m_state;

            for (AssetRecord rec : lst)
            {
                String sysId = rec.getSysId();

                DiscoveredAssetsSummary.ForObject dbObject = state.summaryForDatabase.lookupObject(sysId);
                if (dbObject != null)
                {
                    DiscoveredAssetsSummary.ForAsset dbAsset = dbObject.parentAsset;
                    DiscoveredAssetsSummary.ForRoot  dbRoot  = dbAsset.parentRoot;

                    StagedResultsSummary.ForRoot forRoot = state.summaryForStagedResults.roots.get(dbRoot.sysId);
                    if (forRoot != null)
                    {
                        StagedResultsSummary.ForAsset forAsset = forRoot.assets.get(dbAsset.identifier);
                        if (forAsset != null)
                        {
                            StagedResultsSummary.ForObject forObject = forAsset.objects.get(dbObject.identifier);
                            if (forObject != null)
                            {
                                forObject.markedForFlushing = true;
                                trigger                     = true;
                            }
                        }
                    }
                }
                else
                {
                    DiscoveredAssetsSummary.ForAsset dbAsset = state.summaryForDatabase.lookupAsset(sysId);
                    if (dbAsset != null)
                    {
                        DiscoveredAssetsSummary.ForRoot dbRoot = dbAsset.parentRoot;

                        StagedResultsSummary.ForRoot forRoot = state.summaryForStagedResults.roots.get(dbRoot.sysId);
                        if (forRoot != null)
                        {
                            StagedResultsSummary.ForAsset forAsset = forRoot.assets.get(dbAsset.identifier);
                            if (forAsset != null)
                            {
                                for (StagedResultsSummary.ForObject forObject : forAsset.objects.values())
                                {
                                    forObject.markedForFlushing = true;
                                    trigger                     = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            // Ignore failures.
        }

        if (trigger)
        {
            // Reimport assets.
            importInBackground(StagedResultsProcessingMode.OnlyObjects);
        }
    }

    public TimeSeries flushSamples(String sysId,
                                   Duration maxWaitForSpooler)
    {
        yieldControl();

        //
        // Attempt to acquire the lock for only a few milliseconds, so we don't hog the long-running thread.
        //
        try (Holder holder = accessState(maxWaitForSpooler, false))
        {
            if (!holder.wasAcquired())
            {
                return null;
            }

            if (!holder.canKeepRunning(false, maxWaitForSpooler))
            {
                return null;
            }

            State state = m_state;

            DiscoveredAssetsSummary.ForObject dbObject = state.summaryForDatabase.lookupObject(sysId);
            if (dbObject == null)
            {
                return null;
            }

            DiscoveredAssetsSummary.ForAsset dbAsset = dbObject.parentAsset;
            DiscoveredAssetsSummary.ForRoot  dbRoot  = dbAsset.parentRoot;

            //
            // If we have imported the staged results, we can use it to avoid queuing requests for non-stale records.
            //
            StagedResultsSummary.ForRoot resultRoot = state.summaryForStagedResults.roots.get(dbRoot.sysId);
            if (resultRoot == null)
            {
                return null;
            }

            StagedResultsSummary.ForAsset resultAsset = resultRoot.assets.get(dbAsset.identifier);
            if (resultAsset == null)
            {
                return null;
            }

            StagedResultsSummary.ForObject resultObject = resultAsset.objects.get(dbObject.identifier);
            if (resultObject == null)
            {
                return null;
            }

            TimeSeries timeSeries = TimeSeries.newInstance();

            HubConfiguration                        cfg      = m_app.getServiceNonNull(HubConfiguration.class);
            CommonProtocolHandlerForIngestion<?, ?> ingestor = resultAsset.decoder.prepareIngestor(cfg, LoggerInstance, resultAsset);
            ingestor.flushSamples(state.summaryForStagedResults, resultObject, timeSeries);

            // Compress identical values claser than X minutes.
            timeSeries.removeCloseIdenticalValues(DeviceElementRecord.MAX_TIME_SEPARATION);

            return timeSeries;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private void yieldControl()
    {
        m_yieldControl.set(true);
    }

    //--//

    private boolean waitOnLock(MonotonousTime giveUpAfter)
    {
        return TimeUtils.waitOnLock(m_lock, giveUpAfter);
    }

    private void notifyLock()
    {
        synchronized (m_lock)
        {
            m_lock.notifyAll();
        }
    }

    //--//

    private Holder accessState(Duration maxWaitForSpooler,
                               boolean asWriter)
    {
        return new Holder(maxWaitForSpooler, asWriter);
    }

    private void importInBackground(StagedResultsProcessingMode mode)
    {
        if (mode.shouldProcessSamples())
        {
            m_lastSamplesFlush = null;
        }

        importInBackgroundQueue();
    }

    private void importInBackgroundQueue()
    {
        Executors.getDefaultLongRunningThreadPool()
                 .queue(this::backgroundWorker);
    }

    private void backgroundWorker()
    {
        Duration maxWait = Duration.of(500, ChronoUnit.MILLIS);
        boolean  allDone = false;

        if (m_processingInBackground.compareAndSet(false, true))
        {
            try
            {
                //
                // Attempt to acquire the lock for only a few milliseconds, so we don't hog the long-running thread.
                //
                try (Holder holder = accessState(maxWait, true))
                {
                    if (holder.wasAcquired())
                    {
                        while (holder.canKeepRunning(true, maxWait))
                        {
                            ZonedDateTime now           = TimeUtils.now();
                            ZonedDateTime nowMinusDelay = now.minus(c_samplesFlushDelay);

                            final State state                = requireNonNull(m_state);
                            boolean     shouldProcessObjects = state.summaryForStagedResults.hasPendingObjectUpdates();
                            boolean     shouldProcessSamples = state.summaryForStagedResults.shouldFlush() || TimeUtils.isBeforeOrNull(m_lastSamplesFlush, nowMinusDelay);

                            if (!shouldProcessObjects && !shouldProcessSamples)
                            {
                                // Nothing to do, go to sleep.
                                allDone = true;
                                return;
                            }

                            //
                            // If we have objects to process, suspend the search indexer for five minutes.
                            //
                            if (shouldProcessObjects)
                            {
                                m_app.suspendSearchIndexer(5, TimeUnit.MINUTES);
                            }

                            NextAction nextAction;
                            boolean    reportYield = true;

                            if (shouldProcessSamples)
                            {
                                if (m_app.shouldSuspendResultStagingSpooler())
                                {
                                    nextAction  = NextAction.YieldControl;
                                    reportYield = false;
                                }
                                else
                                {
                                    state.summaryForStagedResults.reportProgress(LoggerInstance, true, false);

                                    try (AsyncGate.Holder ignored = m_app.closeGate(TagsEngine.Gate.class))
                                    {
                                        nextAction = processUpdates(holder, maxWait, StagedResultsProcessingMode.Everything);
                                    }

                                    if (nextAction == NextAction.Done)
                                    {
                                        state.summaryForStagedResults.reportProgress(LoggerInstance, false, true);

                                        purgeStaleRecords();

                                        m_lastSamplesFlush = now;

                                        // Also drop the state, so it doesn't keep growing forever.
                                        holder.flushState();
                                    }
                                }
                            }
                            else
                            {
                                nextAction = processUpdates(holder, maxWait, StagedResultsProcessingMode.OnlyObjects);
                            }

                            m_failureBackoff = 0;

                            if (nextAction == NextAction.YieldControl)
                            {
                                if (reportYield)
                                {
                                    state.summaryForStagedResults.reportYield();
                                }
                                break;
                            }
                        }
                    }
                }
                catch (Throwable t)
                {
                    LoggerInstance.error("Detected a problem while processing staged results: %s", t);

                    m_failureBackoff = Math.max(m_failureBackoff, 500) * 2;
                    Executors.safeSleep(m_failureBackoff);
                }
            }
            finally
            {
                m_processingInBackground.set(false);

                // Only reschedule if not exiting.
                if (!allDone && m_keepRunning.get())
                {
                    Executors.scheduleOnDefaultPool(this::importInBackgroundQueue, 500, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    private NextAction processUpdates(Holder holder,
                                      Duration maxWait,
                                      StagedResultsProcessingMode mode) throws
                                                                        Exception
    {
        LoggerInstance.debugObnoxious("Closing gate");

        try (AsyncGate.Holder ignored = m_app.closeGate(HibernateSearch.Gate.class)) // Disable indexing while we drain the records
        {
            LoggerInstance.debugObnoxious("Gate closed");

            try
            {
                while (true)
                {
                    if (!holder.canKeepRunning(true, maxWait))
                    {
                        // Don't purge stale records on shutdown.
                        return NextAction.YieldControl;
                    }

                    NextAction result;

                    try (var lockHolder = lockTable())
                    {
                        result = processUpdatesUnderLock(mode);

                        LoggerInstance.debugObnoxious("processUpdatesUnderLock: %s", result);
                    }
                    catch (LockTimeoutException t)
                    {
                        LoggerInstance.debugVerbose("Failed to acquire lock, retrying...");
                        return NextAction.YieldControl;
                    }

                    if (result != NextAction.ContinueProcessing)
                    {
                        return result;
                    }
                }
            }
            finally
            {
                LoggerInstance.debugObnoxious("Opening gate");
            }
        }
    }

    private NextAction processUpdatesUnderLock(StagedResultsProcessingMode mode) throws
                                                                                 Exception
    {
        NextAction nextAction;

        try (SessionHolder sessionHolder = SessionHolder.createWithNewSessionWithTransaction(m_app, null, Optio3DbRateLimiter.System))
        {
            while (true)
            {
                if (mode.shouldProcessOnlyMarkedObjects())
                {
                    nextAction = processRoots(sessionHolder, StagedResultsProcessingMode.OnlyMarkedObjects);
                    if (nextAction == NextAction.YieldControl)
                    {
                        break;
                    }

                    if (nextAction == NextAction.ContinueProcessing)
                    {
                        continue;
                    }
                }

                if (mode.shouldProcessObjects())
                {
                    nextAction = processRoots(sessionHolder, StagedResultsProcessingMode.OnlyObjects);
                    if (nextAction == NextAction.YieldControl)
                    {
                        break;
                    }

                    if (nextAction == NextAction.ContinueProcessing)
                    {
                        continue;
                    }
                }

                if (mode.shouldProcessSamples())
                {
                    nextAction = processRoots(sessionHolder, StagedResultsProcessingMode.OnlySamples);
                    if (nextAction == NextAction.YieldControl)
                    {
                        break;
                    }

                    if (nextAction == NextAction.ContinueProcessing)
                    {
                        continue;
                    }
                }

                // All done, we can exit.
                nextAction = NextAction.Done;
                break;
            }

            sessionHolder.commit();
        }

        return nextAction;
    }

    //--//

    private TableLockHolder lockTable()
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithoutTransaction(m_app, null, Optio3DbRateLimiter.System))
        {
            final RecordHelper<ResultStagingRecord> helper_staging = holder.createHelper(ResultStagingRecord.class);

            return helper_staging.lockTable(2, TimeUnit.SECONDS);
        }
    }

    //--//

    private NextAction processRoots(SessionHolder sessionHolder,
                                    StagedResultsProcessingMode mode) throws
                                                                      Exception
    {
        final State state = requireNonNull(m_state);

        for (StagedResultsSummary.ForRoot root : state.summaryForStagedResults.roots.values())
        {
            NextAction nextAction = processRoot(sessionHolder, state.summaryForStagedResults, mode, root);
            if (nextAction != NextAction.Done)
            {
                return nextAction;
            }
        }

        return NextAction.Done;
    }

    private NextAction processRoot(SessionHolder sessionHolder,
                                   StagedResultsSummary stagedResultsSummary,
                                   StagedResultsProcessingMode mode,
                                   StagedResultsSummary.ForRoot staged_root) throws
                                                                             Exception
    {
        NextAction nextAction = NextAction.Done;

        final State                     state       = requireNonNull(m_state);
        final RecordHelper<AssetRecord> helper_root = sessionHolder.createHelper(AssetRecord.class);
        final AssetRecord               rec_root    = helper_root.getOrNull(staged_root.sysId);
        if (rec_root == null)
        {
            // Root deleted, forcibly mark staged results as processed.
            staged_root.forceMarkAsProcessed(stagedResultsSummary);

            return nextAction;
        }

        final DiscoveredAssetsSummary.ForRoot db_root = state.summaryForDatabase.registerRoot(staged_root.sysId, staged_root.rootKind, true);

        LoggerInstance.debugVerbose("processRoot: %s - %s", rec_root.getSysId(), rec_root.getName());

        BatchIngestionContext context = new BatchIngestionContext(sessionHolder,
                                                                  state.summaryForDatabase,
                                                                  state.summaryForStagedResults,
                                                                  m_keepRunning,
                                                                  m_importStagedResults,
                                                                  m_yieldControl,
                                                                  mode,
                                                                  100,
                                                                  db_root,
                                                                  rec_root);

        for (StagedResultsSummary.ForAsset forAsset : staged_root.sortByTimestamp(mode.shouldProcessObjects()))
        {
            if (!state.summaryForStagedResults.mustFlush() && context.shouldReschedule())
            {
                // Stop processing, new result records in the database.
                LoggerInstance.debug("Database activity detected, restarting processing...");
                nextAction = NextAction.YieldControl;
                break;
            }

            processAsset(context, forAsset);

            if (context.hasMadeProgress())
            {
                nextAction = NextAction.ContinueProcessing;
            }
        }

        context.flush();

        return nextAction;
    }

    private void processAsset(BatchIngestionContext context,
                              StagedResultsSummary.ForAsset forAsset) throws
                                                                      Exception
    {
        CommonProtocolHandlerForIngestion<?, ?> ingestor = forAsset.decoder.prepareIngestor(context.cfg, LoggerInstance, forAsset);
        ingestor.process(context);
    }

    //--//

    private void purgeStaleRecords()
    {
        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(m_app, null, Optio3DbRateLimiter.System))
        {
            final RecordHelper<ResultStagingRecord> helper_staging = holder.createHelper(ResultStagingRecord.class);

            ZonedDateTime now            = TimeUtils.now();
            ZonedDateTime purgeThreshold = now.minus(c_storingLength);

            ResultStagingRecord.purgeProcessedRecords(helper_staging, purgeThreshold);

            holder.commit();
        }
        catch (Throwable e)
        {
            LoggerInstance.error("Encountered problem while purging stale staged results: %s", e);
        }
    }
}
