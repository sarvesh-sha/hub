/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.orchestration.state;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wrapAsync;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.client.gateway.model.GatewayAutoDiscovery;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.client.gateway.model.GatewayEntity;
import com.optio3.cloud.client.gateway.model.GatewayNetwork;
import com.optio3.cloud.client.gateway.model.GatewayOperationToken;
import com.optio3.cloud.client.gateway.model.GatewayQueueStatus;
import com.optio3.cloud.client.gateway.proxy.GatewayStatusApi;
import com.optio3.concurrency.DebouncedAction;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.config.ProtocolConfig;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;
import com.optio3.util.ConfigurationPersistenceHelper;
import com.optio3.util.IdGenerator;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.ConsumerWithException;
import org.apache.commons.lang3.StringUtils;

public abstract class GatewayState
{
    public static class ProgressStatus
    {
        public int countDevicesToProcess;
        public int countDevicesProcessed;
        public int countDevicesUnreachable;
        public int countObjects;
        public int countProperties;
        public int countPropertiesBad;
        public int countUnknowns;
        public int countDeadlines;
        public int countTimeouts;

        public boolean hasAnyFailure()
        {
            return countPropertiesBad > 0;
        }

        public void accumulate(ProgressStatus stats)
        {
            countDevicesToProcess += stats.countDevicesToProcess;
            countDevicesProcessed += stats.countDevicesProcessed;
            countDevicesUnreachable += stats.countDevicesUnreachable;
            countObjects += stats.countObjects;
            countProperties += stats.countProperties;
            countPropertiesBad += stats.countPropertiesBad;
            countUnknowns += stats.countUnknowns;
            countDeadlines += stats.countDeadlines;
            countTimeouts += stats.countTimeouts;
        }
    }

    //--//

    public static class ForReport
    {
    }

    public static final Logger LoggerInstance          = new Logger(GatewayState.class);
    public static final Logger LoggerInstanceForReport = LoggerInstance.createSubLogger(ForReport.class);

    //--//

    private static final String  c_gateway_config       = "gateway_config";
    private static final String  c_gateway_state        = "gateway_state-";
    private static final String  c_gateway_batch_prefix = "gateway_batch_";
    private static final String  c_gateway_batch_suffix = "bin";
    private static final Pattern c_gateway_batch_regex  = Pattern.compile(c_gateway_batch_prefix + "([0-9]+)\\." + c_gateway_batch_suffix);

    public static class PersistedProtocolConfiguration
    {
        public ProtocolConfig target;
        public String         stateFileId;
    }

    public static class PersistedNetworkConfiguration
    {
        public GatewayNetwork                       network;
        public List<PersistedProtocolConfiguration> protocols = Lists.newArrayList();

        public PersistedProtocolConfiguration access(ProtocolConfig pc)
        {
            for (PersistedProtocolConfiguration protocolCfg : protocols)
            {
                if (protocolCfg.target.getClass() == pc.getClass())
                {
                    return protocolCfg;
                }
            }

            PersistedProtocolConfiguration protocolCfg = new PersistedProtocolConfiguration();
            protocolCfg.target = ObjectMappers.cloneThroughJson(ObjectMappers.SkipNulls, pc);
            protocols.add(protocolCfg);
            return protocolCfg;
        }
    }

    public static class PersistedGatewayConfiguration
    {
        public List<PersistedNetworkConfiguration> networks = Lists.newArrayList();

        public PersistedNetworkConfiguration access(GatewayNetwork gn)
        {
            for (PersistedNetworkConfiguration networkCfg : networks)
            {
                if (StringUtils.equals(networkCfg.network.sysId, gn.sysId))
                {
                    return networkCfg;
                }
            }

            PersistedNetworkConfiguration networkCfg = new PersistedNetworkConfiguration();
            networkCfg.network = ObjectMappers.cloneThroughJson(ObjectMappers.SkipNulls, gn);
            networkCfg.network.protocolsConfiguration.clear(); // Clear the protocol configuration, we manage it when we reload.
            networks.add(networkCfg);
            return networkCfg;
        }
    }

    //--//

    public abstract class ResultHolder extends GatewayEntity
    {
        public abstract double getTimestamp();

        public void queueContents(String contents)
        {
            synchronized (m_flusher)
            {
                PendingTree node = createTargetNode();

                if (LoggerInstance.isEnabled(Severity.DebugObnoxious))
                {
                    LoggerInstance.debugObnoxious("%,f : %s : %d : %s", getTimestampEpoch(), node.m_hasDataToSend, m_flusher.m_numberOfUnbatchedEntries, contents);
                }

                if (!node.m_hasDataToSend)
                {
                    node.m_hasDataToSend = true;

                    m_flusher.m_numberOfUnbatchedEntries++;

                    //
                    // As we approach the batch threshold, make sure the flusher is running.
                    // At first it won't do anything, but it will keep monitoring the pending level.
                    //
                    if (m_flusher.m_numberOfUnbatchedEntries > getEffectiveMaxNodes() / 2)
                    {
                        m_flusher.startFlushingOfEntities(false);
                    }
                }

                node.m_context.contents = contents;
            }
        }

        protected abstract PendingTree createTargetNode();

        //--//

        public ResultHolder prepareResult(GatewayDiscoveryEntitySelector selectorKey,
                                          BaseAssetDescriptor selectorValue,
                                          boolean addTimestamp) throws
                                                                JsonProcessingException
        {
            return prepareResult(selectorKey, ObjectMappers.SkipNulls.writeValueAsString(selectorValue), addTimestamp);
        }

        public ResultHolder prepareResult(GatewayDiscoveryEntitySelector selectorKey,
                                          String selectorValue,
                                          boolean addTimestamp)
        {
            return new ResultHolderNode(this, selectorKey, selectorValue, addTimestamp);
        }
    }

    private class ResultHolderNode extends ResultHolder
    {
        private final ResultHolder m_parent;

        ResultHolderNode(ResultHolder parent,
                         GatewayDiscoveryEntitySelector selectorKey,
                         String selectorValue,
                         boolean addTimestamp)
        {
            requireNonNull(parent);

            m_parent = parent;

            this.selectorKey   = selectorKey;
            this.selectorValue = selectorValue;

            if (addTimestamp)
            {
                this.setTimestampEpoch(getTimestamp());
            }
        }

        @Override
        public double getTimestamp()
        {
            return m_parent.getTimestamp();
        }

        @Override
        protected PendingTree createTargetNode()
        {
            PendingTree parentNode = m_parent.createTargetNode();

            return parentNode.add(this);
        }

        GatewayDiscoveryEntity flushToBatch(PendingTreeBatch batch)
        {
            int estimatedSize = estimateSize();

            GatewayDiscoveryEntity result = new GatewayDiscoveryEntity();
            result.selectorKey           = selectorKey;
            result.selectorValue         = selectorValue;
            result.timestampEpochSeconds = timestampEpochSeconds;
            result.timestampEpochMillis  = timestampEpochMillis;
            result.contents              = contents;

            contents = null;

            batch.addNodeToBatch(estimatedSize);
            return result;
        }
    }

    private class ResultHolderRoot extends ResultHolder
    {
        private final double m_resultEpochSeconds;

        ResultHolderRoot(double resultEpochSeconds)
        {
            m_resultEpochSeconds = resultEpochSeconds;
        }

        @Override
        public double getTimestamp()
        {
            return m_resultEpochSeconds;
        }

        @Override
        protected PendingTree createTargetNode()
        {
            return m_root;
        }
    }

    public static class PendingTree
    {
        private final ResultHolderNode                       m_context;
        private       boolean                                m_hasDataToSend;
        private       TreeMap<ResultHolderNode, PendingTree> m_subTrees;

        private PendingTree(ResultHolderNode context)
        {
            m_context = context;
        }

        //--//

        private PendingTree add(ResultHolderNode key)
        {
            if (m_subTrees == null)
            {
                m_subTrees = Maps.newTreeMap();
            }

            //
            // Make sure there's a node for the target.
            //
            PendingTree subTree = m_subTrees.get(key);
            if (subTree == null)
            {
                subTree = new PendingTree(key);
                m_subTrees.put(key, subTree);
            }

            return subTree;
        }

        /**
         * This method walks through the pending tree, looking for entities that have to be sent to the Hub.
         * As it walks, it creates a tree of objects to send.
         *
         * @return Non-null if any entity has been marked 'ready to send'
         */
        private GatewayDiscoveryEntity createBatch(PendingTreeBatch batch)
        {
            //
            // See if this node or any of its children needs to be processed.
            //
            GatewayDiscoveryEntity result = null;

            if (m_hasDataToSend && batch.canAddToBatch())
            {
                result = m_context.flushToBatch(batch);

                m_hasDataToSend = false;
                batch.m_owner.m_flusher.m_numberOfUnbatchedEntries--;
            }

            if (m_subTrees != null)
            {
                Iterator<PendingTree> it = m_subTrees.values()
                                                     .iterator();

                while (batch.canAddToBatch() && it.hasNext())
                {
                    PendingTree subTree = it.next();

                    GatewayDiscoveryEntity subResult = subTree.createBatch(batch);
                    if (subResult != null)
                    {
                        if (result == null)
                        {
                            result = new GatewayDiscoveryEntity();
                            if (m_context != null)
                            {
                                result.selectorKey   = m_context.selectorKey;
                                result.selectorValue = m_context.selectorValue;
                            }
                        }

                        if (result.subEntities == null)
                        {
                            result.subEntities = Lists.newArrayList();
                        }

                        result.subEntities.add(subResult);
                    }

                    if (subTree.canRemove())
                    {
                        it.remove();
                    }
                }

                if (m_subTrees.isEmpty())
                {
                    m_subTrees = null;
                }
            }

            return result;
        }

        private double findOldestNode()
        {
            double oldest = Double.POSITIVE_INFINITY;

            if (m_hasDataToSend)
            {
                oldest = Math.min(oldest, m_context.getTimestamp());
            }

            if (m_subTrees != null)
            {
                for (PendingTree subTree : m_subTrees.values())
                {
                    oldest = Math.min(oldest, subTree.findOldestNode());
                }
            }

            return oldest;
        }

        private boolean canRemove()
        {
            return !m_hasDataToSend && m_subTrees == null;
        }
    }

    private static class PendingTreeBatch
    {
        @JsonIgnore
        private GatewayState m_owner;

        @JsonIgnore
        private boolean m_flushedToDisk;

        public ZonedDateTime                creationTime;
        public int                          sequenceNumber;
        public int                          numberOfNodes;
        public int                          estimatedSize;
        public List<GatewayDiscoveryEntity> entities;

        //--//

        private boolean canAddToBatch()
        {
            return numberOfNodes < m_owner.getEffectiveMaxNodes() && estimatedSize < m_owner.m_maxBatchSize;
        }

        private void addNodeToBatch(int estimatedSize)
        {
            this.numberOfNodes++;
            this.estimatedSize += estimatedSize;
        }

        private static PendingTreeBatch loadFromDisk(GatewayState owner,
                                                     int sequenceNumber) throws
                                                                         Exception
        {
            return owner.m_helper.loadFromFile(owner.getBatchFile(sequenceNumber), (stream) ->
            {
                try (InflaterInputStream decompressor = new InflaterInputStream(stream))
                {
                    PendingTreeBatch batch = ObjectMappers.SkipNulls.readValue(decompressor, PendingTreeBatch.class);
                    batch.m_owner         = owner;
                    batch.m_flushedToDisk = true;
                    return batch;
                }
            });
        }

        private void saveToDisk(GatewayState owner) throws
                                                    Exception
        {
            if (!m_flushedToDisk)
            {
                owner.m_helper.saveToFile(owner.getBatchFile(sequenceNumber), (stream) ->
                {
                    try (DeflaterOutputStream compressor = new DeflaterOutputStream(stream))
                    {
                        ObjectMappers.SkipNulls.writeValue(compressor, this);
                    }
                });

                m_flushedToDisk = true;
            }
        }

        private void saveToStream(OutputStream stream) throws
                                                       IOException
        {
            try (DeflaterOutputStream compressor = new DeflaterOutputStream(stream))
            {
                ObjectMappers.SkipNulls.writeValue(compressor, this);
            }
        }

        private void deleteFromDisk()
        {
            if (m_flushedToDisk)
            {
                try
                {
                    File file = m_owner.getBatchFile(sequenceNumber);
                    if (file != null)
                    {
                        file.delete();
                    }
                }
                catch (Throwable t)
                {
                    LoggerInstance.error("Failed to delete batch '%s', due to %s", sequenceNumber, t);
                }

                m_flushedToDisk = false;
            }
        }

        private void reduceMemoryFootprint() throws
                                             Exception
        {
            saveToDisk(m_owner);
            entities = null;
        }

        private boolean isInMemory()
        {
            return entities != null;
        }

        private boolean ensureInMemory()
        {
            if (!isInMemory())
            {
                try
                {
                    PendingTreeBatch batchState = loadFromDisk(m_owner, sequenceNumber);
                    entities = batchState.entities;
                }
                catch (Throwable t)
                {
                    LoggerInstance.error("Failed to load batch '%s', due to %s", sequenceNumber, t);
                }
            }

            return isInMemory();
        }
    }

    private class FlushReport
    {
        private final int c_initialWait     = 40 * 60;
        private final int c_incrementalWait = 10 * 60;

        private final Stopwatch      m_timer             = Stopwatch.createStarted();
        private       int            m_flushedCounter;
        private       boolean        m_reportFinalProgress;
        private       int            m_waitForNextReport = c_initialWait;
        private       MonotonousTime m_nextReport;

        FlushReport()
        {
            computeNextReport();
        }

        private void computeNextReport()
        {
            m_nextReport        = TimeUtils.computeTimeoutExpiration(m_waitForNextReport, TimeUnit.SECONDS);
            m_waitForNextReport = Math.min((2 * 60) * 60, m_waitForNextReport + c_incrementalWait);
        }

        private void updateCounter(int nodes)
        {
            m_flushedCounter += nodes;

            if (getNumberOfPendingEntities() == 0)
            {
                LoggerInstance.debugVerbose("Flushed %d object(s), going to idle.", nodes);
            }
            else
            {
                LoggerInstance.debugVerbose("Flushed %d object(s), still %d batched, %d unbatched...", nodes, getNumberOfBatchedEntities(), getNumberOfUnbatchedEntities());
            }
        }

        private void emitReport()
        {
            if (TimeUtils.isTimeoutExpired(m_nextReport))
            {
                m_reportFinalProgress = true;

                long runDuration = m_timer.elapsed(TimeUnit.SECONDS);
                LoggerInstance.info("[%d secs] %d entities to flush back to Hub...", runDuration, getNumberOfPendingEntities());

                computeNextReport();
            }
        }

        private void emitLastReport()
        {
            if (m_reportFinalProgress)
            {
                long runDuration = m_timer.elapsed(TimeUnit.SECONDS);
                LoggerInstance.info("Flushed %d entities back to Hub in %d seconds", m_flushedCounter, runDuration);
            }
        }
    }

    private class PendingTreeFlusher
    {
        private final TreeMap<Integer, PendingTreeBatch> m_persistedBatches = new TreeMap<>();
        private       long                               m_numberOfUploadedEntities;
        private       long                               m_numberOfUploadedEntitiesRetries;
        private       int                                m_numberOfUnbatchedEntries;
        private       int                                m_numberOfNodesBatched;
        private       int                                m_nextSequenceNumber;

        private final DebouncedAction<Void> m_pendingFlush = new DebouncedAction<Void>(this::flushWorkerForEntitiesWorker);
        private       boolean               m_mustDrain;
        private       boolean               m_offline;
        private       MonotonousTime        m_nextBatch;
        private       MonotonousTime        m_reportError;
        private       FlushReport           m_flushReport;

        void shutdown()
        {
            synchronized (m_flusher)
            {
                while (extractBatchNow())
                {
                    flushBatches(true);
                }

                flushBatches(true);
            }
        }

        GatewayQueueStatus checkQueueStatus()
        {
            synchronized (m_flusher)
            {
                var res = new GatewayQueueStatus();
                res.numberOfUnbatchedEntries = m_numberOfUnbatchedEntries;
                res.numberOfBatchedEntries   = m_numberOfNodesBatched;
                res.numberOfBatches          = m_persistedBatches.size();

                var batch = CollectionUtils.firstElement(m_persistedBatches.values());
                if (batch != null)
                {
                    res.oldestEntry = batch.creationTime;
                }
                else
                {
                    double oldest = m_root.findOldestNode();
                    res.oldestEntry = TimeUtils.fromTimestampToUtcTime(oldest);
                }

                return res;
            }
        }

        CompletableFuture<Void> waitForFlushingOfEntities() throws
                                                            Exception
        {

            CompletableFuture<Void> pendingFlush = startFlushingOfEntities(true);
            if (pendingFlush != null)
            {
                await(pendingFlush);
            }

            return wrapAsync(null);
        }

        CompletableFuture<Void> startFlushingOfEntities(boolean drain)
        {
            synchronized (m_flusher)
            {
                if (drain)
                {
                    m_mustDrain = true;
                }

                return m_pendingFlush.schedule(50, TimeUnit.MILLISECONDS);
            }
        }

        private CompletableFuture<Void> flushWorkerForEntitiesWorker() throws
                                                                       Exception
        {
            LoggerInstance.debugVerbose("flushWorkerForEntities - start");

            while (true)
            {
                LoggerInstance.debugVerbose("flushWorkerForEntities - loop - %d %d %d", getNumberOfUnbatchedEntities(), getNumberOfBatchedEntities(), m_flusher.m_persistedBatches.size());

                if (m_reportError != null)
                {
                    emitReport();

                    await(sleep(10, TimeUnit.SECONDS));
                }

                try
                {
                    emitReport();

                    boolean madeProgress = await(extractBatches());

                    madeProgress |= await(sendNextBatch());

                    synchronized (m_flusher)
                    {
                        //
                        // If no pending entities, we can exit.
                        //
                        if (!hasWorkToDo())
                        {
                            LoggerInstance.debugVerbose("flushWorkerForEntities - done");

                            computeNextBatchTime();
                            m_mustDrain = false;
                            break;
                        }
                    }

                    if (!madeProgress)
                    {
                        LoggerInstance.debugVerbose("flushWorkerForEntities - no progress");

                        await(sleep(5, TimeUnit.SECONDS));
                    }
                }
                catch (Throwable t)
                {
                    if (LoggerInstance.isEnabled(Severity.Debug))
                    {
                        LoggerInstance.debugVerbose("Flushing of entities failed due to exception: %s", t);
                    }
                    else
                    {
                        if (shouldReportError())
                        {
                            LoggerInstance.error("Flushing of entities failed due to exception: %s", t);
                        }
                    }
                }
            }

            emitLastReport();

            return wrapAsync(null);
        }

        private boolean hasWorkToDo()
        {
            return getNumberOfPendingEntities() > 0 || !m_persistedBatches.isEmpty();
        }

        //--//

        private boolean shouldReportError()
        {
            if (m_reportError == null)
            {
                m_reportError = TimeUtils.computeTimeoutExpiration(15, TimeUnit.MINUTES);
            }

            return TimeUtils.isTimeoutExpired(m_reportError);
        }

        private void emitReport()
        {
            if (!m_persistedBatches.isEmpty())
            {
                ensureFlushReport();
            }

            if (m_flushReport != null)
            {
                m_flushReport.emitReport();
            }
        }

        private void emitLastReport()
        {
            if (m_flushReport != null)
            {
                m_flushReport.emitLastReport();
                m_flushReport = null;
            }
        }

        private FlushReport ensureFlushReport()
        {
            if (m_flushReport == null)
            {
                m_flushReport = new FlushReport();
            }

            return m_flushReport;
        }

        //--//

        private boolean shouldCreateNewBatches()
        {
            synchronized (m_flusher)
            {
                if (m_mustDrain)
                {
                    // No point in draining when offline or there are already pending batches.
                    if (m_persistedBatches.isEmpty() && !m_offline)
                    {
                        LoggerInstance.debug("shouldCreateNewBatches: mustDrain offline=%s savedBatches=%d", m_offline, m_persistedBatches.size());
                        return true;
                    }
                }

                int effectiveMaxNodes = getEffectiveMaxNodes();
                if (m_numberOfUnbatchedEntries > effectiveMaxNodes)
                {
                    LoggerInstance.debug("shouldCreateNewBatches: m_numberOfUnbatchedEntries (%d) > m_maxNodesPerBatch (%d)", m_numberOfUnbatchedEntries, effectiveMaxNodes);
                    return true;
                }

                if (TimeUtils.isTimeoutExpired(m_nextBatch))
                {
                    LoggerInstance.debug("shouldCreateNewBatches: timeout %s at %s", m_nextBatch, MonotonousTime.now());
                    return true;
                }

                return false;
            }
        }

        private void computeNextBatchTime()
        {
            int delay;

            if (m_offline || m_persistedBatches.size() > 1)
            {
                // When offline or have many pending batches, we always create batches every ten minutes.
                delay = 10 * 60;
            }
            else
            {
                delay = m_batchPeriodInSeconds;
            }

            m_nextBatch = TimeUtils.computeTimeoutExpiration(delay, TimeUnit.SECONDS);
        }

        private void reloadBatches()
        {
            try
            {
                for (File file : m_helper.listFiles(null))
                {
                    Integer sequenceNumber = parseSequenceNumber(file.getName());
                    if (sequenceNumber == null)
                    {
                        continue;
                    }

                    try
                    {
                        PendingTreeBatch batch = PendingTreeBatch.loadFromDisk(GatewayState.this, sequenceNumber);
                        batch.reduceMemoryFootprint();
                        trackBatch(batch);
                    }
                    catch (Throwable t)
                    {
                        LoggerInstance.error("Failed to reload batch %d, due to %s", sequenceNumber, t);

                        try
                        {
                            file.delete();
                        }
                        catch (Throwable t2)
                        {
                            // Ignore failures.
                        }
                    }
                }
            }
            catch (Throwable t3)
            {
                LoggerInstance.error("Failed to reload batches, due to %s", t3);
            }

            if (hasWorkToDo())
            {
                startFlushingOfEntities(true);
            }
        }

        private CompletableFuture<Boolean> extractBatches() throws
                                                            Exception
        {
            boolean madeProgress = false;

            if (shouldCreateNewBatches())
            {
                // Sleep a bit to accumulate more entries.
                await(sleep(50, TimeUnit.MILLISECONDS));

                madeProgress = extractBatchNow();
            }

            flushBatches(false);

            return wrapAsync(madeProgress);
        }

        private boolean extractBatchNow()
        {
            boolean madeProgress = false;

            PendingTreeBatch newBatch = new PendingTreeBatch();
            newBatch.m_owner      = GatewayState.this;
            newBatch.creationTime = TimeUtils.now();

            GatewayDiscoveryEntity results;

            synchronized (m_flusher)
            {
                results = m_root.createBatch(newBatch);

                // If we don't have anything left in the tree, we are sure not to have any unbatched entries.
                if (m_root.m_subTrees == null)
                {
                    m_numberOfUnbatchedEntries = 0;
                }

                computeNextBatchTime();
            }

            // The first level is for the root, we don't send that.
            List<GatewayDiscoveryEntity> entities = results != null ? results.subEntities : null;
            if (entities != null && !entities.isEmpty())
            {
                madeProgress = true;

                LoggerInstance.debug("Created batch of %d object(s)...", newBatch.numberOfNodes);

                newBatch.entities       = entities;
                newBatch.sequenceNumber = m_nextSequenceNumber;

                if (LoggerInstance.isEnabled(Severity.DebugObnoxious))
                {
                    LoggerInstance.debugObnoxious(ObjectMappers.prettyPrintAsJson(getObjectMapper(), newBatch));
                }

                trackBatch(newBatch);
            }

            return madeProgress;
        }

        private void trackBatch(PendingTreeBatch batch)
        {
            m_persistedBatches.put(batch.sequenceNumber, batch);
            m_numberOfNodesBatched += batch.numberOfNodes;

            m_nextSequenceNumber = Math.max(m_nextSequenceNumber, batch.sequenceNumber + 1);
        }

        private void flushBatches(boolean force)
        {
            if (m_helper.isActive())
            {
                ZonedDateTime now                        = TimeUtils.now();
                ZonedDateTime thresholdForFlushingToDisk = now.minus(m_flushToDiskDelayInSeconds, ChronoUnit.SECONDS);

                long pendingSize  = 0;
                int  pendingCount = 0;

                for (PendingTreeBatch batch : m_persistedBatches.values())
                {
                    if (batch.isInMemory())
                    {
                        pendingSize += batch.estimatedSize;
                        pendingCount++;
                    }
                }

                if (pendingCount > 1 && pendingSize > 512 * 1024)
                {
                    force = true;
                }

                for (PendingTreeBatch batch : m_persistedBatches.values())
                {
                    //
                    // Flush to disk if too big or too old.
                    //
                    if (force || TimeUtils.isBeforeOrNull(batch.creationTime, thresholdForFlushingToDisk))
                    {
                        try
                        {
                            batch.reduceMemoryFootprint();
                        }
                        catch (Exception e)
                        {
                            LoggerInstance.error("Failed to save batch of %d object(s) to disk, due to %s", batch.numberOfNodes, e);
                        }
                    }
                }
            }
        }

        //--//

        private CompletableFuture<Boolean> sendNextBatch()
        {
            try
            {
                Iterator<PendingTreeBatch> it = m_persistedBatches.values()
                                                                  .iterator();

                if (it.hasNext())
                {
                    PendingTreeBatch batch = it.next();

                    GatewayStatusApi proxy = await(getProxy(GatewayStatusApi.class, 10, TimeUnit.MINUTES));

                    if (batch.ensureInMemory())
                    {
                        m_numberOfUploadedEntitiesRetries += batch.numberOfNodes;
                        await(proxy.publishResults(batch.entities));
                        m_numberOfUploadedEntities += batch.numberOfNodes;
                        m_numberOfUploadedEntitiesRetries -= batch.numberOfNodes;
                        LoggerInstance.debug("Sent batch of %d object(s)", batch.numberOfNodes);

                        m_reportError = null;
                        m_offline     = false;
                    }

                    it.remove();

                    m_numberOfNodesBatched -= batch.numberOfNodes;
                    batch.deleteFromDisk();

                    ensureFlushReport().updateCounter(batch.numberOfNodes);

                    return wrapAsync(true);
                }

                emitLastReport();
            }
            catch (TimeoutException te)
            {
                m_offline = true;

                LoggerInstance.debug("Flushing of entities failed due to connection timeout");
            }
            catch (Throwable t)
            {
                if (shouldReportError())
                {
                    LoggerInstance.error("Flushing of entities failed due to exception: %s", t);
                }
            }

            return wrapAsync(false);
        }
    }

    //--//

    private final ConfigurationPersistenceHelper m_helper;
    private final int                            m_batchPeriodInSeconds;
    private final int                            m_flushToDiskDelayInSeconds;
    private final int                            m_maxNodesPerBatch;
    private final int                            m_maxNodesPerBatchOnCellular;
    private final int                            m_maxBatchSize;
    private final PendingTree                    m_root    = new PendingTree(null);
    private final PendingTreeFlusher             m_flusher = new PendingTreeFlusher();
    private       boolean                        m_suspendNetworks;
    private       MonotonousTime                 m_recentNetworkImport;

    private final Map<String, NetworkState> m_networks = Maps.newHashMap();

    protected GatewayState(ConfigurationPersistenceHelper helper,
                           int batchPeriodInSeconds,
                           int flushToDiskDelayInSeconds,
                           int maxNodesPerBatch,
                           int maxNodesPerBatchOnCellular,
                           int maxBatchSize)
    {
        m_helper                     = requireNonNull(helper);
        m_maxNodesPerBatch           = maxNodesPerBatch;
        m_maxNodesPerBatchOnCellular = maxNodesPerBatchOnCellular;
        m_maxBatchSize               = maxBatchSize;

        m_batchPeriodInSeconds      = batchPeriodInSeconds;
        m_flushToDiskDelayInSeconds = flushToDiskDelayInSeconds;

        m_flusher.reloadBatches();
    }

    public void shutdown()
    {
        m_flusher.shutdown();
    }

    //--//

    public final void exportNetworks(List<GatewayNetwork> networks)
    {
        synchronized (m_networks)
        {
            for (NetworkState networkState : m_networks.values())
            {
                networks.add(networkState.configuration);
            }
        }
    }

    public final CompletableFuture<List<String>> importNetworks(List<GatewayNetwork> networks)
    {
        List<String>       changedNetworks = Lists.newArrayList();
        List<NetworkState> toStart         = Lists.newArrayList();
        List<NetworkState> toStop          = Lists.newArrayList();

        synchronized (m_networks)
        {
            List<GatewayNetwork> networksOld = CollectionUtils.transformToList(m_networks.values(), (gs) -> gs.configuration);

            GatewayNetwork.Delta delta = new GatewayNetwork.Delta(networksOld, networks);

            delta.removed.forEach((gn) ->
                                  {
                                      toStop.add(m_networks.get(gn.sysId));
                                  });

            delta.changed.forEach((gn) ->
                                  {
                                      changedNetworks.add(gn.sysId);
                                      toStop.add(m_networks.get(gn.sysId));
                                      toStart.add(allocateNetworkState(gn));
                                  });

            delta.added.forEach((gn) ->
                                {
                                    changedNetworks.add(gn.sysId);
                                    toStart.add(allocateNetworkState(gn));
                                });

            //--//

            for (NetworkState oldNetwork : toStop)
            {
                m_networks.remove(oldNetwork.configuration.sysId);
            }

            for (NetworkState newNetwork : toStart)
            {
                m_networks.put(newNetwork.configuration.sysId, newNetwork);
            }
        }

        for (NetworkState oldNetwork : toStop)
        {
            try
            {
                await(oldNetwork.stop());
            }
            catch (Throwable t)
            {
                LoggerInstance.error("Failed to stop network '%s', due to %s", oldNetwork.configuration.name, t);
            }
        }

        if (!m_suspendNetworks)
        {
            // To debounce suspend/resume, assume suspended if within the timeout window.
            m_recentNetworkImport = TimeUtils.computeTimeoutExpiration(30, TimeUnit.SECONDS);

            for (NetworkState newNetwork : toStart)
            {
                try
                {
                    await(newNetwork.start());
                }
                catch (Throwable t)
                {
                    LoggerInstance.error("Failed to start network '%s', due to %s", newNetwork.configuration.name, t);
                }
            }
        }

        return wrapAsync(changedNetworks);
    }

    protected CompletableFuture<Void> suspendNetworks()
    {
        if (!m_suspendNetworks)
        {
            m_suspendNetworks = true;

            List<NetworkState> toStop = Lists.newArrayList();

            synchronized (m_networks)
            {
                toStop.addAll(m_networks.values());
            }

            for (NetworkState oldNetwork : toStop)
            {
                try
                {
                    await(oldNetwork.stop());
                }
                catch (Throwable t)
                {
                    LoggerInstance.error("Failed to stop network '%s', due to %s", oldNetwork.configuration.name, t);
                }
            }
        }

        return wrapAsync(null);
    }

    protected CompletableFuture<Void> resumeNetworks()
    {
        if (m_suspendNetworks)
        {
            m_suspendNetworks = false;

            List<NetworkState> toStart = Lists.newArrayList();

            synchronized (m_networks)
            {
                toStart.addAll(m_networks.values());
            }

            for (NetworkState newNetwork : toStart)
            {
                try
                {
                    await(newNetwork.start());
                }
                catch (Throwable t)
                {
                    LoggerInstance.error("Failed to start network '%s', due to %s", newNetwork.configuration.name, t);
                }
            }
        }

        return wrapAsync(null);
    }

    //--//

    public abstract CompletableFuture<Boolean> performAutoDiscovery(GatewayOperationTracker.State operationContext) throws
                                                                                                                    Exception;

    public CompletableFuture<List<GatewayAutoDiscovery>> getAutoDiscoveryResults(GatewayOperationTracker tracker,
                                                                                 GatewayOperationToken token)
    {
        GatewayOperationTracker.State state = tracker.get(token);
        if (state != null)
        {
            @SuppressWarnings("unchecked") List<GatewayAutoDiscovery> results = (List<GatewayAutoDiscovery>) state.getValue();

            return wrapAsync(results);
        }

        return wrapAsync(Collections.emptyList());
    }

    public final CompletableFuture<Boolean> triggerDiscovery(GatewayOperationTracker.State operationContext,
                                                             List<GatewayDiscoveryEntity> entities,
                                                             int broadcastIntervals,
                                                             int rebroadcastCount) throws
                                                                                   Exception
    {
        // To avoid start/stop/start events, skip if network import was recent..
        if (!m_suspendNetworks && TimeUtils.isTimeoutExpired(m_recentNetworkImport))
        {
            await(suspendNetworks());
            await(resumeNetworks());
        }

        return forAllNetworks(entities, (networkState, holder, en_network) -> networkState.discover(operationContext, holder, en_network, broadcastIntervals, rebroadcastCount));
    }

    public final CompletableFuture<Boolean> listObjects(GatewayOperationTracker.State operationContext,
                                                        List<GatewayDiscoveryEntity> entities) throws
                                                                                               Exception
    {
        return forAllNetworks(entities, (networkState, holder, en_network) -> networkState.listObjects(operationContext, holder, en_network));
    }

    public final CompletableFuture<Boolean> readAllValues(GatewayOperationTracker.State operationContext,
                                                          List<GatewayDiscoveryEntity> entities) throws
                                                                                                 Exception
    {
        return forAllNetworks(entities, (networkState, holder, en_network) -> networkState.readAllValues(operationContext, holder, en_network));
    }

    public final CompletableFuture<Boolean> writeValues(GatewayOperationTracker.State operationContext,
                                                        List<GatewayDiscoveryEntity> entities) throws
                                                                                               Exception
    {
        return forAllNetworks(entities, (networkState, holder, en_network) -> networkState.writeValues(operationContext, holder, en_network));
    }

    public final CompletableFuture<Boolean> startSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                                       List<GatewayDiscoveryEntity> entities) throws
                                                                                                              Exception
    {
        return forAllNetworks(entities, (networkState, holder, en_network) -> networkState.startSamplingConfiguration(operationContext, holder, en_network));
    }

    public final CompletableFuture<Boolean> updateSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                                        List<GatewayDiscoveryEntity> entities) throws
                                                                                                               Exception
    {
        return forAllNetworks(entities, (networkState, holder, en_network) -> networkState.updateSamplingConfiguration(operationContext, holder, en_network));
    }

    public final CompletableFuture<Boolean> completeSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                                          List<GatewayDiscoveryEntity> entities) throws
                                                                                                                 Exception
    {
        return forAllNetworks(entities, (networkState, holder, en_network) -> networkState.completeSamplingConfiguration(operationContext, holder, en_network));
    }

    public final CompletableFuture<Boolean> flushEntities() throws
                                                            Exception
    {
        LoggerInstance.debug("Starting flushing of entities...");
        await(waitForFlushingOfEntities());
        LoggerInstance.debug("Completed flushing of entities.");

        return wrapAsync(true);
    }

    public GatewayQueueStatus checkQueueStatus()
    {
        return m_flusher.checkQueueStatus();
    }

    //--//

    @FunctionalInterface
    protected interface NetworkEnumerationCallback
    {
        CompletableFuture<Boolean> process(NetworkState networkState,
                                           GatewayState.ResultHolder holder,
                                           GatewayDiscoveryEntity en_network) throws
                                                                              Exception;
    }

    private CompletableFuture<Boolean> forAllNetworks(List<GatewayDiscoveryEntity> networks,
                                                      NetworkEnumerationCallback callback) throws
                                                                                           Exception
    {
        GatewayState.ResultHolder holder_root = getRoot(TimeUtils.nowEpochSeconds());

        for (GatewayDiscoveryEntity en_network : GatewayDiscoveryEntity.filter(networks, GatewayDiscoveryEntitySelector.Network))
        {
            NetworkState networkState;

            synchronized (m_networks)
            {
                networkState = m_networks.get(en_network.selectorValue);
            }

            if (networkState != null)
            {
                boolean success;

                try
                {
                    GatewayState.ResultHolder holder_network = holder_root.prepareResult(GatewayDiscoveryEntitySelector.Network, en_network.selectorValue, false);

                    success = await(callback.process(networkState, holder_network, en_network));
                }
                catch (Throwable t)
                {
                    LoggerInstance.error("forAllNetworks: Failed while processing network '%s': %s", en_network, t);

                    success = false;
                }

                if (!success)
                {
                    return wrapAsync(false);
                }
            }
        }

        return flushEntities();
    }

    protected abstract NetworkState allocateNetworkState(GatewayNetwork network);

    //--//

    public ResultHolder getRoot(double timeEpochSeconds)
    {
        return new ResultHolderRoot(timeEpochSeconds);
    }

    public void startFlushingOfEntities(boolean drain)
    {
        m_flusher.startFlushingOfEntities(drain);
    }

    public CompletableFuture<Void> waitForFlushingOfEntities() throws
                                                               Exception
    {
        return m_flusher.waitForFlushingOfEntities();
    }

    public int getNumberOfPendingEntities()
    {
        return getNumberOfBatchedEntities() + getNumberOfUnbatchedEntities();
    }

    public int getNumberOfBatchedEntities()
    {
        return m_flusher.m_numberOfNodesBatched;
    }

    public int getNumberOfUnbatchedEntities()
    {
        return m_flusher.m_numberOfUnbatchedEntries;
    }

    public long getNumberOfUploadedEntities()
    {
        return m_flusher.m_numberOfUploadedEntities;
    }

    public long getNumberOfUploadedEntitiesRetries()
    {
        return m_flusher.m_numberOfUploadedEntitiesRetries;
    }

    //--//

    public void saveConfiguration(GatewayNetwork network,
                                  ProtocolConfig target,
                                  ConsumerWithException<OutputStream> emitter)
    {
        synchronized (m_networks)
        {
            PersistedGatewayConfiguration cfg = loadConfiguration();
            if (cfg == null)
            {
                cfg = new PersistedGatewayConfiguration();
            }

            PersistedNetworkConfiguration  networkCfg  = cfg.access(network);
            PersistedProtocolConfiguration protocolCfg = networkCfg.access(target);

            try
            {
                if (protocolCfg.stateFileId == null)
                {
                    protocolCfg.stateFileId = IdGenerator.newGuid();
                }

                m_helper.saveToFile(getStateFile(protocolCfg), (outputStream) ->
                {
                    try (DeflaterOutputStream compressor = new DeflaterOutputStream(outputStream))
                    {
                        emitter.accept(compressor);
                    }
                });
            }
            catch (Throwable t)
            {
                LoggerInstance.error("Failed to save network state, due to %s", t);
            }

            saveConfiguration(cfg);
        }
    }

    public void reloadConfiguration()
    {
        try
        {
            List<CompletableFuture<Boolean>> pendingOps = Lists.newArrayList();

            PersistedGatewayConfiguration cfg = loadConfiguration();
            if (cfg != null)
            {
                for (PersistedNetworkConfiguration networkCfg : cfg.networks)
                {
                    GatewayNetwork gn = networkCfg.network;

                    for (PersistedProtocolConfiguration protocolCfg : networkCfg.protocols)
                    {
                        gn.protocolsConfiguration.add(protocolCfg.target);
                    }

                    pendingOps.add(reloadConfiguration(gn, networkCfg));
                }
            }

            for (CompletableFuture<Boolean> pendingOp : pendingOps)
            {
                pendingOp.get();
            }
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed to apply saved configuration, due to %s", t);
        }
    }

    public void loadState(PersistedProtocolConfiguration protocolCfg,
                          ConsumerWithException<InputStream> callback) throws
                                                                       Exception
    {
        m_helper.loadFromFile(getStateFile(protocolCfg), (stream) ->
        {
            try (InflaterInputStream decompressor = new InflaterInputStream(stream))
            {
                callback.accept(decompressor);
            }

            return null;
        });
    }

    private CompletableFuture<Boolean> reloadConfiguration(GatewayNetwork gn,
                                                           PersistedNetworkConfiguration networkCfg)
    {
        NetworkState ns = allocateNetworkState(gn);

        try
        {
            await(ns.reload(networkCfg));

            synchronized (m_networks)
            {
                m_networks.put(gn.sysId, ns);
            }

            return wrapAsync(true);
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed to start network '%s', due to %s", ns.configuration.name, t);

            return wrapAsync(false);
        }
    }

    private PersistedGatewayConfiguration loadConfiguration()
    {
        File file = getConfigurationFile();
        if (file != null && file.exists())
        {
            try
            {
                return m_helper.deserializeFromFile(file, PersistedGatewayConfiguration.class);
            }
            catch (Throwable t)
            {
                LoggerInstance.error("Failed to reload configuration, due to %s", t);

                try
                {
                    file.delete();
                }
                catch (Throwable t2)
                {
                    // Ignore failures.
                }
            }
        }

        return null;
    }

    private void saveConfiguration(PersistedGatewayConfiguration cfg)
    {
        try
        {
            m_helper.serializeToFile(getConfigurationFile(), cfg);
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed to save configuration, due to %s", t);
        }
    }

    private File getConfigurationFile()
    {
        return m_helper.getFile(c_gateway_config);
    }

    private File getStateFile(PersistedProtocolConfiguration cfg)
    {
        return m_helper.getFile(c_gateway_state + cfg.stateFileId);
    }

    private File getBatchFile(int sequenceNumber)
    {
        return m_helper.getFile(String.format(c_gateway_batch_prefix + "%d." + c_gateway_batch_suffix, sequenceNumber));
    }

    private static Integer parseSequenceNumber(String name)
    {
        Matcher matcher = c_gateway_batch_regex.matcher(name);
        if (matcher.find())
        {
            return Integer.parseInt(matcher.group(1));
        }

        return null;
    }

    //--//

    private int getEffectiveMaxNodes()
    {
        return isCellularConnection() ? m_maxNodesPerBatchOnCellular : m_maxNodesPerBatch;
    }

    protected abstract <T> CompletableFuture<T> getProxy(Class<T> clz,
                                                         int timeout,
                                                         TimeUnit timeoutUnit) throws
                                                                               Exception;

    protected abstract ObjectMapper getObjectMapper();

    protected abstract boolean isCellularConnection();

    public abstract void reportSamplingDone(int sequenceNumber,
                                            String suffix,
                                            long samplingSlot,
                                            int period,
                                            ProgressStatus stats);
}
