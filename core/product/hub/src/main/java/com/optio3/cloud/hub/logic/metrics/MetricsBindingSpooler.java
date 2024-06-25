/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.metrics;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.engine.EngineExecutionProgram;
import com.optio3.cloud.hub.engine.metrics.MetricsDefinitionDetails;
import com.optio3.cloud.hub.engine.metrics.MetricsEngineExecutionContext;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.model.asset.graph.AssetGraph;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphNode;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphRequest;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphResponse;
import com.optio3.cloud.hub.model.metrics.MetricsBinding;
import com.optio3.cloud.hub.model.metrics.MetricsBindingForSeries;
import com.optio3.cloud.hub.model.metrics.MetricsBindingForSetOfSeries;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.LogicalAssetRecord;
import com.optio3.cloud.hub.persistence.asset.MetricsDeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.cloud.hub.persistence.metrics.MetricsDefinitionRecord;
import com.optio3.cloud.hub.persistence.metrics.MetricsDefinitionVersionRecord;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.DatabaseActivity;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.search.HibernateSearch;
import com.optio3.collection.MapWithSoftValues;
import com.optio3.collection.Memoizer;
import com.optio3.concurrency.AsyncGate;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;
import com.optio3.logging.LoggerFactory;
import com.optio3.logging.LoggerResource;
import com.optio3.logging.Severity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.service.IServiceProvider;
import com.optio3.util.CollectionUtils;
import com.optio3.util.GcTracker;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class MetricsBindingSpooler
{
    public static class Gate extends AbstractApplicationWithDatabase.GateClass
    {
    }

    private static class MetricsEvaluation
    {
        final ZonedDateTime            start;
        final ZonedDateTime            end;
        final MetricsEngineValueSeries results;

        MonotonousTime expire;

        MetricsEvaluation(ZonedDateTime start,
                          ZonedDateTime end,
                          MetricsEngineValueSeries results)
        {
            this.start   = start;
            this.end     = end;
            this.results = results;

            setExpiration();
        }

        void setExpiration()
        {
            expire = TimeUtils.computeTimeoutExpiration(1, TimeUnit.HOURS);
        }
    }

    private static class MetricsEvaluations
    {
        private final List<MetricsEvaluation> entries = Lists.newArrayList();

        MetricsEngineValueSeries ensureInCache(ZonedDateTime rangeStart,
                                               ZonedDateTime rangeEnd,
                                               MetricsEngineValueSeries newValue)
        {
            for (Iterator<MetricsEvaluation> it = entries.iterator(); it.hasNext(); )
            {
                MetricsEvaluation metricsEvaluation = it.next();

                if (TimeUtils.isTimeoutExpired(metricsEvaluation.expire))
                {
                    it.remove();
                    continue;
                }

                if (TimeUtils.compare(metricsEvaluation.start, rangeStart) == 0 && TimeUtils.compare(metricsEvaluation.end, rangeEnd) == 0)
                {
                    metricsEvaluation.setExpiration();

                    // Lost race, return cached result.
                    return metricsEvaluation.results.copy();
                }
            }

            if (newValue != null)
            {
                entries.add(new MetricsEvaluation(rangeStart, rangeEnd, newValue.copy()));
            }

            return newValue;
        }
    }

    private static class MetricsMetadata
    {
        final String                                           sysId;
        final String                                           title;
        final EngineExecutionProgram<MetricsDefinitionDetails> program;
        final Map<String, MetricsBinding>                      lookupRecordToBindings = Maps.newHashMap();
        final Map<MetricsBinding, String>                      lookupBindingsToRecord = Maps.newHashMap();
        final Map<MetricsBinding, MonotonousTime>              lookupBindingsError    = Maps.newHashMap();

        int lastSyntheticCount;

        MetricsMetadata(String sysId,
                        String title,
                        EngineExecutionProgram<MetricsDefinitionDetails> program)
        {
            this.sysId   = sysId;
            this.title   = title;
            this.program = program;
        }

        static MetricsMetadata fetch(Memoizer memoizer,
                                     IServiceProvider serviceProvider,
                                     String sysId)
        {
            if (serviceProvider instanceof SessionHolder)
            {
                return fetch(memoizer, (SessionHolder) serviceProvider, sysId);
            }

            if (serviceProvider instanceof SessionProvider)
            {
                try (SessionHolder sessionHolder = ((SessionProvider) serviceProvider).newReadOnlySession())
                {
                    return fetch(memoizer, sessionHolder, sysId);
                }
            }

            try (SessionHolder sessionHolder = SessionHolder.createWithNewReadOnlySession(serviceProvider, null, Optio3DbRateLimiter.Normal))
            {
                return fetch(memoizer, sessionHolder, sysId);
            }
        }

        static MetricsMetadata fetch(Memoizer memoizer,
                                     SessionHolder sessionHolder,
                                     String sysId)
        {
            MetricsDefinitionRecord rec_metrics = sessionHolder.getEntityOrNull(MetricsDefinitionRecord.class, sysId);
            if (rec_metrics != null)
            {
                MetricsDefinitionVersionRecord rec_rel = rec_metrics.getReleaseVersion();
                if (rec_rel != null)
                {
                    String title = rec_metrics.getTitle();

                    try
                    {
                        MetricsMetadata state = new MetricsMetadata(memoizer.intern(sysId), title, rec_rel.prepareProgram(sessionHolder));

                        for (MetricsDeviceElementRecord rec_syntheticAsset : rec_metrics.getSyntheticAssets())
                        {
                            String         sysId_metrics = memoizer.intern(rec_syntheticAsset.getSysId());
                            MetricsBinding bindings      = rec_syntheticAsset.getBindings();

                            state.lookupBindingsToRecord.put(bindings, sysId_metrics);
                            state.lookupRecordToBindings.put(sysId_metrics, bindings);
                        }

                        return state;
                    }
                    catch (Throwable t)
                    {
                        LoggerInstance.error("Failed to load '%s', due to %s", title, t);
                    }
                }
            }

            return null;
        }

        void reportNoParent(MetricsBinding binding,
                            AssetRecord rec_parent)
        {
            if (rec_parent == null)
            {
                Severity level = Severity.Debug;

                MonotonousTime nextReport = lookupBindingsError.get(binding);
                if (TimeUtils.isTimeoutExpired(nextReport))
                {
                    nextReport = TimeUtils.computeTimeoutExpiration(1, TimeUnit.DAYS);
                    lookupBindingsError.put(binding, nextReport);

                    level = Severity.Error;
                }

                if (LoggerInstance.isEnabled(level))
                {
                    LoggerInstance.log(null, level, null, null, "Metrics '%s' (%s) can't infer parent for:", sysId, title);
                    try (LoggerResource indent = LoggerFactory.indent(">> "))
                    {
                        LoggerInstance.log(null, level, null, null, "%s", ObjectMappers.prettyPrintAsJson(binding));
                    }
                }
            }
            else
            {
                lookupBindingsError.remove(binding);
            }
        }

        public void reportResults(Stats stats)
        {
            Severity level = (lastSyntheticCount != stats.syntheticCount || stats.hasChanges()) ? Severity.Info : Severity.Debug;

            LoggerInstance.log(null,
                               level,
                               null,
                               null,
                               "Metrics '%s' (%s) synched with %,d synthetic records (%,d added, %,d updated, %,d removed)",
                               sysId,
                               title,
                               stats.syntheticCount,
                               stats.syntheticCountAdded,
                               stats.syntheticCountUpdated,
                               stats.syntheticCountRemoved);

            lastSyntheticCount = stats.syntheticCount;
        }
    }

    public static final Logger LoggerInstance = new Logger(MetricsBindingSpooler.class);

    private static final CompletableFuture<Void> s_done = AsyncRuntime.NullResult;

    public final AsyncGate gate = new AsyncGate(0, TimeUnit.SECONDS);

    private final HubApplication                                m_app;
    private final Memoizer                                      m_memoizer;
    private final Object                                        m_lock                                = new Object();
    private final AtomicBoolean                                 m_keepRunning                         = new AtomicBoolean(true);
    private final AtomicBoolean                                 m_rebuildNeeded                       = new AtomicBoolean(true);
    private final MapWithSoftValues<String, MetricsMetadata>    m_cachedMetadata                      = new MapWithSoftValues<>();
    private final MapWithSoftValues<String, MetricsEvaluations> m_cachedResults                       = new MapWithSoftValues<>();
    private       ArrayListMultimap<String, String>             m_lookupFromElementToDependentMetrics = ArrayListMultimap.create();

    private DatabaseActivity.LocalSubscriber         m_regDbActivity;
    private GcTracker.Holder                         m_gcNotifier;
    private int                                      m_pendingWorkerLazyDelay = 10;
    private ScheduledFuture<CompletableFuture<Void>> m_pendingWorkerLazy;
    private CompletableFuture<Void>                  m_pendingWorker;

    //--//

    public MetricsBindingSpooler(HubApplication app)
    {
        m_app      = app;
        m_memoizer = app.getServiceNonNull(Memoizer.class);

        app.registerService(MetricsBindingSpooler.class, () -> this);
    }

    public void initialize()
    {
        m_regDbActivity = DatabaseActivity.LocalSubscriber.create(m_app.getServiceNonNull(MessageBusBroker.class));

        m_regDbActivity.subscribeToTable(DeviceElementRecord.class, (dbEvent) ->
        {
            String sysId = dbEvent.context.sysId;
            switch (dbEvent.action)
            {
                case UPDATE_DIRECT:
                case UPDATE_INDIRECT:
                    synchronized (m_lock)
                    {
                        m_cachedResults.remove(sysId);

                        // Purge the results of all the metrics that used this element as a source.
                        for (String metricsSysId : m_lookupFromElementToDependentMetrics.get(sysId))
                        {
                            m_cachedResults.remove(metricsSysId);
                        }
                    }
                    break;

                case INSERT:
                    queueLazySynchronization();
                    break;

                case DELETE:
                    synchronized (m_lock)
                    {
                        m_cachedResults.remove(sysId);
                    }

                    queueLazySynchronization();
                    break;
            }
        });

        m_regDbActivity.subscribeToTable(LogicalAssetRecord.class, (dbEvent) ->
        {
            switch (dbEvent.action)
            {
                case INSERT:
                case DELETE:
                    queueLazySynchronization();
                    break;
            }
        });

        m_regDbActivity.subscribeToTable(MetricsDefinitionRecord.class, (dbEvent) ->
        {
            switch (dbEvent.action)
            {
                case INSERT:
                case DELETE:
                case UPDATE_DIRECT: // We don't invalidate for indirect updates.
                    synchronized (m_lock)
                    {
                        m_cachedMetadata.remove(dbEvent.context.sysId);
                    }

                    queueLazySynchronization();
                    break;
            }
        });

        m_gcNotifier = GcTracker.register((freeMemory, totalMemory, maxMemory) ->
                                          {
                                              float       heapUtilization = 100.0f * (totalMemory - freeMemory) / maxMemory;
                                              final float threshold       = 75.0f;
                                              final float purgeTarget     = 10.0f;

                                              if (heapUtilization > threshold)
                                              {
                                                  synchronized (m_lock)
                                                  {
                                                      class Age
                                                      {
                                                          MetricsEvaluations holder;
                                                          MetricsEvaluation  value;
                                                      }

                                                      List<Age> entries = Lists.newArrayList();

                                                      for (Map.Entry<String, MetricsEvaluations> entry : m_cachedResults.entrySet())
                                                      {
                                                          var holder = entry.getValue();

                                                          for (MetricsEvaluation value : holder.entries)
                                                          {
                                                              var age = new Age();
                                                              age.holder = holder;
                                                              age.value  = value;
                                                              entries.add(age);
                                                          }
                                                      }

                                                      entries.sort(Comparator.comparing(a -> a.value.expire));

                                                      int target = (int) (entries.size() * purgeTarget / 100.0f);
                                                      for (Age entryToPurge : entries)
                                                      {
                                                          entryToPurge.holder.entries.remove(entryToPurge.value);

                                                          if (--target <= 0)
                                                          {
                                                              break;
                                                          }
                                                      }
                                                  }
                                              }
                                          });

        // If a fixup runs at startup, make sure we reprocess.
        m_rebuildNeeded.set(true);

        startMetricsSynchronization();
    }

    public void close() throws
                        Exception
    {
        m_keepRunning.set(false);

        // Wait for worker to finish.
        synchronizeMetrics(true);

        if (m_regDbActivity != null)
        {
            m_regDbActivity.close();
            m_regDbActivity = null;
        }

        if (m_gcNotifier != null)
        {
            m_gcNotifier.close();
            m_gcNotifier = null;
        }
    }

    //--//

    public MetricsEngineValueSeries evaluate(SessionProvider sessionProvider,
                                             String sysId,
                                             ZonedDateTime rangeStart,
                                             ZonedDateTime rangeEnd)
    {
        MetricsEngineValueSeries series = ensureInCache(sysId, rangeStart, rangeEnd, null);
        if (series != null)
        {
            return series;
        }

        String sysId_metricsDefinition = MetricsBinding.extractMetricsDefinition(sessionProvider, sysId);
        if (sysId_metricsDefinition == null)
        {
            return null;
        }

        MetricsMetadata metadata = accessMetadata(sessionProvider, sysId_metricsDefinition);
        if (metadata != null)
        {
            MetricsBinding bindings = metadata.lookupRecordToBindings.get(sysId);
            if (bindings != null)
            {
                if (rangeEnd == null)
                {
                    rangeEnd = TimeUtils.now();
                }

                MetricsEngineExecutionContext ctx = new MetricsEngineExecutionContext(LoggerInstance, sessionProvider, metadata.program);
                ctx.rangeStart = rangeStart;
                ctx.rangeEnd   = rangeEnd;

                series = MetricsDeviceElementRecord.evaluate(rangeStart, rangeEnd, ctx, bindings, (stack, line) -> LoggerInstance.debug("Evaluation: %s", line));
                if (series != null)
                {
                    return ensureInCache(sysId, rangeStart, rangeEnd, series);
                }
            }
        }

        return null;
    }

    private MetricsEngineValueSeries ensureInCache(String sysId,
                                                   ZonedDateTime rangeStart,
                                                   ZonedDateTime rangeEnd,
                                                   MetricsEngineValueSeries newValue)
    {
        synchronized (m_lock)
        {
            MetricsEvaluations holder = m_cachedResults.get(sysId);
            if (holder == null)
            {
                holder = new MetricsEvaluations();
                m_cachedResults.put(m_memoizer.intern(sysId), holder);
            }

            return holder.ensureInCache(rangeStart, rangeEnd, newValue);
        }
    }

    //--//

    private void synchronizeMetrics(boolean wait)
    {
        try
        {
            CompletableFuture<Void> synchronization = startMetricsSynchronization();

            if (wait)
            {
                synchronization.get();
            }
        }
        catch (Exception e)
        {
            // Never going to happen...
        }
    }

    private void queueLazySynchronization()
    {
        synchronized (m_lock)
        {
            if (m_keepRunning.get())
            {
                m_rebuildNeeded.set(true);

                if (m_pendingWorkerLazy != null && m_pendingWorkerLazy.isDone())
                {
                    m_pendingWorkerLazy = null;
                }

                if (m_pendingWorkerLazy == null)
                {
                    LoggerInstance.info("Detected DB activity, scheduling analysis in %d seconds...", m_pendingWorkerLazyDelay);

                    m_pendingWorkerLazy = Executors.scheduleOnDefaultPool(this::startMetricsSynchronization, m_pendingWorkerLazyDelay, TimeUnit.SECONDS);
                }
            }
        }
    }

    private CompletableFuture<Void> startMetricsSynchronization()
    {
        synchronized (m_lock)
        {
            if (!m_keepRunning.get())
            {
                return s_done;
            }

            if (!m_rebuildNeeded.get())
            {
                return AsyncRuntime.NullResult;
            }

            if (m_pendingWorker == null)
            {
                m_pendingWorker = m_app.waitForAllGatesToOpenThenExecuteLongRunningTask(this::synchronizeMetrics, MetricsBindingSpooler.Gate.class);
            }

            return m_pendingWorker;
        }
    }

    private void synchronizeMetrics()
    {
        boolean shouldReschedule = false;

        try
        {
            if (m_rebuildNeeded.getAndSet(false))
            {
                LoggerInstance.info("Starting analysis...");

                ArrayListMultimap<String, String> lookupFromElementToDependentMetrics = ArrayListMultimap.create();
                boolean                           madeChanges                         = true;

                while (madeChanges)
                {
                    madeChanges = false;

                    try (AsyncGate.Holder searchGate = m_app.closeGate(HibernateSearch.Gate.class))
                    {
                        TagsEngine          tagsEngine   = m_app.getServiceNonNull(TagsEngine.class);
                        TagsEngine.Snapshot tagsSnapshot = tagsEngine.acquireSnapshot(true);
                        int                 metricsCount = 0;

                        lookupFromElementToDependentMetrics.clear();

                        try (SessionHolder sessionHolder = SessionHolder.createWithNewReadOnlySession(m_app, null, Optio3DbRateLimiter.System))
                        {
                            RecordHelper<MetricsDefinitionRecord> helper    = sessionHolder.createHelper(MetricsDefinitionRecord.class);
                            LinkedList<MetricsMetadata>           pending   = new LinkedList<>();
                            Set<String>                           processed = Sets.newHashSet();

                            for (MetricsDefinitionRecord rec_metrics : helper.listAll())
                            {
                                MetricsMetadata metadata = accessMetadata(sessionHolder, rec_metrics.getSysId());
                                if (metadata != null)
                                {
                                    pending.add(metadata);
                                }
                                else
                                {
                                    LoggerInstance.warn("Failed to load Metrics '%s' (%s)...", rec_metrics.getSysId(), rec_metrics.getTitle());
                                }
                            }

                            while (!pending.isEmpty())
                            {
                                boolean madeProgress = false;

                                loop:
                                for (Iterator<MetricsMetadata> it = pending.iterator(); it.hasNext(); )
                                {
                                    MetricsMetadata state = it.next();

                                    try
                                    {
                                        MetricsEngineExecutionContext ctx = new MetricsEngineExecutionContext(LoggerInstance, sessionHolder.getSessionProvider(), state.program);

                                        //
                                        // Compute a hash that represents the full definition of the metrics.
                                        //
                                        String detailsHash;

                                        {
                                            MetricsBinding.DetailsSummary detailsSummary = new MetricsBinding.DetailsSummary();
                                            detailsSummary.add(state.sysId, state.program);

                                            for (String dependency : ctx.extractDependencies())
                                            {
                                                MetricsMetadata dependencyMetadata = accessMetadata(sessionHolder, dependency);
                                                if (dependencyMetadata == null)
                                                {
                                                    LoggerInstance.warn("Metrics '%s' (%s) has a dependency on non-existing metrics '%s'...", state.sysId, state.title, dependency);
                                                    continue loop;
                                                }

                                                if (!processed.contains(dependency))
                                                {
                                                    continue loop;
                                                }

                                                detailsSummary.add(dependency, dependencyMetadata.program);
                                            }

                                            detailsHash = detailsSummary.extractHash();
                                        }

                                        processed.add(state.sysId);
                                        metricsCount++;

                                        Set<MetricsBinding> validBindings = Sets.newHashSet();

                                        AssetGraphRequest graphReq = new AssetGraphRequest();
                                        graphReq.graph = ctx.graphAnalyzed.graph;

                                        AssetGraphResponse tuples = graphReq.evaluate(tagsSnapshot);

                                        for (AssetGraphResponse.Resolved graphResponse : tuples.results)
                                        {
                                            MetricsBinding binding = new MetricsBinding();
                                            boolean        invalid = false;

                                            binding.detailsHash = detailsHash;
                                            binding.graphSource = graphResponse;

                                            for (String nodeId : ctx.seriesInGraph)
                                            {
                                                AssetGraphNode.Analyzed nodeAnalyzed = ctx.graphAnalyzed.lookupNode(nodeId);
                                                if (nodeAnalyzed == null)
                                                {
                                                    invalid = true;
                                                }
                                                else
                                                {
                                                    String[] records = graphResponse.tuple[nodeAnalyzed.index];

                                                    switch (records.length)
                                                    {
                                                        case 0:
                                                            if (!nodeAnalyzed.node.optional)
                                                            {
                                                                invalid = true;
                                                            }
                                                            break;

                                                        case 1:
                                                        {
                                                            String recordSysId = records[0];

                                                            MetricsBindingForSeries bindValue = new MetricsBindingForSeries();
                                                            bindValue.record = RecordIdentity.newTypedInstance(DeviceElementRecord.class, recordSysId);

                                                            binding.bindingForSeries.put(m_memoizer.intern(nodeId), bindValue);
                                                            break;
                                                        }

                                                        default:
                                                            // Not unique, skip.
                                                            invalid = true;
                                                            break;
                                                    }
                                                }
                                            }

                                            for (String nodeId : ctx.setOfSeriesInGraph)
                                            {
                                                AssetGraphNode.Analyzed nodeAnalyzed = ctx.graphAnalyzed.lookupNode(nodeId);
                                                if (nodeAnalyzed == null)
                                                {
                                                    invalid = true;
                                                }
                                                else
                                                {
                                                    String[] records = graphResponse.tuple[nodeAnalyzed.index];

                                                    switch (records.length)
                                                    {
                                                        case 0:
                                                            if (!nodeAnalyzed.node.optional)
                                                            {
                                                                invalid = true;
                                                            }
                                                            break;

                                                        default:
                                                        {
                                                            MetricsBindingForSetOfSeries bindValue = new MetricsBindingForSetOfSeries();
                                                            bindValue.records = new TypedRecordIdentityList<>();

                                                            for (String recordSysId : records)
                                                            {
                                                                bindValue.records.add(RecordIdentity.newTypedInstance(DeviceElementRecord.class, recordSysId));
                                                            }

                                                            binding.bindingForSetOfSeries.put(m_memoizer.intern(nodeId), bindValue);
                                                            break;
                                                        }
                                                    }
                                                }
                                            }

                                            if (!invalid)
                                            {
                                                if (ctx.outputForNamedSeries.isEmpty())
                                                {
                                                    validBindings.add(binding);
                                                }
                                                else
                                                {
                                                    for (String namedOutput : ctx.outputForNamedSeries.keySet())
                                                    {
                                                        MetricsBinding bindingNamed = binding.copy();
                                                        bindingNamed.namedOutput = namedOutput;
                                                        validBindings.add(bindingNamed);
                                                    }
                                                }
                                            }
                                        }

                                        Stats stats = new Stats();

                                        for (MetricsBinding binding : validBindings)
                                        {
                                            processValidBinding(ctx, state, stats, binding);
                                        }

                                        // Copy due to potential mutation in the loop.
                                        Set<MetricsBinding> existingBindings = Sets.newHashSet(state.lookupBindingsToRecord.keySet());
                                        for (MetricsBinding binding : existingBindings)
                                        {
                                            processExistingBinding(sessionHolder, stats, state, validBindings, binding);
                                        }

                                        madeChanges |= stats.madeChanges;

                                        state.reportResults(stats);
                                    }
                                    catch (Throwable t)
                                    {
                                        LoggerInstance.error("Failed to update Metrics '%s' (%s), due to %s", state.sysId, state.title, t);
                                    }

                                    for (MetricsBinding binding : state.lookupBindingsToRecord.keySet())
                                    {
                                        String sysId_dependentMetrics = state.lookupBindingsToRecord.get(binding);

                                        for (String sourceElementSysId : binding.collectSysIds())
                                        {
                                            lookupFromElementToDependentMetrics.put(m_memoizer.intern(sourceElementSysId), sysId_dependentMetrics);
                                        }
                                    }

                                    it.remove();
                                    madeProgress = true;
                                }

                                if (!madeProgress)
                                {
                                    LoggerInstance.error("Found loop between Metrics definitions: %s", pending);
                                    break;
                                }
                            }
                        }

                        LoggerInstance.info("Processed %,d metrics", metricsCount);
                    }
                }

                LoggerInstance.info("Completed analysis");

                synchronized (m_lock)
                {
                    if (m_rebuildNeeded.get())
                    {
                        m_pendingWorkerLazyDelay = Math.min(10 * 60, Math.min(60, m_pendingWorkerLazyDelay * 2));
                        shouldReschedule         = true;
                    }
                    else
                    {
                        m_pendingWorkerLazyDelay              = 10;
                        m_lookupFromElementToDependentMetrics = lookupFromElementToDependentMetrics;
                    }
                }
            }
        }
        catch (Exception e)
        {
            LoggerInstance.error("Failed to update Metrics state, due to %s", e);
            shouldReschedule = true;
        }
        finally
        {
            synchronized (m_lock)
            {
                m_pendingWorker.complete(null);
                m_pendingWorker = null;
            }
        }

        if (shouldReschedule)
        {
            queueLazySynchronization();
        }
    }

    //--//

    private static class Stats
    {
        boolean madeChanges;
        int     syntheticCount;
        int     syntheticCountAdded;
        int     syntheticCountUpdated;
        int     syntheticCountRemoved;

        boolean hasChanges()
        {
            return syntheticCountAdded != 0 || syntheticCountUpdated != 0 || syntheticCountRemoved != 0;
        }
    }

    private void processValidBinding(MetricsEngineExecutionContext ctx,
                                     MetricsMetadata state,
                                     Stats stats,
                                     MetricsBinding binding)
    {
        try (SessionHolder subSessionHolder = ctx.sessionProvider.newSessionWithTransaction())
        {
            MetricsDeviceElementRecord rec = subSessionHolder.getEntityOrNull(MetricsDeviceElementRecord.class, state.lookupBindingsToRecord.get(binding));
            if (rec == null)
            {
                AssetRecord rec_parent = inferParent(subSessionHolder, binding, ctx);
                state.reportNoParent(binding, rec_parent);

                if (rec_parent != null)
                {
                    try
                    {
                        inferSchema(ctx, binding, state.title);

                        MetricsDefinitionRecord rec_metrics = subSessionHolder.getEntityOrNull(MetricsDefinitionRecord.class, state.sysId);
                        String                  newSysId    = MetricsDeviceElementRecord.newInstance(subSessionHolder, rec_parent, rec_metrics, state.title, binding);
                        if (newSysId != null)
                        {
                            newSysId = m_memoizer.intern(newSysId);

                            state.lookupBindingsToRecord.put(binding, newSysId);
                            state.lookupRecordToBindings.put(newSysId, binding);

                            stats.madeChanges = true;
                            stats.syntheticCountAdded++;
                        }

                        if (LoggerInstance.isEnabled(Severity.Debug))
                        {
                            LoggerInstance.debug("Added %s:\n%s", binding.generateId(rec_parent, rec_metrics), ObjectMappers.prettyPrintAsJson(binding));
                        }

                        stats.syntheticCount++;
                    }
                    catch (Throwable t)
                    {
                        LoggerInstance.error("Failed to create Synthetic record for Metrics '%s' (%s) under parent '%s', due to %s", state.sysId, state.title, rec_parent.getSysId(), t);
                    }
                }
            }
            else
            {
                MetricsBinding bindingOld = rec.getBindings();

                stats.syntheticCount++;
                stats.madeChanges |= rec.setPhysicalName(binding.generateTitle(state.title));

                if (!StringUtils.equals(bindingOld.detailsHash, binding.detailsHash))
                {
                    try
                    {
                        inferSchema(ctx, binding, state.title);

                        if (LoggerInstance.isEnabled(Severity.Debug))
                        {
                            LoggerInstance.debug("Update %s:\n%s", rec.getSysId(), ObjectMappers.prettyPrintAsJson(binding));
                        }

                        rec.updateBindings(binding);
                        stats.syntheticCountUpdated++;
                        stats.madeChanges = true;
                    }
                    catch (Throwable t)
                    {
                        LoggerInstance.error("Failed to update schema on Synthetic record '%s' for Metrics '%s' (%s), due to %s", rec.getSysId(), state.sysId, state.title, t);
                    }
                }
            }

            subSessionHolder.commit();
        }
    }

    private void processExistingBinding(SessionHolder sessionHolder,
                                        Stats stats,
                                        MetricsMetadata state,
                                        Set<MetricsBinding> validBindings,
                                        MetricsBinding binding)
    {
        if (!validBindings.contains(binding))
        {
            String sysId = state.lookupBindingsToRecord.remove(binding);
            if (sysId != null)
            {
                state.lookupRecordToBindings.remove(sysId);

                try (SessionHolder subSessionHolder = sessionHolder.spawnNewSessionWithTransaction())
                {
                    MetricsDeviceElementRecord rec_syntheticAsset = subSessionHolder.getEntityOrNull(MetricsDeviceElementRecord.class, sysId);
                    if (rec_syntheticAsset != null)
                    {
                        subSessionHolder.deleteEntity(rec_syntheticAsset);

                        if (LoggerInstance.isEnabled(Severity.Debug))
                        {
                            LoggerInstance.debug("Removed %s:\n%s", sysId, ObjectMappers.prettyPrintAsJson(binding));
                        }

                        stats.syntheticCountRemoved++;
                        stats.madeChanges = true;

                        subSessionHolder.commit();
                    }
                }
            }
        }
    }

    //--//

    private static AssetRecord inferParent(SessionHolder sessionHolder,
                                           MetricsBinding binding,
                                           MetricsEngineExecutionContext ctx)
    {
        Set<String> nodeIds = Sets.newHashSet();

        nodeIds.addAll(extractParents(ctx.graphAnalyzed, binding.bindingForSeries));
        nodeIds.addAll(extractParents(ctx.graphAnalyzed, binding.bindingForSetOfSeries));

        String parentNodeId = reduceToCommonParent(nodeIds, ctx.graphAnalyzed);
        if (parentNodeId != null)
        {
            AssetGraphNode.Analyzed nodeAnalyzed = binding.graphSource.graph.lookupNode(parentNodeId);
            for (String sysId : binding.graphSource.tuple[nodeAnalyzed.index])
            {
                AssetRecord result = sessionHolder.getEntityOrNull(AssetRecord.class, sysId);
                if (result != null)
                {
                    if (SessionHolder.isEntityOfClass(result, DeviceElementRecord.class))
                    {
                        return result.getParentAsset();
                    }

                    return result;
                }
            }
        }

        try (SessionHolder subSessionHolder = sessionHolder.spawnNewReadOnlySession())
        {
            AssetRecord rec_firstParent = null;

            for (String nodeId : nodeIds)
            {
                AssetGraphNode.Analyzed nodeAnalyzed = binding.graphSource.graph.lookupNode(nodeId);
                for (String sysId : binding.graphSource.tuple[nodeAnalyzed.index])
                {
                    AssetRecord rec = subSessionHolder.getEntityOrNull(AssetRecord.class, sysId);
                    if (rec != null)
                    {
                        AssetRecord rec_parent = rec.getParentAsset();
                        if (rec_parent != null)
                        {
                            if (rec_firstParent != null && rec_firstParent != rec_parent)
                            {
                                // More than one parent...
                                return null;
                            }

                            rec_firstParent = rec_parent;
                        }
                    }
                }
            }

            return rec_firstParent;
        }
    }

    private static Collection<String> extractParents(AssetGraph.Analyzed graph,
                                                     Map<String, ?> map)
    {
        Set<String> parentIds = Sets.newHashSet();

        for (String nodeId : map.keySet())
        {
            AssetGraphNode.Analyzed node = graph.lookupNode(nodeId);
            if (node != null)
            {
                if (node.parents.isEmpty())
                {
                    parentIds.add(nodeId);
                }
                else
                {
                    for (AssetGraphNode.Analyzed parent : node.parents)
                    {
                        parentIds.add(parent.node.id);
                    }
                }
            }
        }

        return parentIds;
    }

    private static String reduceToCommonParent(Set<String> nodeIds,
                                               AssetGraph.Analyzed graph)
    {
        if (nodeIds.size() == 1)
        {
            return CollectionUtils.firstElement(nodeIds);
        }

        for (String nodeId : nodeIds)
        {
            AssetGraphNode.Analyzed node = graph.lookupNode(nodeId);
            if (node == null)
            {
                // Wrong id?
                return null;
            }

            switch (node.parents.size())
            {
                case 0:
                    break;

                case 1:
                    AssetGraphNode.Analyzed parentNode = node.parents.get(0);
                    Set<String> nodeIds2 = Sets.newHashSet(nodeIds);
                    nodeIds2.remove(nodeId);
                    nodeIds2.add(parentNode.node.id);

                    String commonParentId = reduceToCommonParent(nodeIds2, graph);
                    if (commonParentId != null)
                    {
                        return commonParentId;
                    }
                    break;

                default:
                    // Multiple parents, no common parent.
                    return null;
            }
        }

        return null;
    }

    //--//

    private static void inferSchema(MetricsEngineExecutionContext ctx,
                                    MetricsBinding binding,
                                    String title)
    {
        ZonedDateTime rangeEnd   = TimeUtils.now();
        ZonedDateTime rangeStart = rangeEnd.minusDays(1);

        MetricsEngineValueSeries output = MetricsDeviceElementRecord.evaluate(rangeStart, rangeEnd, ctx, binding, null);
        if (output != null)
        {
            TimeSeriesPropertyType pt = output.values.propertySchema;
            pt.name        = DeviceElementRecord.DEFAULT_PROP_NAME;
            pt.displayName = title;

            if (output.values.enumLookup != null)
            {
                for (int index = 0; index < output.values.enumLookup.length; index++)
                {
                    var newEnumValue = pt.addEnumValue();
                    newEnumValue.value = index;
                    newEnumValue.name  = output.values.enumLookup[index];
                }

                pt.type         = TimeSeries.SampleType.Enumerated;
                pt.expectedType = String.class;
            }
            else
            {
                pt.type         = TimeSeries.SampleType.Decimal;
                pt.expectedType = double.class;
            }

            pt.targetField = pt.name;

            binding.schema = pt;
        }
    }

    //--//

    private MetricsMetadata accessMetadata(IServiceProvider serviceProvider,
                                           String sysId)
    {
        synchronized (m_lock)
        {
            MetricsMetadata metadata = m_cachedMetadata.get(sysId);
            if (metadata == null)
            {
                metadata = MetricsMetadata.fetch(m_memoizer, serviceProvider, sysId);
                if (metadata == null)
                {
                    return null;
                }

                m_cachedMetadata.put(metadata.sysId, metadata);
            }

            return metadata;
        }
    }
}
