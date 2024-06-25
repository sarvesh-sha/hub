/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.spooler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.logic.protocol.BACnetDecoder;
import com.optio3.cloud.hub.logic.protocol.GatewayPerfDecoder;
import com.optio3.cloud.hub.logic.protocol.IProtocolDecoder;
import com.optio3.cloud.hub.logic.protocol.IpnDecoder;
import com.optio3.cloud.hub.logic.protocol.NetworkPerfDecoder;
import com.optio3.cloud.hub.logic.protocol.RestPerfDecoder;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.ResultStagingRecord;
import com.optio3.cloud.persistence.DbAction;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.collection.Memoizer;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.stream.MemoryMappedHeap;
import com.optio3.util.MonotonousTime;
import com.optio3.util.ResourceAutoCleaner;
import com.optio3.util.TimeUtils;

public class StagedResultsSummary implements AutoCloseable
{
    public static final Logger LoggerInstance = new Logger(StagedResultsSummary.class);

    public static final int[] EmptyRecordsArray = new int[0];

    public static class UpdateData
    {
        public final double timestampEpochSeconds;
        public final String contents;

        public UpdateData(double timestampEpochSeconds,
                          String contents)
        {
            this.timestampEpochSeconds = timestampEpochSeconds;
            this.contents              = contents;
        }
    }

    //
    // To process a large amount of data as fast as possible, we uncompress and decode the staged results and laid them out in a memory-mapped disk heap.
    // For each entries, we store timestamp and contents, in a singly linked list:
    //
    // struct Block
    // {
    //      long   nextBlockPointer
    //      int    nextBlockLength
    //      long   utcSeconds        <== -1 means no timestamp
    //      int    contentsLength    <== -1 means no contents
    //      byte[] UTF8contents
    // }
    //
    // We keep track of the last block written, so we can add to the list.
    //
    public static class CompactUpdateData
    {
        public int[]  records    = EmptyRecordsArray;
        public double rangeStart = TimeUtils.maxEpochSeconds();
        public double rangeEnd   = TimeUtils.minEpochSeconds();

        private long m_lastPosition = -1;
        private int  m_lastLength;

        public void add(StagedResultsSummary stagedResultsSummary,
                        ForStagedResult stagedResult,
                        double timestampEpochSeconds,
                        String contents,
                        boolean forSamples) throws
                                            IOException
        {
            rangeStart = Math.min(rangeStart, timestampEpochSeconds);
            rangeEnd   = Math.max(rangeEnd, timestampEpochSeconds);

            records = stagedResult.link(stagedResultsSummary, records, forSamples);

            try (MemoryMappedHeap.SkippableOutputStream stream = stagedResultsSummary.m_heap.resource.allocateAsOutputStream())
            {
                DataOutputStream dataOutput = new DataOutputStream(stream);
                dataOutput.writeLong(m_lastPosition); // Link with previous record.
                dataOutput.writeInt(m_lastLength);

                dataOutput.writeDouble(timestampEpochSeconds);

                MemoryMappedHeap.serializeString(dataOutput, contents);

                m_lastPosition = stream.absolutePosition();
                m_lastLength   = (int) stream.position();
            }
        }

        public List<UpdateData> get(StagedResultsSummary stagedResultsSummary) throws
                                                                               IOException
        {
            List<UpdateData> res = Lists.newArrayList();

            long position = m_lastPosition;
            int  length   = m_lastLength;

            while (position >= 0)
            {
                InputStream     stream    = stagedResultsSummary.m_heap.resource.sliceAsInputStream(position, length);
                DataInputStream dataInput = new DataInputStream(stream);

                position = dataInput.readLong();
                length   = dataInput.readInt();

                double timestampEpochSeconds = dataInput.readDouble();
                String contents              = MemoryMappedHeap.deserializeString(dataInput);

                res.add(new UpdateData(timestampEpochSeconds, contents));
            }

            res.sort(Comparator.comparingDouble(a -> a.timestampEpochSeconds));

            return res;
        }

        public void markRecords(StagedResultsSummary stagedResultsSummary,
                                boolean forSamples)
        {
            for (int seqNum : records)
            {
                StagedResultsSummary.ForStagedResult stagedResult = stagedResultsSummary.getStagedResult(seqNum);

                stagedResult.markAsProcessed(stagedResultsSummary, forSamples);
            }
        }
    }

    public static class ForObject
    {
        public final Class<?> decoder;
        public final String   identifier;

        public boolean markedForFlushing;

        public CompactUpdateData objectData;
        public CompactUpdateData sampleData;

        //
        // We set this flag once we flush the samples to database.
        // On a second pass, we don't flush the remaining samples if the flag is set.
        // This allows us to fully drain older results, even if we see new samples coming in.
        // Required for the proper completion of the background import.
        //
        public boolean samplesProcessedOnce;

        //--//

        private ForObject(Class<?> decoder,
                          String identifier)
        {
            this.decoder    = decoder;
            this.identifier = identifier;
        }

        public double getEarliestEntry(boolean prioritizeObjectUpdates)
        {
            double t = TimeUtils.maxEpochSeconds();

            t = minTimestamp(t, objectData);

            if (!prioritizeObjectUpdates)
            {
                t = minTimestamp(t, sampleData);
            }

            return t;
        }

        public void registerUpdate(StagedResultsSummary stagedResultsSummary,
                                   ForStagedResult stagedResult,
                                   double timestampEpochSeconds,
                                   String contents) throws
                                                    IOException
        {
            if (objectData == null)
            {
                objectData = new CompactUpdateData();
            }

            objectData.add(stagedResultsSummary, stagedResult, timestampEpochSeconds, contents, false);
        }

        public void registerSample(StagedResultsSummary stagedResultsSummary,
                                   ForStagedResult stagedResult,
                                   double timestampEpochSeconds,
                                   String contents) throws
                                                    IOException
        {
            if (sampleData == null)
            {
                sampleData = new CompactUpdateData();
            }

            sampleData.add(stagedResultsSummary, stagedResult, timestampEpochSeconds, contents, true);
        }

        public boolean hasPendingData()
        {
            return objectData != null || sampleData != null;
        }

        public void forceMarkAsProcessed(StagedResultsSummary stagedResultsSummary)
        {
            if (objectData != null)
            {
                objectData.markRecords(stagedResultsSummary, false);
                objectData = null;
            }

            if (sampleData != null)
            {
                sampleData.markRecords(stagedResultsSummary, true);
                sampleData = null;
            }

            samplesProcessedOnce = true;
        }
    }

    //--//

    public static class ForAsset
    {
        public final IProtocolDecoder                 decoder;
        public final BaseAssetDescriptor              identifier;
        public final ConcurrentMap<String, ForObject> objects = Maps.newConcurrentMap();

        public CompactUpdateData assetData;
        public CompactUpdateData assetReachabilityData;

        private ForAsset(IProtocolDecoder decoder,
                         BaseAssetDescriptor identifier)
        {
            this.decoder    = decoder;
            this.identifier = identifier;
        }

        public List<ForObject> sortByTimestamp(boolean prioritizeObjectUpdates)
        {
            List<ForObject> lst = Lists.newArrayList(objects.values());

            lst.sort(Comparator.comparingDouble(a -> a.getEarliestEntry(prioritizeObjectUpdates)));

            return lst;
        }

        public double getEarliestEntry(boolean prioritizeObjectUpdates)
        {
            double t = TimeUtils.maxEpochSeconds();

            t = minTimestamp(t, assetData);
            t = minTimestamp(t, assetReachabilityData);

            for (ForObject forObject : objects.values())
            {
                t = Math.min(t, forObject.getEarliestEntry(prioritizeObjectUpdates));
            }

            return t;
        }

        public void registerUpdate(StagedResultsSummary stagedResultsSummary,
                                   ForStagedResult stagedResult,
                                   double timestampEpochSeconds,
                                   String contents) throws
                                                    IOException
        {
            if (assetData == null)
            {
                assetData = new CompactUpdateData();
            }

            assetData.add(stagedResultsSummary, stagedResult, timestampEpochSeconds, contents, false);
        }

        public void registerReachability(StagedResultsSummary stagedResultsSummary,
                                         ForStagedResult stagedResult,
                                         double timestampEpochSeconds,
                                         String contents) throws
                                                          IOException
        {
            if (assetReachabilityData == null)
            {
                assetReachabilityData = new CompactUpdateData();
            }

            assetReachabilityData.add(stagedResultsSummary, stagedResult, timestampEpochSeconds, contents, false);
        }

        public boolean hasPendingData()
        {
            return assetData != null || assetReachabilityData != null;
        }

        public ForObject registerObject(StagedResultsSummary stagedResultsSummary,
                                        Class<?> decoder,
                                        String id)
        {
            ForObject result = objects.get(id);
            if (result == null)
            {
                result = new ForObject(decoder, stagedResultsSummary.m_cache.intern(id));

                ForObject oldResult = objects.putIfAbsent(result.identifier, result);
                if (oldResult != null)
                {
                    result = oldResult;
                }
            }

            return result;
        }

        public void forceMarkAsProcessed(StagedResultsSummary stagedResultsSummary)
        {
            if (assetData != null)
            {
                assetData.markRecords(stagedResultsSummary, false);
                assetData = null;
            }

            if (assetReachabilityData != null)
            {
                assetReachabilityData.markRecords(stagedResultsSummary, false);
                assetReachabilityData = null;
            }

            for (ForObject forObject : objects.values())
            {
                forObject.forceMarkAsProcessed(stagedResultsSummary);
            }
        }
    }

    //--//

    public static class ForRoot
    {
        public final String                           sysId;
        public final DiscoveredAssetsSummary.RootKind rootKind;
        public final ConcurrentMap<Object, ForAsset>  assets = Maps.newConcurrentMap();

        private ForRoot(String sysId,
                        DiscoveredAssetsSummary.RootKind rootKind)
        {
            this.sysId    = sysId;
            this.rootKind = rootKind;
        }

        public ForAsset registerDevice(StagedResultsSummary stagedResultsSummary,
                                       IProtocolDecoder decoder,
                                       BaseAssetDescriptor id)
        {
            ForAsset result = assets.get(id);
            if (result == null)
            {
                result = new ForAsset(decoder, stagedResultsSummary.m_cache.intern(id, BaseAssetDescriptor.class));

                ForAsset oldResult = assets.putIfAbsent(result.identifier, result);
                if (oldResult != null)
                {
                    result = oldResult;
                }
            }

            return result;
        }

        public List<ForAsset> sortByTimestamp(boolean prioritizeObjectUpdates)
        {
            List<ForAsset> lst = Lists.newArrayList(assets.values());

            lst.sort(Comparator.comparingDouble(a -> a.getEarliestEntry(prioritizeObjectUpdates)));

            return lst;
        }

        public void forceMarkAsProcessed(StagedResultsSummary stagedResultsSummary)
        {
            for (ForAsset forAsset : assets.values())
            {
                forAsset.forceMarkAsProcessed(stagedResultsSummary);
            }
        }
    }

    //--//

    public static class ForStagedResult
    {
        private final int    m_sequenceNumber;
        private final String m_sysId;

        private boolean m_objectsProcessed;
        private boolean m_samplesProcessed;

        private int m_linkedForObjects;
        private int m_linkedForSamples;

        private ForStagedResult(int sequenceNumber,
                                String sysId,
                                boolean objectsProcessed,
                                boolean samplesProcessed)
        {
            m_sequenceNumber   = sequenceNumber;
            m_sysId            = sysId;
            m_objectsProcessed = objectsProcessed;
            m_samplesProcessed = samplesProcessed;
        }

        @Override
        public boolean equals(Object o)
        {
            ForStagedResult that = Reflection.as(o, ForStagedResult.class);
            if (that == null)
            {
                return false;
            }

            return Objects.equals(m_sysId, that.m_sysId);
        }

        @Override
        public int hashCode()
        {
            return m_sysId.hashCode();
        }

        @Override
        public String toString()
        {
            return m_sysId;
        }

        //--//

        void markAsProcessed(StagedResultsSummary stagedResultsSummary,
                             boolean forSamples)
        {
            if (forSamples)
            {
                m_linkedForSamples--;
                stagedResultsSummary.m_totalPendingSamples--;
            }
            else
            {
                m_linkedForObjects--;
                stagedResultsSummary.m_totalPendingObjects--;
            }
        }

        //--//

        private int[] link(StagedResultsSummary stagedResultsSummary,
                           int[] recordsWithUpdates,
                           boolean forSamples)
        {
            int length = recordsWithUpdates.length;
            for (int recordsWithUpdate : recordsWithUpdates)
            {
                if (recordsWithUpdate == m_sequenceNumber)
                {
                    //
                    // If the record is already referenced in this chain, we don't update the number of pending items.
                    // This is because we don't have a reference count. Thus we can at most count the presence of the record.
                    //
                    return recordsWithUpdates;
                }
            }

            recordsWithUpdates         = Arrays.copyOf(recordsWithUpdates, length + 1);
            recordsWithUpdates[length] = m_sequenceNumber;

            if (forSamples)
            {
                m_linkedForSamples++;
                stagedResultsSummary.m_totalPendingSamples++;
            }
            else
            {
                m_linkedForObjects++;
                stagedResultsSummary.m_totalPendingObjects++;
            }

            return recordsWithUpdates;
        }
    }

    //--//

    private final static int c_reportFrequency = 1;

    private final ResourceAutoCleaner<MemoryMappedHeap> m_heap = new ResourceAutoCleaner<>(this, new MemoryMappedHeap("StagedResultsSummary", 64 * 1024 * 1024, 0));
    private final Memoizer                              m_cache;

    private final List<ForStagedResult>         m_stagedRecordsInOrder          = Lists.newLinkedList();
    private final Map<Integer, ForStagedResult> m_stagedRecordsBySequenceNumber = Maps.newHashMap();
    private final Map<String, ForStagedResult>  m_stagedRecordsBySysId          = Maps.newHashMap();
    private       int                           m_stagedRecordsSequenceNumber;
    private       ZonedDateTime                 m_lastImport;

    private MonotonousTime m_nextProgressReport;
    private int            m_totalPendingObjects;
    private int            m_totalPendingSamples;
    private boolean        m_shouldFlush;
    private boolean        m_mustFlush;
    private int            m_yieldCount;
    private Stopwatch      m_st;

    public final ConcurrentMap<String, ForRoot> roots = Maps.newConcurrentMap();

    private final List<IProtocolDecoder> m_decoders = Lists.newArrayList();

    //--//

    public StagedResultsSummary(Memoizer cache)
    {
        m_cache = cache;

        m_decoders.add(new BACnetDecoder());
        m_decoders.add(new IpnDecoder());
        m_decoders.add(new GatewayPerfDecoder());
        m_decoders.add(new NetworkPerfDecoder());
        m_decoders.add(new RestPerfDecoder());
    }

    @Override
    public void close()
    {
        m_heap.clean();
    }

    //--//

    public int getTotalPendingObjects()
    {
        return m_totalPendingObjects;
    }

    public int getTotalPendingSamples()
    {
        return m_totalPendingSamples;
    }

    public long getMemoryMappedHeapSize()
    {
        return m_heap.resource.length();
    }

    public boolean shouldFlush()
    {
        return m_shouldFlush;
    }

    public boolean mustFlush()
    {
        return m_mustFlush;
    }

    public boolean hasPendingObjectUpdates()
    {
        for (ForRoot network : roots.values())
        {
            for (ForAsset forAsset : network.assets.values())
            {
                if (forAsset.hasPendingData())
                {
                    return true;
                }

                for (StagedResultsSummary.ForObject forObject : forAsset.objects.values())
                {
                    if (forObject.objectData != null)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public void emitDatabaseEvents(HubApplication app,
                                   DiscoveredAssetsSummary summaryForAssets)

    {
        for (ForRoot root : roots.values())
        {
            DiscoveredAssetsSummary.ForRoot rootSummaryForAsset = summaryForAssets.registerRoot(root.sysId, root.rootKind, false);
            if (rootSummaryForAsset != null)
            {
                for (ForAsset forAsset : root.assets.values())
                {
                    DiscoveredAssetsSummary.ForAsset assetSummaryForAsset = rootSummaryForAsset.registerAsset(summaryForAssets, null, forAsset.identifier, false);
                    if (assetSummaryForAsset != null)
                    {
                        emitEventIfNeeded(app, assetSummaryForAsset.clz, assetSummaryForAsset.getSysId(), true, forAsset.assetData);
                        emitEventIfNeeded(app, assetSummaryForAsset.clz, assetSummaryForAsset.getSysId(), true, forAsset.assetReachabilityData);

                        for (StagedResultsSummary.ForObject forObject : forAsset.objects.values())
                        {
                            DiscoveredAssetsSummary.ForObject objectSummaryForAsset = assetSummaryForAsset.registerObject(summaryForAssets, forObject.identifier, false);
                            if (objectSummaryForAsset != null)
                            {
                                emitEventIfNeeded(app, DeviceElementRecord.class, objectSummaryForAsset.getSysId(), true, forObject.objectData);
                                emitEventIfNeeded(app, DeviceElementRecord.class, objectSummaryForAsset.getSysId(), false, forObject.sampleData);
                            }
                        }
                    }
                }
            }
        }
    }

    private void emitEventIfNeeded(HubApplication app,
                                   Class<?> clz,
                                   String sysId,
                                   boolean direct,
                                   CompactUpdateData data)
    {
        if (sysId != null && data != null && TimeUtils.isValid(data.rangeEnd))
        {
            app.postDatabaseEvent(null, clz, sysId, direct ? DbAction.UPDATE_DIRECT : DbAction.UPDATE_INDIRECT, TimeUtils.fromTimestampToUtcTime(data.rangeEnd));
            data.rangeEnd = TimeUtils.minEpochSeconds();
        }
    }

    public void importRecords(SessionHolder holder) throws
                                                    Exception
    {
        class ImportStats
        {
            ZonedDateTime firstTimestamp;
            ZonedDateTime lastTimestamp;

            boolean update(ResultStagingRecord rec)
            {
                ZonedDateTime createdOn = rec.getCreatedOn();
                if (firstTimestamp == null)
                {
                    firstTimestamp = createdOn;
                }

                lastTimestamp = TimeUtils.updateIfAfter(lastTimestamp, createdOn);

                Duration span = Duration.between(firstTimestamp, lastTimestamp);
                if (span.toHours() >= 72) // Three days
                {
                    m_shouldFlush = true;
                }

                long heapSize = getMemoryMappedHeapSize();
                if (heapSize > 3_000_000_000L) // Max out at 3GB per session.
                {
                    m_shouldFlush = true;
                    m_mustFlush   = true; // We need to drain the spooler, it's getting too big.
                }

                if (heapSize > 1_500_000_000L)
                {
                    m_shouldFlush = true; // The spooler size is getting big, best to attemp to flush.
                }

                return !m_shouldFlush;
            }
        }

        final RecordHelper<ResultStagingRecord> helper_staging = holder.createHelper(ResultStagingRecord.class);
        ImportStats                             stats          = new ImportStats();

        ResultStagingRecord.streamUnprocessed(helper_staging, -1, m_lastImport, (rec) ->
        {
            if (!importStagedResult(rec))
            {
                return true;
            }

            return stats.update(rec);
        });

        for (IProtocolDecoder decoder : m_decoders)
        {
            decoder.importDone(holder);
        }

        // Remember the time, minus a minute, such that next time around, we'll only process newer records.
        if (stats.lastTimestamp != null)
        {
            m_lastImport = stats.lastTimestamp.minus(1, ChronoUnit.MINUTES);
        }
    }

    private boolean importStagedResult(ResultStagingRecord rec) throws
                                                                Exception
    {
        final boolean objectsProcessed = rec.getObjectsProcessed();
        final boolean samplesProcessed = rec.getSamplesProcessed();

        if (LoggerInstance.isEnabled(Severity.DebugVerbose))
        {
            LoggerInstance.debugVerbose("importStagedResult (%s/%s/%s):\n%s", rec.getSysId(), objectsProcessed, samplesProcessed, ObjectMappers.prettyPrintAsJson(rec.getEntities()));
        }

        if (objectsProcessed && samplesProcessed)
        {
            return false;
        }

        final String sysId = rec.getSysId();
        if (m_stagedRecordsBySysId.containsKey(sysId))
        {
            return false;
        }

        ForStagedResult stagedResult = new ForStagedResult(m_stagedRecordsSequenceNumber++, m_cache.intern(sysId), objectsProcessed, samplesProcessed);

        m_stagedRecordsInOrder.add(stagedResult);
        m_stagedRecordsBySequenceNumber.put(stagedResult.m_sequenceNumber, stagedResult);
        m_stagedRecordsBySysId.put(stagedResult.m_sysId, stagedResult);

        final boolean includeObjects = !stagedResult.m_objectsProcessed;
        final boolean includeSamples = !stagedResult.m_samplesProcessed;

        for (IProtocolDecoder decoder : m_decoders)
        {
            GatewayDiscoveryEntitySelector rootSelector = decoder.getRootSelector();

            for (GatewayDiscoveryEntity en_root : GatewayDiscoveryEntity.filter(rec.getEntities(), rootSelector))
            {
                DiscoveredAssetsSummary.RootKind rootKind;

                switch (rootSelector)
                {
                    case Gateway:
                        rootKind = DiscoveredAssetsSummary.RootKind.Gateway;
                        break;

                    case Network:
                        rootKind = DiscoveredAssetsSummary.RootKind.Network;
                        break;

                    case Host:
                        rootKind = DiscoveredAssetsSummary.RootKind.Host;
                        break;

                    default:
                        rootKind = DiscoveredAssetsSummary.RootKind.Device;
                        break;
                }

                ForRoot rootSummaryForResult = registerRoot(en_root.selectorValue, rootKind);

                for (GatewayDiscoveryEntity en_protocol : en_root.filter(GatewayDiscoveryEntitySelector.Protocol, decoder.getProtocolValue()))
                {
                    for (GatewayDiscoveryEntity en_device : en_protocol.filter(decoder.getDeviceSelector()))
                    {
                        BaseAssetDescriptor identifier = decoder.decodeDeviceContext(en_root, en_device);

                        StagedResultsSummary.ForAsset assetSummaryForResult = rootSummaryForResult.registerDevice(this, decoder, identifier);

                        BaseAssetDescriptor target = assetSummaryForResult.identifier;

                        decoder.mergeDetails(target, identifier);

                        if (includeObjects)
                        {
                            double timestampEpoch = en_device.getTimestampEpoch();

                            if (TimeUtils.isValid(timestampEpoch) || en_device.contents != null)
                            {
                                assetSummaryForResult.registerUpdate(this, stagedResult, timestampEpoch, en_device.contents);
                            }

                            GatewayDiscoveryEntitySelector selector = decoder.getDeviceReachabilitySelector();
                            if (selector != null)
                            {
                                for (GatewayDiscoveryEntity en_deviceReachability : en_device.filter(selector))
                                {
                                    double timestampEpoch2 = en_deviceReachability.getTimestampEpoch();
                                    if (TimeUtils.isValid(timestampEpoch2))
                                    {
                                        assetSummaryForResult.registerReachability(this, stagedResult, timestampEpoch2, en_deviceReachability.contents);
                                    }
                                }
                            }
                        }

                        for (GatewayDiscoveryEntity en_object : en_device.filter(decoder.getObjectSelector()))
                        {
                            StagedResultsSummary.ForObject objectSummaryForResult = assetSummaryForResult.registerObject(this, decoder.getClass(), en_object.selectorValue);

                            if (includeObjects)
                            {
                                double timestampEpoch = en_object.getTimestampEpoch();

                                if (TimeUtils.isValid(timestampEpoch) || en_object.contents != null)
                                {
                                    objectSummaryForResult.registerUpdate(this, stagedResult, timestampEpoch, en_object.contents);
                                }
                            }

                            if (includeSamples)
                            {
                                for (GatewayDiscoveryEntity en_sample : en_object.filter(decoder.getSampleSelector()))
                                {
                                    double timestampEpoch = en_sample.getTimestampEpoch();

                                    if (TimeUtils.isValid(timestampEpoch) || en_sample.contents != null)
                                    {
                                        objectSummaryForResult.registerSample(this, stagedResult, timestampEpoch, en_sample.contents);

                                        decoder.trackSampleIfNeeded(rootSummaryForResult, target, objectSummaryForResult.identifier, timestampEpoch, en_sample.contents);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    //--//

    private ForRoot registerRoot(String sysId,
                                 DiscoveredAssetsSummary.RootKind rootKind)
    {
        ForRoot result = roots.get(sysId);
        if (result == null)
        {
            result = new ForRoot(m_cache.intern(sysId), rootKind);

            ForRoot oldResult = roots.putIfAbsent(result.sysId, result);
            if (oldResult != null)
            {
                result = oldResult;
            }
        }

        return result;
    }

    //--//

    public ForStagedResult getStagedResult(int sequenceNumber)
    {
        return m_stagedRecordsBySequenceNumber.get(sequenceNumber);
    }

    public void flush(Logger loggerInstance,
                      RecordHelper<ResultStagingRecord> helper)
    {
        for (Iterator<ForStagedResult> it = m_stagedRecordsInOrder.iterator(); it.hasNext(); )
        {
            ForStagedResult stagedResult = it.next();

            boolean needsObjectProcessing = stagedResult.m_linkedForObjects > 0;
            boolean needsSampleProcessing = stagedResult.m_linkedForSamples > 0;

            if (needsObjectProcessing && needsSampleProcessing)
            {
                continue;
            }

            boolean fetch = false;

            if (!needsObjectProcessing && !stagedResult.m_objectsProcessed)
            {
                fetch = true;
            }

            if (!needsSampleProcessing && !stagedResult.m_samplesProcessed)
            {
                fetch = true;
            }

            if (fetch)
            {
                ResultStagingRecord rec   = helper.getOrNull(stagedResult.m_sysId);
                boolean             flush = false;

                if (!needsObjectProcessing && !rec.getObjectsProcessed())
                {
                    loggerInstance.debug("Done processing objects in staged result '%s'", stagedResult);
                    rec.setObjectsProcessed(true);
                    stagedResult.m_objectsProcessed = true;
                    flush                           = true;
                }

                if (!needsSampleProcessing && !rec.getSamplesProcessed())
                {
                    loggerInstance.debug("Done processing samples in staged result '%s'", stagedResult);
                    rec.setSamplesProcessed(true);
                    stagedResult.m_samplesProcessed = true;
                    flush                           = true;
                }

                if (flush)
                {
                    helper.flushAndEvict(rec);
                }
                else
                {
                    helper.evict(rec);
                }
            }

            if (!needsObjectProcessing && !needsSampleProcessing)
            {
                it.remove();
                m_stagedRecordsBySequenceNumber.remove(stagedResult.m_sequenceNumber);
                m_stagedRecordsBySysId.remove(stagedResult.m_sysId);

                loggerInstance.debug("Done processing staged result '%s', %d to go...", stagedResult, m_stagedRecordsInOrder.size());
            }
        }

        reportProgress(loggerInstance, false, false);
    }

    public void reportProgress(Logger loggerInstance,
                               boolean startSampleProcessing,
                               boolean lastCall)
    {
        if (m_st == null && startSampleProcessing)
        {
            m_st                 = Stopwatch.createStarted();
            m_yieldCount         = 0;
            m_nextProgressReport = null;
        }

        if (TimeUtils.isTimeoutExpired(m_nextProgressReport))
        {
            if (m_totalPendingObjects > 0 || m_totalPendingSamples > 0)
            {
                if (m_st != null)
                {
                    if (m_nextProgressReport == null)
                    {
                        loggerInstance.info("Progress report: %,d objects to go, %,d samples to go, %,d bytes used in disk heap.",
                                            m_totalPendingObjects,
                                            m_totalPendingSamples,
                                            getMemoryMappedHeapSize());
                    }
                    else
                    {
                        loggerInstance.info("Progress report: %,d objects to go, %,d samples to go, after %,d seconds and %,d yields...",
                                            m_totalPendingObjects,
                                            m_totalPendingSamples,
                                            m_st.elapsed(TimeUnit.SECONDS),
                                            m_yieldCount);
                    }
                }
                else
                {
                    loggerInstance.info("Progress report: %,d objects, %,d samples...", m_totalPendingObjects, m_totalPendingSamples);
                }
            }

            m_nextProgressReport = TimeUtils.computeTimeoutExpiration(c_reportFrequency, TimeUnit.MINUTES);
        }

        if (lastCall)
        {
            loggerInstance.info("Progress report: done in %,d seconds!", m_st.elapsed(TimeUnit.SECONDS));
        }
    }

    public void reportYield()
    {
        m_yieldCount++;
    }

    //--//

    private static double minTimestamp(double t,
                                       CompactUpdateData data)
    {
        if (data != null && TimeUtils.isValid(data.rangeStart))
        {
            t = Math.min(t, data.rangeStart);
        }

        return t;
    }
}