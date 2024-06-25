/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Tuple;
import javax.persistence.criteria.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.cloud.exception.NotFoundException;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.logic.BackgroundActivityScheduler;
import com.optio3.cloud.logic.BackgroundActivityStatus;
import com.optio3.cloud.logic.IBackgroundActivityProgress;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.SortCriteria;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.scheduler.BackgroundActivityFilterPair;
import com.optio3.cloud.model.scheduler.BackgroundActivityFilterRequest;
import com.optio3.cloud.model.scheduler.BaseBackgroundActivityProgress;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.FunctionWithException;
import org.apache.commons.lang3.StringUtils;

@MappedSuperclass
public abstract class RecordForBackgroundActivity<R extends RecordForBackgroundActivity<R, C, W>, C extends RecordForBackgroundActivityChunk<R, C, W>, W extends RecordForWorker<W>> extends RecordWithCommonFields
{
    private static class FakeRecordForClassNotFound extends RecordForBackgroundActivity<FakeRecordForClassNotFound, FakeChunkRecordForClassNotFound, FakeWorkerRecordForClassNotFound>
    {
        @Override
        public Set<FakeRecordForClassNotFound> getWaitingActivities()
        {
            return Collections.emptySet();
        }

        @Override
        public Set<FakeRecordForClassNotFound> getSubActivities()
        {
            return Sets.newHashSet();
        }

        @Override
        public FakeWorkerRecordForClassNotFound getWorker()
        {
            return null;
        }

        @Override
        public void setWorker(FakeWorkerRecordForClassNotFound worker)
        {
        }
    }

    private static class FakeChunkRecordForClassNotFound extends RecordForBackgroundActivityChunk<FakeRecordForClassNotFound, FakeChunkRecordForClassNotFound, FakeWorkerRecordForClassNotFound>
    {
        @Override
        public FakeRecordForClassNotFound getOwningActivity()
        {
            return null;
        }

        @Override
        public void setOwningActivity(FakeRecordForClassNotFound owningActivity)
        {
        }
    }

    private static class FakeWorkerRecordForClassNotFound implements RecordForWorker<FakeWorkerRecordForClassNotFound>
    {
    }

    private static class ClassNotFoundHandler extends BackgroundActivityHandler<FakeRecordForClassNotFound, FakeChunkRecordForClassNotFound, FakeWorkerRecordForClassNotFound>
    {
        private final String m_handler;

        public ClassNotFoundHandler(String handler)
        {
            m_handler = handler;
        }

        @Override
        protected Class<FakeChunkRecordForClassNotFound> getChunkClass()
        {
            return FakeChunkRecordForClassNotFound.class;
        }

        @Override
        public void configureContext()
        {
        }

        @Override
        public String getTitle()
        {
            return String.format("Class '%s' Not Found", m_handler);
        }

        @Override
        public RecordLocator<? extends RecordWithCommonFields> getContext()
        {
            return null;
        }
    }

    //--//

    public static final List<SortCriteria>                   SortByNextActivation = Collections.unmodifiableList(SortCriteria.build("nextActivation", true));
    public static final Collection<BackgroundActivityStatus> ReadySet;
    public static final Collection<BackgroundActivityStatus> NotDoneSet;
    public static final Collection<BackgroundActivityStatus> DoneSet;
    public static final Collection<BackgroundActivityStatus> ActiveSet;

    static
    {
        List<BackgroundActivityStatus> lstDone    = Lists.newArrayList();
        List<BackgroundActivityStatus> lstNotDone = Lists.newArrayList();

        for (BackgroundActivityStatus v : BackgroundActivityStatus.values())
        {
            if (v.isDone())
            {
                lstDone.add(v);
            }
            else
            {
                lstNotDone.add(v);
            }
        }

        NotDoneSet = Collections.unmodifiableList(lstNotDone);
        DoneSet    = Collections.unmodifiableList(lstDone);

        ActiveSet = Collections.unmodifiableList(Lists.newArrayList(BackgroundActivityStatus.ACTIVE,
                                                                    BackgroundActivityStatus.ACTIVE_BUT_CANCELLING,
                                                                    BackgroundActivityStatus.EXECUTING,
                                                                    BackgroundActivityStatus.EXECUTING_BUT_CANCELLING));

        // The ready set also includes cancelling states, to trigger actual cancellation.
        ReadySet = Collections.unmodifiableList(Lists.newArrayList(BackgroundActivityStatus.ACTIVE,
                                                                   BackgroundActivityStatus.ACTIVE_BUT_CANCELLING,
                                                                   BackgroundActivityStatus.WAITING_BUT_CANCELLING,
                                                                   BackgroundActivityStatus.SLEEPING,
                                                                   BackgroundActivityStatus.SLEEPING_BUT_CANCELLIN,
                                                                   BackgroundActivityStatus.PAUSED_BUT_CANCELLING));
    }

    //--//

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "was_processing", nullable = false)
    private boolean wasProcessing;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BackgroundActivityStatus status;

    @Column(name = "rpc_id")
    private String rpcId;

    @Column(name = "timeout")
    private Duration timeout;

    @Column(name = "next_activation", nullable = false)
    private ZonedDateTime nextActivation;

    //--//

    @Column(name = "last_activation")
    private ZonedDateTime lastActivation;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_activation_status")
    private BackgroundActivityStatus lastActivationStatus;

    @Lob
    @Column(name = "last_activation_failure")
    private String lastActivationFailure;

    @Lob
    @Column(name = "last_activation_failure_trace")
    private String lastActivationFailureTrace;

    //--//

    @Column(name = "handler", nullable = false)
    private String handler;

    @Column(name = "handler_key")
    private String handlerKey;

    @Lob
    @Column(name = "binary_handler_state")
    @Basic(fetch = FetchType.LAZY)
    private byte[] binaryHandlerState;

    //--//

    public static <R extends RecordForBackgroundActivity<R, ?, ?>, H extends IBackgroundActivityProgress<P>, P extends BaseBackgroundActivityProgress> P getProgress(RecordHelper<R> helper,
                                                                                                                                                                     String sysId,
                                                                                                                                                                     boolean detailed,
                                                                                                                                                                     Class<H> clzHandler)
    {
        R rec = helper.getOrNull(sysId);
        if (rec != null)
        {
            SessionHolder                      sessionHolder = helper.currentSessionHolder();
            BackgroundActivityHandler<R, ?, ?> hRaw          = rec.getHandler(sessionHolder);
            H                                  h             = Reflection.as(hRaw, clzHandler);
            if (h != null)
            {
                P res = h.fetchProgress(sessionHolder, detailed);
                res.status = rec.getStatus();
                return res;
            }
        }

        return null;
    }

    public static <R extends RecordForBackgroundActivity<R, ?, ?>, H extends IBackgroundActivityProgress<P>, P extends BaseBackgroundActivityProgress> InputStream streamContents(RecordHelper<R> helper,
                                                                                                                                                                                  String sysId,
                                                                                                                                                                                  Class<H> clzHandler) throws
                                                                                                                                                                                                       IOException
    {
        R rec = helper.getOrNull(sysId);
        if (rec != null)
        {
            SessionHolder                      sessionHolder = helper.currentSessionHolder();
            BackgroundActivityHandler<R, ?, ?> hRaw          = rec.getHandler(sessionHolder);
            H                                  h             = Reflection.as(hRaw, clzHandler);
            if (h != null)
            {
                return h.streamContents();
            }
        }

        throw new NotFoundException(sysId);
    }

    public static <R extends RecordForBackgroundActivity<R, ?, ?>> R cancelActivity(RecordHelper<R> helper,
                                                                                    String sysId)
    {
        RecordLocked<R> lock = helper.getWithLockOrNull(sysId, 10, TimeUnit.SECONDS);
        if (lock == null)
        {
            return null;
        }

        R                        rec    = lock.get();
        BackgroundActivityStatus status = rec.getStatus();
        if (!status.isDone())
        {
            rec.transitionToCancelling();
        }

        return rec;
    }

    //--//

    protected RecordForBackgroundActivity()
    {
        ensureNextActivation(null, true);
    }

    protected static <R extends RecordForBackgroundActivity<R, C, W>, C extends RecordForBackgroundActivityChunk<R, C, W>, W extends RecordForWorker<W>> R newInstance(SessionHolder sessionHolder,
                                                                                                                                                                       BackgroundActivityHandler<R, C, W> handler,
                                                                                                                                                                       ZonedDateTime nextActivation,
                                                                                                                                                                       Class<R> clz)
    {
        requireNonNull(sessionHolder);
        requireNonNull(handler);

        R rec = Reflection.newInstance(clz);

        handler.setSessionProvider(sessionHolder.getSessionProvider());

        if (nextActivation == null)
        {
            nextActivation = TimeUtils.now();
        }

        String title = handler.getTitle();

        // Truncate long title.
        if (title.length() > 240)
        {
            title = title.substring(0, 240) + "...";
        }

        rec.setTitle(title);

        rec.setHandler(handler);
        rec.transitionToActive(nextActivation);

        sessionHolder.persistEntity(rec);

        handler.setActivity(sessionHolder.createLocator(rec));

        return rec;
    }

    public abstract Set<R> getWaitingActivities();

    public abstract Set<R> getSubActivities();

    public void clearSubActivities()
    {
        getSubActivities().clear();
    }

    //--//

    public abstract W getWorker();

    public abstract void setWorker(W worker);

    public String getRpcId()
    {
        return rpcId;
    }

    public void setRpcId(W worker,
                         String rpcId)
    {
        this.rpcId = rpcId;

        setWorker(worker);
    }

    //--//

    public String getDisplayName()
    {
        if (StringUtils.isBlank(title))
        {
            Class<? extends BackgroundActivityHandler<?, ?, ?>> clz = getHandlerClass();
            if (clz != null)
            {
                return String.format("%s / %s", clz.getSimpleName(), getSysId());
            }

            return getSysId();
        }
        else
        {
            return String.format("%s / %s", title, getSysId());
        }
    }

    public String getTitle()
    {
        return title;
    }

    protected void setTitle(String title)
    {
        this.title = title;
    }

    public RecordIdentity getContext()
    {
        BackgroundActivityHandler<R, C, W> h = getHandler(handler, binaryHandlerState);
        return RecordIdentity.newTypedInstance(h.getContext());
    }

    public BackgroundActivityStatus getStatus()
    {
        return status;
    }

    public Duration getTimeout()
    {
        return timeout;
    }

    public void setTimeout(Duration timeout)
    {
        this.timeout = timeout;
    }

    public ZonedDateTime getNextActivation()
    {
        return nextActivation;
    }

    private void ensureNextActivation(ZonedDateTime nextActivation,
                                      boolean near)
    {
        if (nextActivation == null)
        {
            if (near)
            {
                nextActivation = TimeUtils.now();
            }
            else
            {
                nextActivation = TimeUtils.future(365, TimeUnit.DAYS); // Far, far into the future...
            }
        }

        if (TimeUtils.compare(this.nextActivation, nextActivation) != 0)
        {
            this.nextActivation = nextActivation;
        }
    }

    public ZonedDateTime getLastActivation()
    {
        return lastActivation;
    }

    public void setLastActivation(ZonedDateTime lastActivation)
    {
        this.lastActivation = lastActivation;
    }

    public BackgroundActivityStatus getLastActivationStatus()
    {
        return lastActivationStatus;
    }

    public String getLastActivationFailure()
    {
        return lastActivationFailure;
    }

    public String getLastActivationFailureTrace()
    {
        return lastActivationFailureTrace;
    }

    public Class<? extends BackgroundActivityHandler<R, C, W>> getHandlerClass()
    {
        return getHandlerClass(handler);
    }

    @SuppressWarnings("unchecked")
    public BackgroundActivityHandler<R, C, W> getHandler(SessionHolder sessionHolder)
    {
        BackgroundActivityHandler<R, C, W> h = getHandler(handler, binaryHandlerState);
        h.setSessionProvider(sessionHolder.getSessionProvider());
        h.setActivity(sessionHolder.createLocator((R) this));

        h.displayName = getDisplayName();
        return h;
    }

    @SuppressWarnings("unchecked")
    public static <R extends RecordForBackgroundActivity<R, C, W>, C extends RecordForBackgroundActivityChunk<R, C, W>, W extends RecordForWorker<W>> Class<? extends BackgroundActivityHandler<R, C, W>> getHandlerClass(String handler)
    {
        try
        {
            return (Class<? extends BackgroundActivityHandler<R, C, W>>) Class.forName(handler);
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <R extends RecordForBackgroundActivity<R, C, W>, C extends RecordForBackgroundActivityChunk<R, C, W>, W extends RecordForWorker<W>> BackgroundActivityHandler<R, C, W> getHandler(String handler,
                                                                                                                                                                                                    byte[] binaryHandlerState)
    {
        try
        {
            Class<? extends BackgroundActivityHandler<R, C, W>> clz = getHandlerClass(handler);

            if (binaryHandlerState != null)
            {
                var h = ObjectMappers.deserializeFromGzip(binaryHandlerState, clz);
                h.configureContext();
                return h;
            }
            else
            {
                return Reflection.newInstance(clz);
            }
        }
        catch (Throwable t)
        {
            BackgroundActivityScheduler.LoggerInstance.error("Encountered error while decoding task: %s", t);

            return (BackgroundActivityHandler<R, C, W>) new ClassNotFoundHandler(handler);
        }
    }

    public void setHandler(BackgroundActivityHandler<R, C, W> handler)
    {
        String handlerName = handler.getClass()
                                    .getName();

        if (!StringUtils.equals(this.handler, handlerName))
        {
            this.handler = handlerName;
        }

        String handlerKey = getContextAsKey(handler.getContext());
        if (!StringUtils.equals(this.handlerKey, handlerKey))
        {
            this.handlerKey = handlerKey;
        }

        byte[] newState = ObjectMappers.serializeToGzip(handler);

        if (!Arrays.equals(this.binaryHandlerState, newState))
        {
            this.binaryHandlerState = newState;
        }
    }

    public void checkForSleeping(ZonedDateTime now)
    {
        if (nextActivation != null && nextActivation.isAfter(now))
        {
            return;
        }

        // Wake up with a bit of delay.
        transitionToActive(TimeUtils.future(100, TimeUnit.MILLISECONDS));
    }

    public void checkForSubActivitiesDone(ZonedDateTime now)
    {
        for (RecordForBackgroundActivity<R, C, W> t : getSubActivities())
        {
            BackgroundActivityStatus status = t.getStatus();
            if (!status.isDone())
            {
                if (now != null && nextActivation != null && nextActivation.isBefore(now))
                {
                    // If the activity has a next activation, wake it up even if some of the subactivities is not done.
                    break;
                }

                return;
            }
        }

        // Wake up with a bit of delay.
        transitionToActive(TimeUtils.future(100, TimeUnit.MILLISECONDS));
    }

    private boolean tryToSetStatus(BackgroundActivityStatus newStatus)
    {
        if (status != null)
        {
            if (status.isDone())
            {
                return false;
            }

            if (status.isCancelling() && newStatus.getCancellingStatus() != null)
            {
                newStatus = newStatus.getCancellingStatus();
            }
        }

        status = newStatus;
        return true;
    }

    public void transitionToActive(ZonedDateTime when)
    {
        setWorker(null);

        transitionTo(when, BackgroundActivityStatus.ACTIVE);
    }

    public void transitionToSleeping(ZonedDateTime when)
    {
        setWorker(null);

        transitionTo(when, BackgroundActivityStatus.SLEEPING);
    }

    private void transitionTo(ZonedDateTime when,
                              BackgroundActivityStatus status)
    {
        clearSubActivities();

        if (tryToSetStatus(status))
        {
            ensureNextActivation(when, true);
            wasProcessing = false;
        }
    }

    public void transitionToExecuting(W worker)
    {
        setWorker(worker);

        if (tryToSetStatus(BackgroundActivityStatus.EXECUTING))
        {
            if (!wasProcessing)
            {
                lastActivation = TimeUtils.now();
                wasProcessing  = true;
            }
        }
    }

    public void transitionToCancelling()
    {
        BackgroundActivityStatus newStatus = status.getCancellingStatus();
        if (newStatus != null && !status.isDone())
        {
            status = newStatus;
            ensureNextActivation(null, true);
        }
    }

    public void transitionToWaiting(R subActivity,
                                    ZonedDateTime forcedWakeup)
    {
        requireNonNull(subActivity);

        if (tryToSetStatus(BackgroundActivityStatus.WAITING))
        {
            Set<R> subs = getSubActivities();
            subs.clear();
            subs.add(subActivity);

            ensureNextActivation(forcedWakeup, false);
        }
    }

    public void transitionToWaiting(Collection<R> subActivities,
                                    ZonedDateTime forcedWakeup)
    {
        requireNonNull(subActivities);

        setWorker(null);

        if (tryToSetStatus(BackgroundActivityStatus.WAITING))
        {
            Set<R> subs = getSubActivities();
            subs.clear();
            subs.addAll(subActivities);

            ensureNextActivation(forcedWakeup, false);
        }
    }

    public void transitionToPaused()
    {
        switch (status)
        {
            case ACTIVE:
            case EXECUTING:
                status = BackgroundActivityStatus.PAUSED;
                break;
        }
    }

    public void setResult(SessionHolder sessionHolder,
                          Throwable t)
    {
        setWorker(null);

        if (t != null)
        {
            status                     = (t instanceof CancellationException) ? BackgroundActivityStatus.CANCELLED : BackgroundActivityStatus.FAILED;
            lastActivationStatus       = status;
            lastActivationFailure      = t.getMessage();
            lastActivationFailureTrace = Exceptions.convertStackTraceToString(t);
        }
        else
        {
            status                     = BackgroundActivityStatus.COMPLETED;
            lastActivationStatus       = BackgroundActivityStatus.COMPLETED;
            lastActivationFailure      = null;
            lastActivationFailureTrace = null;
        }

        clearSubActivities();

        wasProcessing = false;

        final SessionProvider sessionProvider = sessionHolder.getSessionProvider();

        for (R sub : getWaitingActivities())
        {
            RecordLocator<R> loc = sessionHolder.createLocator(sub);

            //
            // Because an activity could be waiting on multiple other activity,
            // we cannot check the status of the other activities from inside the transaction of one of them,
            // it could see an incorrect state (since it was fetched before a concurrent change).
            // We queue the checks in the background, such that they'll run in their own transaction, with a consistent view of the DB.
            //
            sessionHolder.scheduleOnTransactionCommit(() ->
                                                      {
                                                          try (SessionHolder subSessionHolder = sessionProvider.newSessionWithTransaction())
                                                          {
                                                              RecordLocked<R> lock = subSessionHolder.fromLocatorWithLockOrNull(loc, 10, TimeUnit.SECONDS);
                                                              if (lock != null)
                                                              {
                                                                  R rec = lock.get();
                                                                  rec.checkForSubActivitiesDone(null);
                                                              }

                                                              subSessionHolder.commit();
                                                          }
                                                      }, 5, TimeUnit.MILLISECONDS);
        }
    }

    //--//

    public long getTimeoutAsSeconds()
    {
        return timeout != null ? timeout.getSeconds() : -1;
    }

    public void setTimeoutAsSeconds(long seconds)
    {
        if (seconds < 0)
        {
            timeout = null;
        }
        else
        {
            timeout = Duration.ofSeconds(seconds);
        }
    }

    public JsonNode getHandlerStateAsJson()
    {
        return ObjectMappers.deserializeFromGzipAsJsonTree(binaryHandlerState);
    }

    //--//

    public static <R extends RecordForBackgroundActivity<R, ?, ?>> ZonedDateTime findNextActivation(RecordHelper<R> helper,
                                                                                                    Consumer<QueryHelperWithCommonFields<ZonedDateTime, R>> callback)
    {
        QueryHelperWithCommonFields<ZonedDateTime, R> jh = new QueryHelperWithCommonFields<>(helper, ZonedDateTime.class);

        jh.cq.select(jh.root.get(RecordForBackgroundActivity_.nextActivation));

        BackgroundActivityFilterRequest filters = new BackgroundActivityFilterRequest();
        filters.sortBy       = SortByNextActivation;
        filters.statusFilter = BackgroundActivityFilterPair.build(ReadySet);

        refineQuery(jh, filters, callback);

        return jh.getFirstResultOrNull();
    }

    public static <R extends RecordForBackgroundActivity<R, ?, ?>> TypedRecordIdentityList<R> list(RecordHelper<R> helper,
                                                                                                   BackgroundActivityFilterRequest filters,
                                                                                                   Consumer<QueryHelperWithCommonFields<Tuple, R>> callback)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            refineQuery(jh, filters, callback);
        });
    }

    protected static <H extends BackgroundActivityHandler<R, C, W>, R extends RecordForBackgroundActivity<R, C, W>, C extends RecordForBackgroundActivityChunk<R, C, W>, W extends RecordForWorker<W>> TypedRecordIdentityList<R> findHandlers(SessionHolder sessionHolder,
                                                                                                                                                                                                                                               boolean onlyDone,
                                                                                                                                                                                                                                               boolean onlyNotDone,
                                                                                                                                                                                                                                               Class<R> clzRecord,
                                                                                                                                                                                                                                               Class<H> clzHandler,
                                                                                                                                                                                                                                               RecordLocator<?> handlerContext)
    {
        return QueryHelperWithCommonFields.list(sessionHolder.createHelper(clzRecord), (qh) ->
        {
            qh.addWhereClauseWithEqual(qh.root.get(RecordForBackgroundActivity_.handler), clzHandler.getName());

            if (handlerContext != null)
            {
                qh.addWhereClauseWithEqual(qh.root.get(RecordForBackgroundActivity_.handlerKey), getContextAsKey(handlerContext));
            }

            if (onlyDone)
            {
                addRawFilter(qh, DoneSet);
            }
            else if (onlyNotDone)
            {
                addRawFilter(qh, NotDoneSet);
            }
        });
    }

    private static <H extends BackgroundActivityHandler<R, C, W>, R extends RecordForBackgroundActivity<R, C, W>, C extends RecordForBackgroundActivityChunk<R, C, W>, W extends RecordForWorker<W>> void addRawFilter(QueryHelperWithCommonFields<Tuple, R> qh,
                                                                                                                                                                                                                       Collection<BackgroundActivityStatus> set)
    {
        Predicate[] preds = new Predicate[set.size()];
        int         pos   = 0;
        for (BackgroundActivityStatus status : set)
        {
            preds[pos++] = qh.equal(qh.root, RecordForBackgroundActivity_.status, status);
        }

        qh.addWhereClause(qh.or(preds));
    }

    private static String getContextAsKey(RecordLocator<?> ctx)
    {
        if (ctx == null)
        {
            return null;
        }

        return RecordHelper.registerEntityTable(ctx.getEntityClass()) + "/" + ctx.getIdRaw();
    }

    public static <R extends RecordForBackgroundActivity<R, ?, ?>> StreamHelperResult streamNoNesting(RecordHelper<R> helper,
                                                                                                      BackgroundActivityFilterRequest filters,
                                                                                                      Consumer<QueryHelperWithCommonFields<R, R>> callbackFilter,
                                                                                                      FunctionWithException<R, StreamHelperNextAction> callbackResults) throws
                                                                                                                                                                        Exception
    {
        QueryHelperWithCommonFields<R, R> jh = QueryHelperWithCommonFields.prepareQueryForEntity(helper);

        refineQuery(jh, filters, callbackFilter);

        return QueryHelperWithCommonFields.streamNoNesting(0, jh, callbackResults);
    }

    private static <T, R extends RecordForBackgroundActivity<R, ?, ?>> void refineQuery(QueryHelperWithCommonFields<T, R> jh,
                                                                                        BackgroundActivityFilterRequest filters,
                                                                                        Consumer<QueryHelperWithCommonFields<T, R>> callback)
    {
        if (filters != null)
        {
            if (StringUtils.isNotEmpty(filters.likeFilter))
            {
                String likeEscaped = "%" + StringUtils.replace(filters.likeFilter, "%", "\\%") + "%";

                Predicate pred = jh.isLike(jh.root, RecordForBackgroundActivity_.title, likeEscaped);
                jh.addWhereClause(pred);
            }

            if (filters.onlyReadyToGo)
            {
                jh.addWhereClause(jh.lessThanOrEqualTo(jh.root, RecordForBackgroundActivity_.nextActivation, TimeUtils.now()));
            }

            if (filters.statusFilter != null && filters.statusFilter.filter != null)
            {
                Collection<BackgroundActivityStatus> statusFilter = null;

                switch (filters.statusFilter.filter)
                {
                    case hideCompleted:
                        statusFilter = ActiveSet;
                        break;

                    case running:
                        statusFilter = NotDoneSet;
                        break;

                    case completed:
                        statusFilter = DoneSet;
                        break;

                    case matchingStatus:
                        statusFilter = filters.statusFilter.targets;
                        break;
                }

                if (statusFilter != null && !statusFilter.isEmpty())
                {
                    Predicate[] preds = new Predicate[statusFilter.size()];
                    int         pos   = 0;
                    for (BackgroundActivityStatus status : statusFilter)
                    {
                        preds[pos++] = jh.equal(jh.root, RecordForBackgroundActivity_.status, status);
                    }

                    jh.addWhereClause(jh.or(preds));
                }
            }

            if (filters.sortBy != null)
            {
                for (SortCriteria sortCriteria : filters.sortBy)
                {
                    switch (sortCriteria.column)
                    {
                        case "createdOn":
                            jh.addOrderBy(jh.root, RecordForBackgroundActivity_.createdOn, sortCriteria.ascending);
                            break;

                        case "nextActivation":
                            jh.addOrderBy(jh.root, RecordForBackgroundActivity_.nextActivation, sortCriteria.ascending);
                            break;

                        case "lastActivation":
                            jh.addOrderBy(jh.root, RecordForBackgroundActivity_.lastActivation, sortCriteria.ascending);
                            break;

                        case "title":
                            jh.addOrderBy(jh.root, RecordForBackgroundActivity_.title, sortCriteria.ascending);
                            break;

                        case "status":
                            jh.addOrderBy(jh.root, RecordForBackgroundActivity_.status, sortCriteria.ascending);
                            break;
                    }
                }
            }
        }

        if (callback != null)
        {
            callback.accept(jh);
        }
    }
}
