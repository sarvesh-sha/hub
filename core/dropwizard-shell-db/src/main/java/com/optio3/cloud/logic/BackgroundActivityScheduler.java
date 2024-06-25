/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.logic;

import static com.optio3.asyncawait.CompileTime.await;

import java.lang.management.ThreadInfo;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;
import com.optio3.asyncawait.AsyncBackground;
import com.optio3.asyncawait.AsyncDelay;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.annotation.Optio3RecurringProcessor;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.DatabaseActivity;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.ITableLockProvider;
import com.optio3.cloud.persistence.RecordForBackgroundActivity;
import com.optio3.cloud.persistence.RecordForBackgroundActivityChunk;
import com.optio3.cloud.persistence.RecordForWorker;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;
import com.optio3.serialization.Reflection;
import com.optio3.service.IServiceProvider;
import com.optio3.util.MonotonousTime;
import com.optio3.util.StackTraceAnalyzer;
import com.optio3.util.TimeUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

public abstract class BackgroundActivityScheduler<R extends RecordForBackgroundActivity<R, C, W>, C extends RecordForBackgroundActivityChunk<R, C, W>, W extends RecordForWorker<W>> implements IServiceProvider
{
    private class RecurringProcessorState
    {
        private final String                   m_handlerName;
        private final RecurringActivityHandler m_handler;
        private       CompletableFuture<Void>  m_pendingWorker;
        private       boolean                  m_runningWorker;
        private       ZonedDateTime            m_nextActivation;
        private       boolean                  m_startupDelay;
        private       boolean                  m_reschedule;

        private RecurringProcessorState(Class<? extends RecurringActivityHandler> handlerClass)
        {
            m_handlerName = handlerClass.getCanonicalName();
            m_handler     = Reflection.newInstance(handlerClass);

            rescheduleNow();

            Duration startupDelay = m_handler.startupDelay();
            if (startupDelay != null)
            {
                m_nextActivation = m_nextActivation.plus(startupDelay);
                m_startupDelay   = true;
            }

            RecurringActivityHandler.ForTable hForTable = Reflection.as(m_handler, RecurringActivityHandler.ForTable.class);
            if (hForTable != null)
            {
                m_regDbActivity.subscribeToTable(hForTable.getEntityClass(), (dbEvent) ->
                {
                    if (hForTable.shouldTrigger(dbEvent))
                    {
                        rescheduleNow();
                        trigger();
                    }
                });
            }
        }

        private void rescheduleNow()
        {
            if (!m_startupDelay) // Don't override the startup delay.
            {
                m_reschedule     = true;
                m_nextActivation = TimeUtils.now();
            }
        }

        private void close()
        {
            try
            {
                m_handler.shutdown();
            }
            catch (Throwable t)
            {
                LoggerInstance.error("Recurring Activity Handler '%s' returned an error on shutdown: %s", m_handlerName, t);
            }

            CompletableFuture<Void> pending;

            synchronized (m_handler)
            {
                if (m_pendingWorker != null && m_pendingWorker.cancel(false))
                {
                    m_pendingWorker = null;
                }

                pending = m_pendingWorker;
            }

            if (pending != null)
            {
                try
                {
                    pending.get(10, TimeUnit.SECONDS);
                }
                catch (Throwable t)
                {
                    LoggerInstance.error("Recurring Activity Handler '%s' returned an error on shutdown: %s", m_handlerName, t);
                }
            }
        }

        private void advance()
        {
            synchronized (m_handler)
            {
                if (m_pendingWorker != null)
                {
                    if (m_pendingWorker.isDone())
                    {
                        m_pendingWorker = null;
                    }
                    else if (m_reschedule && !m_runningWorker)
                    {
                        if (m_pendingWorker.cancel(false))
                        {
                            m_pendingWorker = null;
                        }
                    }
                }

                if (m_pendingWorker == null)
                {
                    m_reschedule = false;

                    ZonedDateTime now = TimeUtils.now();

                    Duration delay = Duration.between(now, m_nextActivation);
                    if (delay.isNegative())
                    {
                        delay = Duration.of(0, ChronoUnit.SECONDS);
                    }

                    try
                    {
                        LoggerInstance.debug("Recurring Activity Handler '%s': queued for %s", m_handlerName, m_nextActivation.toLocalDateTime());

                        m_pendingWorker = execute(delay.toMillis(), TimeUnit.MILLISECONDS);
                    }
                    catch (Throwable t)
                    {
                        LoggerInstance.error("Recurring Activity Handler '%s' returned an error: %s", m_handlerName, t);
                    }
                }
            }
        }

        @AsyncBackground
        private CompletableFuture<Void> execute(@AsyncDelay long delay,
                                                @AsyncDelay TimeUnit delayUnit) throws
                                                                                Exception
        {
            m_runningWorker = true;
            m_startupDelay  = false;

            ZonedDateTime nextActivation;

            try
            {
                ITableLockProvider provider = getService(ITableLockProvider.class);
                if (provider == null)
                {
                    throw new RuntimeException("Table-level locking not supported");
                }

                SessionProvider sessionProvider = new SessionProvider(m_app, null, Optio3DbRateLimiter.Background);

                try (var lock = provider.lockTable(sessionProvider, m_handler.getClass(), 30, TimeUnit.MINUTES))
                {
                    LoggerInstance.debug("Recurring Activity Handler '%s': starting...", m_handlerName);

                    nextActivation = await(m_handler.process(sessionProvider));

                    if (nextActivation == null)
                    {
                        nextActivation = TimeUtils.future(15, TimeUnit.MINUTES);
                    }

                    LoggerInstance.debug("Recurring Activity Handler '%s': next activation at %s", m_handlerName, nextActivation.toLocalDateTime());
                }
            }
            catch (Exception t)
            {
                nextActivation = TimeUtils.future(15, TimeUnit.MINUTES);

                if (t instanceof CancellationException)
                {
                    // Not a failure.
                }
                else
                {
                    LoggerInstance.error("Recurring Activity Handler '%s' returned an error: %s", m_handlerName, t);
                }
            }
            finally
            {
                m_runningWorker = false;
            }

            synchronized (m_handler)
            {
                m_pendingWorker  = null;
                m_nextActivation = nextActivation;
                m_reschedule     = true;
            }

            trigger();

            return AsyncRuntime.NullResult;
        }
    }

    private static final int MAX_SLEEP_MSEC = 2000;

    private static final Collection<BackgroundActivityStatus> s_completedSet = Collections.unmodifiableList(Lists.newArrayList(BackgroundActivityStatus.WAITING,
                                                                                                                               BackgroundActivityStatus.SLEEPING,
                                                                                                                               BackgroundActivityStatus.EXECUTING,
                                                                                                                               BackgroundActivityStatus.CANCELLED,
                                                                                                                               BackgroundActivityStatus.COMPLETED,
                                                                                                                               BackgroundActivityStatus.FAILED));

    public static final Logger LoggerInstance = new Logger(BackgroundActivityScheduler.class);

    private final AbstractApplicationWithDatabase<?> m_app;
    private final Class<R>                           m_entityClass;

    private final Object                        m_lock                = new Object();
    private final List<RecurringProcessorState> m_recurringProcessors = Lists.newArrayList();
    private final AtomicInteger                 m_pendingActivities   = new AtomicInteger();

    private Thread                           m_worker;
    private boolean                          m_shutdown;
    private DatabaseActivity.LocalSubscriber m_regDbActivity;

    private MonotonousTime m_completedActivities;

    //--//

    protected BackgroundActivityScheduler(AbstractApplicationWithDatabase<?> app,
                                          Class<R> entityClass)
    {
        m_app         = app;
        m_entityClass = entityClass;
    }

    public void trigger()
    {
        synchronized (m_lock)
        {
            m_lock.notifyAll();
        }
    }

    public void start(String packagePrefix)
    {
        synchronized (m_lock)
        {
            if (m_worker != null)
            {
                return;
            }

            m_shutdown = false;

            m_regDbActivity = DatabaseActivity.LocalSubscriber.create(m_app.getServiceNonNull(MessageBusBroker.class));

            m_regDbActivity.subscribeToTable(m_entityClass, (dbEvent) ->
            {
                switch (dbEvent.action)
                {
                    case DELETE:
                    case INSERT:
                    case UPDATE_DIRECT:
                        trigger();
                        break;

                    default:
                        break;
                }
            });

            if (packagePrefix != null)
            {
                Reflections                                    reflections = new Reflections(packagePrefix, new SubTypesScanner(false));
                Set<Class<? extends RecurringActivityHandler>> candidates  = reflections.getSubTypesOf(RecurringActivityHandler.class);

                for (Class<? extends RecurringActivityHandler> candidate : candidates)
                {
                    if (candidate.isAnnotationPresent(Optio3RecurringProcessor.class))
                    {
                        final RecurringProcessorState e = new RecurringProcessorState(candidate);
                        m_recurringProcessors.add(e);
                    }
                }
            }

            m_worker = new Thread(this::process);
            m_worker.setName("Background Scheduler");
            m_worker.setDaemon(true);
            m_worker.start();
        }
    }

    public void stop(long timeout,
                     TimeUnit unit)
    {
        synchronized (m_lock)
        {
            if (m_worker == null)
            {
                return;
            }

            m_shutdown = true;
            m_lock.notifyAll();
        }

        LoggerInstance.info("Signalling BackgroundActivityScheduler to stop...");

        MonotonousTime limit = TimeUtils.computeTimeoutExpiration(timeout, unit);
        while (!TimeUtils.isTimeoutExpired(limit))
        {
            synchronized (m_lock)
            {
                if (m_worker == null)
                {
                    return;
                }

                m_lock.notifyAll();
            }

            Executors.safeSleep(1000);
        }

        LoggerInstance.warn("Failed to stop scheduler before deadline!");

        Map<StackTraceAnalyzer, List<ThreadInfo>> uniqueStackTraces = StackTraceAnalyzer.allThreadInfos();
        List<String>                              lines             = StackTraceAnalyzer.printThreadInfos(false, uniqueStackTraces);
        for (String line : lines)
        {
            LoggerInstance.warn(line);
        }
    }

    protected abstract void initialize(SessionHolder holder);

    protected abstract W getWorker(SessionHolder sessionHolder);

    protected abstract ZonedDateTime findNextActivation(RecordHelper<R> helper);

    protected abstract TypedRecordIdentityList<R> listReadyActivities(RecordHelper<R> helper);

    protected abstract TypedRecordIdentityList<R> listActivities(RecordHelper<R> helper,
                                                                 W hostAffinity,
                                                                 Collection<BackgroundActivityStatus> filterStatus);

    //--//

    private void process()
    {
        boolean sleepAfterFailure = false;

        LoggerInstance.info("Starting BackgroundActivityScheduler");

        try
        {
            try (SessionHolder holder = SessionHolder.createWithNewSessionWithoutTransaction(this, null, Optio3DbRateLimiter.Background))
            {
                initialize(holder);
            }
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Initialization encountered a problem: %s", t);
        }

        LoggerInstance.info("Started BackgroundActivityScheduler");

        processOrphanedActivities();

        while (!m_shutdown)
        {
            try
            {
                if (sleepAfterFailure)
                {
                    sleepAfterFailure = false;
                    synchronized (m_lock)
                    {
                        if (m_shutdown)
                        {
                            break;
                        }

                        m_lock.wait(1000);
                    }
                }

                processCompletedActivities();

                processReadyActivities();

                for (RecurringProcessorState recurringProcessor : m_recurringProcessors)
                {
                    recurringProcessor.advance();
                }

                ZonedDateTime nextActivation = null;

                try (SessionHolder holderReadonly = SessionHolder.createWithNewSessionWithoutTransaction(this, null, Optio3DbRateLimiter.System))
                {
                    RecordHelper<R> helper = holderReadonly.createHelper(m_entityClass);

                    nextActivation = TimeUtils.min(nextActivation, findNextActivation(helper));
                }

                //
                // Nothing new showed up, let's try to go to sleep.
                //
                if (nextActivation == null)
                {
                    LoggerInstance.debug("No ready activities, sleeping...");
                    synchronized (m_lock)
                    {
                        if (m_shutdown)
                        {
                            break;
                        }

                        m_lock.wait();
                    }
                }
                else
                {
                    LoggerInstance.debug("Next activation: %s", nextActivation.toLocalDateTime());

                    ZonedDateTime now = TimeUtils.now();

                    long waitMillis = now.until(nextActivation, ChronoUnit.MILLIS);
                    if (waitMillis > 0)
                    {
                        LoggerInstance.debug("Wait time: %dmsec", waitMillis);
                        synchronized (m_lock)
                        {
                            if (m_shutdown)
                            {
                                break;
                            }

                            m_lock.wait(Math.min(MAX_SLEEP_MSEC, waitMillis));
                        }
                    }
                }
            }
            catch (Throwable e)
            {
                LoggerInstance.error("Encountered error while processing activities: %s", e);

                sleepAfterFailure = true;
            }
        }

        LoggerInstance.info("Stopping BackgroundActivityScheduler...");

        synchronized (m_lock)
        {
            while (m_pendingActivities.get() > 0)
            {
                try
                {
                    m_lock.wait(10000);
                }
                catch (InterruptedException e)
                {
                    // Ignore
                }
            }
        }

        synchronized (m_lock)
        {
            for (RecurringProcessorState recurringProcessor : m_recurringProcessors)
            {
                recurringProcessor.close();
            }

            if (m_regDbActivity != null)
            {
                m_regDbActivity.close();
                m_regDbActivity = null;
            }

            m_recurringProcessors.clear();
            m_shutdown = false;
            m_worker   = null;
        }

        LoggerInstance.info("Stopped BackgroundActivityScheduler");
    }

    private void processReadyActivities()
    {
        TypedRecordIdentityList<R> list;

        try (SessionHolder holderReadonly = SessionHolder.createWithNewSessionWithoutTransaction(this, null, Optio3DbRateLimiter.System))
        {
            RecordHelper<R> helper = holderReadonly.createHelper(m_entityClass);

            LoggerInstance.debug("processReadyActivities: checking...");
            list = listReadyActivities(helper);
            LoggerInstance.debug("processReadyActivities: got %d entries", list.size());
        }

        for (RecordIdentity ri : list)
        {
            LoggerInstance.debug("processReadyActivities: activity %s", ri.sysId);

            BackgroundActivityHandler<R, C, W> handler = shouldProcessActivity(ri.sysId);
            if (handler != null)
            {
                LoggerInstance.debug("processActivity: activity %s (%s)", ri.sysId, handler.displayName);

                try
                {
                    m_pendingActivities.incrementAndGet();

                    CompletableFuture<Void> future = handler.process();
                    future.whenComplete((unused, throwable) ->
                                        {
                                            m_pendingActivities.decrementAndGet();
                                            trigger();
                                        });
                }
                catch (Throwable ex)
                {
                    // This should never happen, since we do everything in a background async thread...
                    LoggerInstance.error("Failed to start processing handler for '%s'", handler.displayName);
                }
            }
        }
    }

    private void processOrphanedActivities()
    {

        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
        {
            RecordHelper<R> helper = holder.createHelper(m_entityClass);

            var orphanedSet = Collections.unmodifiableList(Lists.newArrayList(BackgroundActivityStatus.EXECUTING, BackgroundActivityStatus.EXECUTING_BUT_CANCELLING));

            for (RecordIdentity ri : listActivities(helper, getWorker(holder), orphanedSet))
            {
                R rec = helper.getOrNull(ri.sysId);
                if (rec != null)
                {
                    rec.transitionToActive(null);

                    holder.commitAndBeginNewTransactionIfNeeded(100);
                }
            }

            holder.commit();
        }
        catch (Throwable t)
        {
            LoggerInstance.error("processOrphanedActivities failed: %s", t);
        }
    }

    private void processCompletedActivities()
    {
        if (TimeUtils.isTimeoutExpired(m_completedActivities))
        {
            final ZonedDateTime now             = TimeUtils.now();
            final ZonedDateTime nowMinusOneHour = now.minus(1, ChronoUnit.HOURS);
            final ZonedDateTime nowMinusOneDay  = now.minus(1, ChronoUnit.DAYS);
            List<String>        deleteList      = Lists.newArrayList();

            try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
            {
                RecordHelper<R> helper = holder.createHelper(m_entityClass);

                for (RecordIdentity ri : listActivities(helper, null, s_completedSet))
                {
                    R rec = helper.getOrNull(ri.sysId);
                    if (rec != null)
                    {
                        final ZonedDateTime updatedOn = rec.getUpdatedOn();

                        BackgroundActivityStatus status = rec.getStatus();
                        switch (status.getNormalStatus())
                        {
                            case SLEEPING:
                                rec.checkForSleeping(now);
                                break;

                            case WAITING:
                                rec.checkForSubActivitiesDone(now);
                                break;

                            case COMPLETED:
                            case CANCELLED:
                                // Keep completed activites for one hour.
                                if (updatedOn.isBefore(nowMinusOneHour))
                                {
                                    LoggerInstance.debug("processCompletedActivities: removing old completed activity %s, older than %s", ri.sysId, updatedOn);
                                    deleteList.add(ri.sysId);
                                }
                                break;

                            case FAILED:
                                // Keep failed activites for one day.
                                if (updatedOn.isBefore(nowMinusOneDay))
                                {
                                    LoggerInstance.debug("processCompletedActivities: removing old failed activity %s, older than %s", ri.sysId, updatedOn);
                                    deleteList.add(ri.sysId);
                                }
                                break;
                        }
                    }

                    holder.commitAndBeginNewTransactionIfNeeded(100);
                }

                holder.commit();
            }
            catch (Throwable t)
            {
                LoggerInstance.error("processCompletedActivities failed: %s", t);
            }

            if (!deleteList.isEmpty())
            {
                try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
                {
                    RecordHelper<R> helper = holder.createHelper(m_entityClass);

                    for (String sysId : deleteList)
                    {
                        R rec = helper.getOrNull(sysId);
                        if (rec != null)
                        {
                            final Set<R> waitingActivities = rec.getWaitingActivities();
                            if (!waitingActivities.isEmpty())
                            {
                                // Don't delete if another activity depends on us.
                                continue;
                            }

                            rec.clearSubActivities();

                            helper.delete(rec);

                            holder.commitAndBeginNewTransactionIfNeeded(100);
                        }
                    }

                    holder.commit();
                }
                catch (Throwable t)
                {
                    LoggerInstance.error("deleteOldActivity failed on batch: %s", t);

                    //
                    // If the batch delete failed, delete one activity per transaction.
                    //
                    for (String sysId : deleteList)
                    {
                        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.System))
                        {
                            RecordHelper<R> helper = holder.createHelper(m_entityClass);

                            RecordLocked<R> lock = helper.getWithLockOrNull(sysId, 10, TimeUnit.SECONDS);
                            if (lock != null)
                            {
                                R rec = lock.get();

                                final Set<R> waitingActivities = rec.getWaitingActivities();
                                if (waitingActivities.isEmpty()) // Don't delete if another activity depends on us.
                                {
                                    rec.clearSubActivities();

                                    helper.flush();

                                    helper.delete(rec);

                                    holder.commit();
                                }
                            }
                        }

                        catch (Throwable t2)
                        {
                            LoggerInstance.error("deleteOldActivity failed on '%s': %s", sysId, t2);
                        }
                    }
                }
            }

            m_completedActivities = TimeUtils.computeTimeoutExpiration(5, TimeUnit.MINUTES);
        }
    }

    private BackgroundActivityHandler<R, C, W> shouldProcessActivity(String sysId)
    {
        try (SessionHolder sessionHolder = SessionHolder.createWithNewSessionWithTransaction(this, null, Optio3DbRateLimiter.Background))
        {
            RecordHelper<R> helper = sessionHolder.createHelper(m_entityClass);

            RecordLocked<R> lock = helper.getWithLockOrNull(sysId, 10, TimeUnit.SECONDS);
            if (lock == null)
            {
                return null;
            }

            R                        rec            = lock.get();
            BackgroundActivityStatus status         = rec.getStatus();
            ZonedDateTime            nextActivation = rec.getNextActivation();

            LoggerInstance.debug("shouldProcessActivity: %s : %s : %s : %s", sysId, rec.getTitle(), status, nextActivation.toLocalDateTime());

            ZonedDateTime now = TimeUtils.now();

            switch (status)
            {
                case ACTIVE:
                case SLEEPING:
                    if (now.isAfter(nextActivation))
                    {
                        BackgroundActivityHandler<R, C, W> handler = rec.getHandler(sessionHolder);

                        rec.transitionToExecuting(getWorker(sessionHolder));

                        sessionHolder.commit();

                        return handler;
                    }
                    break;

                case ACTIVE_BUT_CANCELLING:
                case SLEEPING_BUT_CANCELLIN:
                case PAUSED_BUT_CANCELLING:
                {
                    BackgroundActivityHandler<R, C, W> handler = rec.getHandler(sessionHolder);

                    handler.markAsFailed(new CancellationException());
                    handler.drainPostActions(sessionHolder, rec);

                    sessionHolder.commit();
                }
                break;
            }
        }
        catch (Throwable ex)
        {
            LoggerInstance.error("shouldProcessActivity: activity %s failed with exception %s", sysId, ex);
        }

        return null;
    }
}
