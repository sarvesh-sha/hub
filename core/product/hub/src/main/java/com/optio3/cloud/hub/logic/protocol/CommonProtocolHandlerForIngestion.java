/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.protocol;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.logic.spooler.DiscoveredAssetsSummary;
import com.optio3.cloud.hub.logic.spooler.StagedResultsSummary;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.ResultStagingRecord;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.cloud.persistence.LazyRecordFlusher;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.logging.ILogger;
import com.optio3.logging.Logger;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.util.TimeUtils;

public abstract class CommonProtocolHandlerForIngestion<T extends AssetRecord, TS extends BaseObjectModel>
{
    public static abstract class SamplesContext<T extends AssetRecord, TS extends BaseObjectModel>
    {
        private List<SampleValue> m_values = Lists.newArrayList();

        public abstract boolean processSampleUpdate(ILogger logger,
                                                    BatchIngestionContext context,
                                                    StagedResultsSummary.ForAsset forAsset,
                                                    StagedResultsSummary.ForObject forObject,
                                                    double timestampEpochSeconds,
                                                    String contents);

        public abstract void postProcessSampleUpdate(DeviceElementRecord.ArchiveHolder archiveHolder);

        public void addValue(String prop,
                             TimeSeries.SampleType sampleType,
                             TimeSeries.SampleResolution resolution,
                             double timestampEpochSeconds,
                             int digitsOfPrecision,
                             Object value)
        {
            SampleValue sv = new SampleValue(prop, sampleType, resolution, timestampEpochSeconds, digitsOfPrecision, value);
            m_values.add(sv);
        }
    }

    static class SampleValue
    {
        final String                prop;
        final TimeSeries.SampleType sampleType;

        final TimeSeries.SampleResolution resolution;
        final double                      timestampEpochSeconds;

        final int    digitsOfPrecision;
        final Object value;

        SampleValue(String prop,
                    TimeSeries.SampleType sampleType,
                    TimeSeries.SampleResolution resolution,
                    double timestampEpochSeconds,
                    int digitsOfPrecision,
                    Object value)
        {
            this.prop       = prop;
            this.sampleType = sampleType;

            this.resolution            = resolution;
            this.timestampEpochSeconds = timestampEpochSeconds;

            this.digitsOfPrecision = digitsOfPrecision;
            this.value             = value;
        }
    }

    //--//

    protected final HubConfiguration m_cfg;
    protected final Logger           m_logger;
    protected final Class<T>         m_clzDevice;

    //--//

    protected final StagedResultsSummary.ForAsset    m_forAsset;
    private         DiscoveredAssetsSummary.ForAsset m_assetSummary;
    private         LazyRecordFlusher<T>             m_assetRecord;

    //--//

    private   StagedResultsSummary.ForObject         m_forObject;
    private   DiscoveredAssetsSummary.ForObject      m_objectSummary;
    private   LazyRecordFlusher<DeviceElementRecord> m_objectRecord;
    protected boolean                                m_objectReconfigureSampling;

    //--//

    private DeviceElementRecord.ArchiveHolder m_objectArchiveHolder;

    //--//

    protected CommonProtocolHandlerForIngestion(HubConfiguration cfg,
                                                Logger logger,
                                                Class<T> clzDevice,
                                                StagedResultsSummary.ForAsset forAsset)
    {
        m_cfg       = cfg;
        m_logger    = logger;
        m_clzDevice = clzDevice;
        m_forAsset  = forAsset;
    }

    //--//

    public void process(BatchIngestionContext context) throws
                                                       Exception
    {
        m_logger.debugVerbose("processDevice: %s (%d objects)", m_forAsset.identifier, m_forAsset.objects.size());

        m_assetSummary = fetchAssetSummary(context);

        if (m_forAsset.assetData != null)
        {
            for (StagedResultsSummary.UpdateData ud : m_forAsset.assetData.get(context.summaryForPending))
            {
                processAssetUpdate(context, ud.timestampEpochSeconds, ud.contents);
            }

            m_forAsset.assetData.markRecords(context.summaryForPending, false);
            m_forAsset.assetData = null;

            context.markUpdate(1);
        }

        if (m_forAsset.assetReachabilityData != null)
        {
            for (StagedResultsSummary.UpdateData ud : m_forAsset.assetReachabilityData.get(context.summaryForPending))
            {
                processAssetReachability(context, ud.timestampEpochSeconds, ud.contents);
            }

            m_forAsset.assetReachabilityData.markRecords(context.summaryForPending, false);
            m_forAsset.assetReachabilityData = null;

            context.markUpdate(1);
        }

        //--//

        for (StagedResultsSummary.ForObject forObject : m_forAsset.sortByTimestamp(!context.mode.shouldProcessSamples()))
        {
            if (context.mode.shouldProcessOnlyMarkedObjects() && !forObject.markedForFlushing)
            {
                continue;
            }

            forObject.markedForFlushing = false;

            m_forObject = forObject;
            processObject(context);

            context.flushIfNeeded();
        }

        if (m_assetRecord != null)
        {
            T rec_device = m_assetRecord.getAfterPersist();
            m_assetSummary.setSysId(context.summaryForDatabase, rec_device.getSysId());
            m_assetRecord = null;

            context.helper_asset.flushAndEvict(rec_device);
        }

        context.flushIfNeeded();
    }

    protected abstract DiscoveredAssetsSummary.ForAsset fetchAssetSummary(BatchIngestionContext context);

    protected final RecordHelper<T> getHelper(BatchIngestionContext context)
    {
        return context.sessionHolder.createHelper(m_clzDevice);
    }

    protected final T ensureAssetRecord(BatchIngestionContext context,
                                        boolean shouldPersist) throws
                                                               Exception
    {
        if (m_assetRecord == null)
        {
            m_assetRecord = fetchAssetRecord(context, m_assetSummary);
        }

        return shouldPersist ? m_assetRecord.getAfterPersist() : m_assetRecord.get();
    }

    protected abstract LazyRecordFlusher<T> fetchAssetRecord(BatchIngestionContext context,
                                                             DiscoveredAssetsSummary.ForAsset assetSummary) throws
                                                                                                            Exception;

    protected abstract void processAssetUpdate(BatchIngestionContext context,
                                               double timestampEpochSeconds,
                                               String contents) throws
                                                                Exception;

    protected abstract void processAssetReachability(BatchIngestionContext context,
                                                     double timestampEpochSeconds,
                                                     String contents) throws
                                                                      Exception;

    //--//

    private void processObject(BatchIngestionContext context) throws
                                                              Exception
    {
        if (m_forObject.objectData != null)
        {
            m_logger.debugVerbose("   processObject: %s (%d samples)", m_forObject.identifier, m_forObject.objectData.records.length);

            for (StagedResultsSummary.UpdateData ud : m_forObject.objectData.get(context.summaryForPending))
            {
                processObjectUpdate(context, ud.timestampEpochSeconds, ud.contents);
            }

            m_forObject.objectData.markRecords(context.summaryForPending, false);
            m_forObject.objectData = null;

            context.markUpdate(1);
        }

        //--//

        SamplesContext<T, TS> sampleUpdateContext = null;

        if (context.mode.shouldProcessSamples())
        {
            if (!m_forObject.samplesProcessedOnce && m_forObject.sampleData != null)
            {
                sampleUpdateContext = processSamples(context);

                m_forObject.sampleData.markRecords(context.summaryForPending, true);
                m_forObject.sampleData = null;

                context.markUpdate(1);
                m_forObject.samplesProcessedOnce = true;
            }
        }

        //--//

        if (m_objectArchiveHolder != null)
        {
            context.markUpdate(m_objectArchiveHolder.getNumberOfArchivesUsed());

            m_objectArchiveHolder.close();

            if (sampleUpdateContext != null)
            {
                sampleUpdateContext.postProcessSampleUpdate(m_objectArchiveHolder);
            }

            m_objectArchiveHolder = null;
        }

        //--//

        if (m_objectRecord != null)
        {
            DeviceElementRecord rec_object = m_objectRecord.getAfterPersist();
            m_objectSummary.setSysId(context.summaryForDatabase, rec_object.getSysId());
            m_objectRecord = null;

            if (m_objectReconfigureSampling)
            {
                m_objectReconfigureSampling = false;

                rec_object.reconfigureSampling(context.helper_element.currentSessionHolder());
            }

            context.helper_element.flushAndEvict(rec_object);
        }
    }

    private SamplesContext<T, TS> processSamples(BatchIngestionContext context) throws
                                                                                Exception
    {
        SamplesContext<T, TS> samplesContext = prepareSampleUpdateContext();

        if (m_forObject.sampleData != null)
        {
            m_logger.debugVerbose("   processSamples: %s (%d samples)", m_forObject.identifier, m_forObject.sampleData.records.length);

            for (StagedResultsSummary.UpdateData ud : m_forObject.sampleData.get(context.summaryForPending))
            {
                if (!samplesContext.processSampleUpdate(m_logger, context, m_forAsset, m_forObject, ud.timestampEpochSeconds, ud.contents))
                {
                    // Even if there are no values, we create an entry in the time series, to track failures.
                    samplesContext.addValue(null, null, TimeSeries.SampleResolution.Max1Hz, ud.timestampEpochSeconds, 0, null);
                }
            }
        }

        if (m_objectArchiveHolder == null)
        {
            DeviceElementRecord rec_element = ensureObjectRecord(context, true);
            m_objectArchiveHolder = rec_element.newArchiveHolder(context.helper_archive);
        }

        for (SampleValue sv : samplesContext.m_values)
        {
            TimeSeries ts = m_objectArchiveHolder.getTimeSeries(sv.timestampEpochSeconds);

            ts.addSample(sv.resolution, sv.timestampEpochSeconds, sv.prop, sv.sampleType, sv.digitsOfPrecision, sv.value);
        }

        m_objectArchiveHolder.compactIfNeeded();

        samplesContext.m_values = null;

        return samplesContext;
    }

    public void flushSamples(StagedResultsSummary summaryForPending,
                             StagedResultsSummary.ForObject resultObject,
                             TimeSeries ts) throws
                                            Exception
    {
        m_forObject = resultObject;

        SamplesContext<T, TS> samplesContext = prepareSampleUpdateContext();

        if (m_forObject.sampleData != null)
        {
            m_logger.debugVerbose("   processSamples: %s (%d samples)", m_forObject.identifier, m_forObject.sampleData.records.length);

            for (StagedResultsSummary.UpdateData ud : m_forObject.sampleData.get(summaryForPending))
            {
                if (!samplesContext.processSampleUpdate(m_logger, null, m_forAsset, m_forObject, ud.timestampEpochSeconds, ud.contents))
                {
                    // Even if there are no values, we create an entry in the time series, to track failures.
                    samplesContext.addValue(null, null, TimeSeries.SampleResolution.Max1Hz, ud.timestampEpochSeconds, 0, null);
                }
            }
        }

        for (SampleValue sv : samplesContext.m_values)
        {
            ts.addSample(sv.resolution, sv.timestampEpochSeconds, sv.prop, sv.sampleType, sv.digitsOfPrecision, sv.value);
        }
    }

    //--//

    protected String getObjectIdentifier()
    {
        return m_forObject.identifier;
    }

    protected DeviceElementRecord ensureObjectRecord(BatchIngestionContext context,
                                                     boolean shouldPersist) throws
                                                                            Exception
    {
        if (m_objectRecord == null)
        {
            ensureAssetRecord(context, false);

            m_objectSummary = m_assetSummary.registerObject(context.summaryForDatabase, m_forObject.identifier, true);
            String sysId = m_objectSummary.getSysId();
            if (sysId != null)
            {
                try
                {
                    DeviceElementRecord rec = context.helper_element.get(sysId);
                    m_objectRecord = context.helper_element.wrapAsExistingRecord(rec);
                }
                catch (Throwable t)
                {
                    // If the device got deleted, recreate it.
                }
            }

            if (m_objectRecord == null)
            {
                T assetRecord = ensureAssetRecord(context, true);
                m_objectRecord = DeviceElementRecord.ensureIdentifier(context.helper_element, assetRecord, m_forObject.identifier);
                context.markInsert(1);
            }
        }

        return shouldPersist ? m_objectRecord.getAfterPersist() : m_objectRecord.get();
    }

    protected abstract void processObjectUpdate(BatchIngestionContext context,
                                                double timestampEpochSeconds,
                                                String contents) throws
                                                                 Exception;

    //--//

    protected abstract SamplesContext<T, TS> prepareSampleUpdateContext();

    //--//

    public static void dumpGenericStatistics(SessionHolder holder,
                                             Logger logger) throws
                                                            Exception
    {
        class ObjectStats
        {
            int    sampleCounter;
            int    counter;
            double firstTimestamp = TimeUtils.maxEpochSeconds();
            double lastTimestamp  = TimeUtils.minEpochSeconds();
        }

        class NetworkStats
        {
            int  batchCount;
            int  sampleCount;
            long size;

            final Map<String, ObjectStats> objects = Maps.newHashMap();
        }

        Map<String, NetworkStats> networks   = Maps.newHashMap();
        Set<Double>               timestamps = Sets.newHashSet();

        final RecordHelper<ResultStagingRecord> helper_staging = holder.createHelper(ResultStagingRecord.class);

        QueryHelperWithCommonFields<ResultStagingRecord, ResultStagingRecord> jh = new QueryHelperWithCommonFields<>(helper_staging, ResultStagingRecord.class);

        QueryHelperWithCommonFields.streamNoNesting(-1, jh, (rec) ->
        {
            for (GatewayDiscoveryEntity en_network : GatewayDiscoveryEntity.filter(rec.getEntities(), GatewayDiscoveryEntitySelector.Network))
            {
                NetworkStats network = networks.computeIfAbsent(en_network.selectorValue, (key) -> new NetworkStats());

                network.batchCount++;
                network.size += rec.getSizeOfEntities();

                recurseStatistics("", en_network, (key, entity) ->
                {
                    ObjectStats object = network.objects.computeIfAbsent(key, (k) -> new ObjectStats());
                    object.counter++;

                    double timestampEpoch = entity.getTimestampEpoch();
                    if (TimeUtils.isValid(timestampEpoch))
                    {
                        timestamps.add(timestampEpoch);

                        if (object.firstTimestamp > timestampEpoch)
                        {
                            object.firstTimestamp = timestampEpoch;
                        }

                        if (object.lastTimestamp < timestampEpoch)
                        {
                            object.lastTimestamp = timestampEpoch;
                        }
                    }

                    if (entity.contents != null)
                    {
                        object.sampleCounter++;
                        network.sampleCount++;
                    }
                });
            }

            return StreamHelperNextAction.Continue_Evict;
        });

        logger.debug("Total timestamps: %d\n", timestamps.size());

        List<String> networksList = Lists.newArrayList(networks.keySet());
        networksList.sort(String::compareTo);

        for (String networkId : networksList)
        {
            NetworkStats network = networks.get(networkId);

            List<String> objects = Lists.newArrayList(network.objects.keySet());
            objects.sort(String::compareTo);

            logger.info("%s # Batches: %4d # Samples: %,10d # Batches Total Size: %,10d # Bytes/sample: %d",
                        networkId,
                        network.batchCount,
                        network.sampleCount,
                        network.size,
                        network.size / Math.max(1, network.sampleCount));

            for (String objectId : objects)
            {
                ObjectStats stats = network.objects.get(objectId);
                if (stats != null && stats.sampleCounter > 1)
                {
                    final ZonedDateTime firstTimestamp = TimeUtils.fromTimestampToUtcTime(stats.firstTimestamp);
                    final ZonedDateTime lastTimestamp  = TimeUtils.fromTimestampToUtcTime(stats.lastTimestamp);
                    logger.info("%s # %-60s # %,8d %,8d # %s # %s",
                                networkId,
                                objectId,
                                stats.sampleCounter,
                                stats.counter,
                                firstTimestamp != null ? TimeUtils.DEFAULT_FORMATTER_NO_MILLI.format(firstTimestamp) : "<none>",
                                lastTimestamp != null ? TimeUtils.DEFAULT_FORMATTER_NO_MILLI.format(lastTimestamp) : "<none>");
                }
            }
        }
    }

    private static void recurseStatistics(String root,
                                          GatewayDiscoveryEntity entity,
                                          BiConsumer<String, GatewayDiscoveryEntity> callback)
    {
        if (entity != null && entity.subEntities != null)
        {
            for (GatewayDiscoveryEntity subEntity : entity.subEntities)
            {
                String subKey = String.format("%s/%s=%s", root, subEntity.selectorKey, subEntity.selectorValue);
                callback.accept(subKey, subEntity);

                recurseStatistics(subKey, subEntity, callback);
            }
        }
    }
}