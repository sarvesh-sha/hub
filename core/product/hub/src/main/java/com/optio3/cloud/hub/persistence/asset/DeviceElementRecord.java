/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.asset;

import static java.util.Objects.requireNonNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.NoResultException;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Tuple;
import javax.persistence.criteria.Predicate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.formatting.TabularField;
import com.optio3.cloud.formatting.TabularReport;
import com.optio3.cloud.formatting.TabularReportAsExcel;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.logic.samples.SamplesCache;
import com.optio3.cloud.hub.model.asset.AssetFilterRequest;
import com.optio3.cloud.hub.model.asset.DeviceElement;
import com.optio3.cloud.hub.model.asset.DeviceElementFilterRequest;
import com.optio3.cloud.hub.model.asset.DeviceElementSampling;
import com.optio3.cloud.hub.model.asset.LogicalAsset;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesEnumeratedValue;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesExtract;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.hub.orchestration.tasks.TaskForPropertyUpdate;
import com.optio3.cloud.hub.persistence.FixupProcessingRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.SortCriteria;
import com.optio3.cloud.persistence.LazyRecordFlusher;
import com.optio3.cloud.persistence.MetadataField;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.MetadataSortExtension;
import com.optio3.cloud.persistence.MetadataTagsMap;
import com.optio3.cloud.persistence.PersistAsJsonHelper;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RawQueryHelper;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.cloud.persistence.StreamHelperResult;
import com.optio3.cloud.search.HibernateIndexingContext;
import com.optio3.cloud.search.HibernateSearch;
import com.optio3.cloud.search.Optio3HibernateSearchContext;
import com.optio3.collection.ExpandableArrayOfDoubles;
import com.optio3.collection.Memoizer;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.ipn.IpnDeviceDescriptor;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.FunctionWithException;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Entity
@Table(name = "ASSET_DEVICE_ELEMENT", indexes = { @Index(columnList = "identifier") })
@DynamicUpdate // Due to HHH-11506
@Indexed()
@Optio3HibernateSearchContext(handler = DeviceElementRecord.DeviceElementIndexingHelper.class)
@Optio3TableInfo(externalId = "DeviceElement", model = DeviceElement.class, metamodel = DeviceElementRecord_.class, metadata = DeviceElementRecord.WellKnownMetadata.class)
public class DeviceElementRecord extends AssetRecord
{
    public static class FixupForContentsMigration extends FixupProcessingRecord.Handler
    {
        @Override
        public Result process(Logger logger,
                              SessionHolder sessionHolder) throws
                                                           Exception
        {
            SessionProvider sessionProvider = sessionHolder.getSessionProvider();

            HubConfiguration cfg = sessionHolder.getServiceNonNull(HubConfiguration.class);
            if (cfg.developerSettings.developerMode)
            {
                // Likely a developer instance, ignore migration.
            }
            else
            {
                // This is going to take a long time, we have a runtime fixup as well, let it run in the background.
                Executors.getDefaultLongRunningThreadPool()
                         .queue(() -> processInBackground(logger, sessionProvider));
            }

            return Result.Done;
        }

        private void processInBackground(Logger logger,
                                         SessionProvider sessionProvider) throws
                                                                          Exception
        {
            try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
            {
                RecordHelper<DeviceElementRecord> helper  = sessionHolder.createHelper(DeviceElementRecord.class);
                AtomicInteger                     counter = new AtomicInteger();

                AssetRecord.enumerate(helper, true, -1, null, (rec_asset) ->
                {
                    boolean modified = rec_asset.contentsMigrationFixup();
                    if (modified)
                    {
                        rec_asset.dontRefreshUpdatedOn();

                        if (counter.incrementAndGet() % 200 == 0)
                        {
                            if (counter.get() % 5000 == 0)
                            {
                                logger.info("Migrating %d elements...", counter.get());
                            }

                            return StreamHelperNextAction.Continue_Flush_Evict_Commit;
                        }

                        return StreamHelperNextAction.Continue_Flush_Evict;
                    }

                    return StreamHelperNextAction.Continue_Evict;
                });

                logger.info("Migrated %d elements", counter.get());

                helper.queueDefragmentation();

                sessionHolder.commit();
            }
        }
    }

    public static class WellKnownMetadata implements Optio3TableInfo.IMetadataDigest
    {
        public static final MetadataField<JsonNode>      elementState              = new MetadataField<>("elementState", JsonNode.class);
        public static final MetadataField<JsonNode>      elementDesiredState       = new MetadataField<>("elementDesiredState", JsonNode.class);
        public static final MetadataField<ZonedDateTime> elementDesiredStateNeeded = new MetadataField<>("elementDesiredStateNeeded", ZonedDateTime.class);
    }

    public static final String DEFAULT_PROP_NAME = BACnetPropertyIdentifier.present_value.name();

    private static final TypeReference<List<DeviceElementSampling>> s_settingsTypeRef = new TypeReference<List<DeviceElementSampling>>()
    {
    };

    private static final TypeReference<List<SampleRange>> s_rangesTypeRef = new TypeReference<List<SampleRange>>()
    {
    };

    private static class SampleRange
    {
        @Transient
        SampleRange linkToNewer;

        @Transient
        SampleRange linkToOlder;

        public String sysId;

        public double rangeStart;
        public double rangeEnd;

        //
        // To avoid writing the ranges to the DB all the time, we update this field instead of "rangeEnd".
        // When we flush the ranges, we copy the value to "rangeEnd", but not for the newest range, because it's still open.
        //
        @Transient
        public double rangeEndUpdated;

        public int length;

        //--//

        boolean isOldest()
        {
            return linkToOlder == null;
        }

        boolean isNewest()
        {
            return linkToNewer == null;
        }

        void update(DeviceElementSampleRecord rec)
        {
            sysId  = rec.getSysId();
            length = rec.getSizeOfTimeSeries();

            TimeSeries ts = rec.getTimeSeries();

            rangeStart      = ts.getStartTimestamp();
            rangeEndUpdated = ts.getEndTimestamp();
        }

        boolean isTimestampCoveredByStart(ZonedDateTime timestamp)
        {
            return isTimestampCoveredByStart(TimeUtils.fromUtcTimeToTimestamp(timestamp));
        }

        boolean isTimestampCoveredByStart(double timestampEpochSeconds)
        {
            return rangeStart <= timestampEpochSeconds;
        }

        boolean isTimestampCoveredByEnd(ZonedDateTime timestamp)
        {
            return isTimestampCoveredByEnd(TimeUtils.fromUtcTimeToTimestamp(timestamp));
        }

        boolean isTimestampCoveredByEnd(double timestampEpochSeconds)
        {
            // The newest range is always open, so it has no end.
            return isNewest() || timestampEpochSeconds <= rangeEnd;
        }

        @Override
        public String toString()
        {
            ZonedDateTime timeStart = TimeUtils.fromTimestampToUtcTime(rangeStart);
            ZonedDateTime timeEnd   = TimeUtils.fromTimestampToUtcTime(rangeEnd);

            return "SampleRange{sysId='" + sysId + '\'' + ", rangeStart=" + timeStart + ", rangeEnd=" + timeEnd + ", length=" + length + '}';
        }
    }

    public static class ArchiveSummary
    {
        public static class Range
        {
            public final String        sysId_element;
            public final String        sysId_archive;
            public final ZonedDateTime rangeStart;
            public final ZonedDateTime rangeEnd;

            private Range(String sysId_element,
                          String sysId_archive,
                          ZonedDateTime rangeStart,
                          ZonedDateTime rangeEnd)
            {
                this.sysId_element = sysId_element;
                this.sysId_archive = sysId_archive;
                this.rangeStart    = rangeStart;
                this.rangeEnd      = rangeEnd;
            }

            public boolean isTimestampCoveredByStart(ZonedDateTime timestamp)
            {
                return !rangeStart.isAfter(timestamp);
            }

            public boolean isTimestampCoveredByEnd(ZonedDateTime timestamp)
            {
                return rangeEnd == null || !timestamp.isAfter(rangeEnd);
            }
        }

        private final Map<String, TimeSeriesPropertyType> m_properties;
        private final boolean                             m_handlePresentationType;
        private final List<Range>                         m_rangesInForwardTimeOrder;
        private       TimeSeriesExtract.ConversionContext m_lastCtx;

        private ArchiveSummary(Map<String, TimeSeriesPropertyType> properties,
                               boolean handlePresentationType,
                               List<Range> rangesInForwardTimeOrder)
        {
            m_properties               = properties;
            m_handlePresentationType   = handlePresentationType;
            m_rangesInForwardTimeOrder = Collections.unmodifiableList(rangesInForwardTimeOrder);
        }

        public Iterator<Range> getIterator(boolean reverseOrder)
        {
            var lst = reverseOrder ? Lists.reverse(m_rangesInForwardTimeOrder) : m_rangesInForwardTimeOrder;
            return lst.iterator();
        }

        public <T> T convertIfNeeded(String prop,
                                     EngineeringUnitsFactors convertTo,
                                     Class<T> clz,
                                     T val)
        {
            TimeSeriesExtract.ConversionContext ctx = extractConversionContext(prop, convertTo);
            if (ctx.propType != null)
            {
                if (ctx.shouldProcess() && val instanceof Number)
                {
                    Number num = (Number) val;

                    double convertedNum = ctx.process(num.doubleValue());

                    val = Reflection.coerceNumber(convertedNum, clz);
                }

                if (m_handlePresentationType && ctx.propType.values != null)
                {
                    try
                    {
                        int valInt = Reflection.coerceNumber(val, Integer.class);

                        for (TimeSeriesEnumeratedValue enumeratedValue : ctx.propType.values)
                        {
                            if (enumeratedValue.typedValue != null && enumeratedValue.value == valInt)
                            {
                                val = clz.cast(enumeratedValue.typedValue);
                                break;
                            }
                        }
                    }
                    catch (Throwable t)
                    {
                        // Just in case the conversion is incompatible.
                    }
                }
            }

            return val;
        }

        public <T> void convertIfNeeded(String prop,
                                        EngineeringUnitsFactors convertTo,
                                        TimeSeriesExtract<T> extract)
        {
            TimeSeriesExtract.ConversionContext ctx = extractConversionContext(prop, convertTo);
            extract.convertIfNeeded(ctx);
        }

        private TimeSeriesExtract.ConversionContext extractConversionContext(String prop,
                                                                             EngineeringUnitsFactors convertTo)
        {
            if (m_lastCtx != null)
            {
                if (m_lastCtx.propType.name.equals(prop) && EngineeringUnitsFactors.areIdentical(m_lastCtx.convertTo, convertTo))
                {
                    return m_lastCtx;
                }
            }

            TimeSeriesExtract.ConversionContext ctx = new TimeSeriesExtract.ConversionContext(m_properties.get(prop), convertTo);
            m_lastCtx = ctx;

            return ctx;
        }
    }

    public class ArchiveDescriptor implements AutoCloseable
    {
        private final RecordHelper<DeviceElementSampleRecord>      m_helper;
        private final SampleRange                                  m_range;
        private       TimeSeries                                   m_timeSeries;
        private       LazyRecordFlusher<DeviceElementSampleRecord> m_flusher;
        private       boolean                                      m_removed;

        ArchiveDescriptor(RecordHelper<DeviceElementSampleRecord> helper,
                          SampleRange range)
        {
            m_helper = helper;
            m_range  = range;
        }

        @Override
        public void close() throws
                            Exception
        {
            if (m_timeSeries == null)
            {
                // Nothing to do.
                return;
            }

            boolean isNew = isNew();
            if (isNew)
            {
                if (m_timeSeries.numberOfSamples() == 0)
                {
                    // Don't flush a new empty archive!
                    m_flusher = null;
                    return;
                }
            }

            LazyRecordFlusher<DeviceElementSampleRecord> flusher = ensureFlusher();
            if (flusher == null)
            {
                return;
            }

            // Compress identical values claser than X minutes.
            m_timeSeries.removeCloseIdenticalValues(MAX_TIME_SEPARATION);

            //
            // Flush the time series, *before* ensuring there's a record.
            // This way we write to memory and later to the database.
            //
            DeviceElementSampleRecord recBeforePersist = flusher.get();
            boolean                   modified         = recBeforePersist.setTimeSeries(m_timeSeries);

            //
            // Persist and update the sample range with actual sysId, length and start/end times.
            //
            DeviceElementSampleRecord recAfterPersist        = flusher.getAfterPersist();
            boolean                   invalidateSamplesCache = false;

            if (isNew || modified)
            {
                invalidateSamplesCache = true;

                m_range.update(recAfterPersist);

                m_helper.flushAndEvict(recAfterPersist);
            }
            else
            {
                m_helper.evict(recAfterPersist);
            }

            m_flusher = null;

            if (isNew)
            {
                //
                // Because we don't writeback the rangeEnd for the newest range,
                // we can't rely on its value to update the chain.
                // Reload the chain from the database.
                //
                invalidateSampleRanges();
            }

            encodeSampleRanges(m_helper, invalidateSamplesCache);

            m_timeSeries = null;
        }

        boolean isIncluded(double timestampEpochSeconds) throws
                                                         Exception
        {
            TimeSeries ts = getTimeSeries();

            if (timestampEpochSeconds < ts.getStartTimestamp())
            {
                //
                // If we are the oldest time series, allow adding to us.
                //
                return m_range.isOldest();
            }

            if (timestampEpochSeconds <= ts.getEndTimestamp())
            {
                return true;
            }

            if (m_range.isNewest())
            {
                // We are the most recent time series, allow adding to us.
                return true;
            }

            if (timestampEpochSeconds < m_range.linkToNewer.rangeStart)
            {
                //
                // The timestamp is between the end of this time series and the beginning of the next one.
                // Allow adding to this time series.
                //
                return true;
            }

            return false;
        }

        public ZonedDateTime getRangeStart()
        {
            return TimeUtils.fromTimestampToUtcTime(m_range.rangeStart);
        }

        public ZonedDateTime getRangeEnd()
        {
            return TimeUtils.fromTimestampToUtcTime(m_range.rangeEnd);
        }

        public boolean isKnownSize()
        {
            return TimeSeries.isKnownSize(m_range.length);
        }

        public boolean isNewest()
        {
            return m_range.isNewest();
        }

        public boolean isNew()
        {
            return m_range.sysId == null;
        }

        public TimeSeries getTimeSeries()
        {
            if (m_timeSeries == null)
            {
                if (isNew())
                {
                    m_timeSeries = TimeSeries.newInstance();
                }
                else
                {
                    m_timeSeries = getRecord().getTimeSeries();
                }
            }

            return m_timeSeries;
        }

        public DeviceElementSampleRecord getRecord()
        {
            LazyRecordFlusher<DeviceElementSampleRecord> flusher = ensureFlusher();
            if (flusher == null)
            {
                throw new RuntimeException("Sample range was deleted");
            }

            return flusher.get();
        }

        void remove()
        {
            LazyRecordFlusher<DeviceElementSampleRecord> flusher = ensureFlusher();
            if (flusher != null)
            {
                DeviceElementSampleRecord rec = flusher.get();
                rec.unloadTimeSeries();
                m_helper.delete(rec);
                m_flusher = null;
                m_removed = true;
            }

            invalidateSampleRanges();
        }

        private LazyRecordFlusher<DeviceElementSampleRecord> ensureFlusher()
        {
            if (m_flusher == null && !m_removed)
            {
                if (isNew())
                {
                    m_flusher = m_helper.wrapAsNewRecord(DeviceElementSampleRecord.newInstance(DeviceElementRecord.this));
                }
                else
                {
                    DeviceElementSampleRecord rec_sample = m_helper.getOrNull(m_range.sysId);
                    if (rec_sample == null)
                    {
                        return null;
                    }

                    m_flusher = m_helper.wrapAsExistingRecord(rec_sample);
                }
            }

            return m_flusher;
        }
    }

    public class ArchiveHolder implements AutoCloseable
    {
        private final RecordHelper<DeviceElementSampleRecord> m_helper;

        private ArchiveDescriptor m_last_archive;
        private int               m_numberOfArchivesUsed;
        private boolean           m_shouldCompact;

        private ArchiveHolder(RecordHelper<DeviceElementSampleRecord> helper)
        {
            m_helper = helper;
        }

        @Override
        public void close() throws
                            Exception
        {
            flush();
        }

        public int getNumberOfArchivesUsed()
        {
            return m_numberOfArchivesUsed;
        }

        public void deleteSamplesOlderThan(ZonedDateTime purgeThreshold) throws
                                                                         Exception
        {
            DeviceElementRecord.this.deleteSamplesOlderThan(m_helper, purgeThreshold);
        }

        public TimeSeries getTimeSeries(double timestampEpochSeconds) throws
                                                                      Exception
        {
            if (m_last_archive != null && !m_last_archive.isIncluded(timestampEpochSeconds))
            {
                flush();
            }

            if (m_last_archive == null)
            {
                m_last_archive = ensureArchive(m_helper, timestampEpochSeconds);
                m_numberOfArchivesUsed++;
            }

            TimeSeries ts = m_last_archive.getTimeSeries();

            m_shouldCompact |= ts.shouldUpgrade();

            //
            // To avoid creating an archive with an unbound number of samples, thanks to compression, let's check the size.
            // If too big, force a new archive. But *only* if the target timestamp is after the end of the current archive.
            //
            if (m_last_archive.isNewest() && ts.hasTooManySamples() && ts.getEndTimestamp() < timestampEpochSeconds)
            {
                m_last_archive.close();

                m_last_archive = new ArchiveDescriptor(m_helper, new SampleRange());
                ts             = m_last_archive.getTimeSeries();
            }

            return ts;
        }

        public void compactIfNeeded() throws
                                      Exception
        {
            flush();

            SessionHolder sessionHolder = m_helper.currentSessionHolder();

            if (m_shouldCompact)
            {
                compactTimeSeries(sessionHolder);
                m_shouldCompact = false;
            }
            else
            {
                compactTimeSeriesIfNeeded(sessionHolder);
            }
        }

        private void flush() throws
                             Exception
        {
            if (m_last_archive != null)
            {
                m_last_archive.close();

                m_last_archive = null;
            }
        }
    }

    //--//

    public static class ArchiveCompactor implements AutoCloseable
    {
        public static final Logger LoggerInstance = new Logger(ArchiveCompactor.class);

        private final RecordHelper<DeviceElementSampleRecord> m_helper;
        private final DeviceElementRecord                     m_rec;
        private final List<DeviceElementSampleRecord>         m_existingTimeSeries  = Lists.newArrayList();
        private final List<TimeSeries>                        m_compactedTimeSeries = Lists.newArrayList();
        private       TimeSeries.SizeEstimator                m_estimator;
        private       int                                     m_count;

        ArchiveCompactor(RecordHelper<DeviceElementSampleRecord> helper,
                         DeviceElementRecord rec)
        {
            m_helper = helper;
            m_rec    = rec;
        }

        @Override
        public void close()
        {
            if (m_estimator != null)
            {
                m_estimator.close();
                m_estimator = null;
            }
        }

        private void execute() throws
                               Exception
        {
            load();

            convert();

            persist();

            cleanup();
        }

        private void load() throws
                            Exception
        {
            for (SampleRange sampleRange = m_rec.ensureSampleRanges(m_helper, false); sampleRange != null; sampleRange = sampleRange.linkToNewer)
            {
                DeviceElementSampleRecord rec = m_helper.getOrNull(sampleRange.sysId);
                if (rec != null)
                {
                    m_existingTimeSeries.add(rec);
                }
            }
        }

        private void convert()
        {
            LoggerInstance.debugVerbose("Convert %s: %d archives", m_rec.getSysId(), m_existingTimeSeries.size());

            boolean isSampled                = m_rec.isSampled();
            double  thresholdForStaleSamples = TimeUtils.fromUtcTimeToTimestamp(TimeUtils.past(3 * 30, TimeUnit.DAYS));

            for (DeviceElementSampleRecord rec : m_existingTimeSeries)
            {
                TimeSeries ts_src = rec.getTimeSeries();

                if (ts_src.numberOfSamples() > 0)
                {
                    int numberOfSamples = ts_src.numberOfSamples();

                    if (!isSampled && ts_src.getEndTimestamp() < thresholdForStaleSamples)
                    {
                        // Drop whole archive if the last sample is older than the threshold for stale samples.
                        continue;
                    }

                    TimeSeries.Encoded tseSrc                = ts_src.encode();
                    int                needed                = tseSrc.getUnpaddedLength();
                    float              averageBytesPerSample = ((float) needed) / numberOfSamples;

                    LoggerInstance.debugVerbose("  Needed: %d (%f)", needed, averageBytesPerSample);

                    int lastCommittedPosition = 0;

                    while (lastCommittedPosition < numberOfSamples)
                    {
                        if (m_estimator == null)
                        {
                            m_estimator = new TimeSeries.SizeEstimator(TimeSeries.newInstance(), averageBytesPerSample);
                        }

                        LoggerInstance.debugObnoxious("  Checking at %d / %d", lastCommittedPosition, numberOfSamples);
                        int estimateSpareSamples = m_estimator.estimateSpareSamples();
                        if (estimateSpareSamples <= 0)
                        {
                            flushEstimator();
                            continue;
                        }

                        int checkpoint = Math.min(lastCommittedPosition + estimateSpareSamples, numberOfSamples);

                        ts_src.copySnapshots(lastCommittedPosition, checkpoint, m_estimator.timeSeries);

                        // Compress identical values claser than X minutes.
                        m_estimator.timeSeries.removeCloseIdenticalValues(MAX_TIME_SEPARATION);

                        if (m_estimator.wouldFit())
                        {
                            lastCommittedPosition = checkpoint;
                        }
                        else
                        {
                            LoggerInstance.debugVerbose("  Rollback from %d back to %d", checkpoint, lastCommittedPosition);
                        }
                    }
                }
            }

            flushEstimator();
        }

        private void persist()
        {
            if (LoggerInstance.isEnabled(Severity.Debug))
            {
                Set<Double> after = Sets.newHashSet();

                for (TimeSeries ts : m_compactedTimeSeries)
                {
                    ExpandableArrayOfDoubles timestamps    = ts.getTimeStampsAsEpochSeconds();
                    int                      numTimestamps = timestamps.size();

                    for (int index = 0; index < numTimestamps; index++)
                    {
                        double timeStampsAsEpochSecond = timestamps.get(index, Double.NaN);

                        if (!after.add(timeStampsAsEpochSecond))
                        {
                            LoggerInstance.debug("%s: %,f - %,f: duplicate-after %,f", m_rec.getSysId(), ts.getStartTimestamp(), ts.getEndTimestamp(), timeStampsAsEpochSecond);
                        }
                    }
                }

                Set<Double> before = Sets.newHashSet();

                for (DeviceElementSampleRecord rec : m_existingTimeSeries)
                {
                    TimeSeries ts = rec.getTimeSeries();

                    ExpandableArrayOfDoubles timestamps    = ts.getTimeStampsAsEpochSeconds();
                    int                      numTimestamps = timestamps.size();

                    for (int index = 0; index < numTimestamps; index++)
                    {
                        double timeStampsAsEpochSecond = timestamps.get(index, Double.NaN);

                        if (!after.contains(timeStampsAsEpochSecond))
                        {
                            LoggerInstance.debug("%s: %,f - %,f: missing %,f", m_rec.getSysId(), ts.getStartTimestamp(), ts.getEndTimestamp(), timeStampsAsEpochSecond);
                        }

                        if (!before.add(timeStampsAsEpochSecond))
                        {
                            LoggerInstance.debug("%s: %,f - %,f: duplicate-before %,f", m_rec.getSysId(), ts.getStartTimestamp(), ts.getEndTimestamp(), timeStampsAsEpochSecond);
                        }
                    }
                }
            }

            for (int i = 0; i < m_compactedTimeSeries.size(); i++)
            {
                TimeSeries ts = m_compactedTimeSeries.get(i);

                if (i < m_existingTimeSeries.size())
                {
                    DeviceElementSampleRecord rec = m_existingTimeSeries.get(i);
                    rec.setTimeSeries(ts);

                    LoggerInstance.debugVerbose("  Reusing archive record %s", rec.getSysId());
                }
                else
                {
                    DeviceElementSampleRecord rec = DeviceElementSampleRecord.newInstance(m_rec);
                    rec.setTimeSeries(ts);
                    m_helper.persist(rec);

                    LoggerInstance.debugVerbose("  New archive record %s", rec.getSysId());

                    m_existingTimeSeries.add(rec);
                }
            }

            while (m_compactedTimeSeries.size() < m_existingTimeSeries.size())
            {
                DeviceElementSampleRecord rec = m_existingTimeSeries.remove(m_existingTimeSeries.size() - 1);
                LoggerInstance.debugVerbose("  Delete archive record %s", rec.getSysId());

                m_helper.delete(rec);
            }
        }

        private void cleanup() throws
                               Exception
        {
            m_rec.invalidateSampleRanges();

            // Rebuild the ranges.
            m_rec.ensureSampleRanges(m_helper, true);
            m_helper.flush();

            for (DeviceElementSampleRecord rec : m_existingTimeSeries)
            {
                rec.unloadTimeSeries();
                m_helper.evict(rec);
            }
        }

        //--//

        private void flushEstimator()
        {
            if (m_estimator != null)
            {
                TimeSeries.Encoded tseSrc = m_estimator.timeSeries.encode();
                if (tseSrc != null)
                {
                    LoggerInstance.debugVerbose("  Flushed %d", tseSrc.getUnpaddedLength());

                    m_compactedTimeSeries.add(m_estimator.timeSeries);
                    m_count++;
                }

                m_estimator = null;
            }
        }
    }

    public int compactTimeSeriesIfNeeded(SessionHolder sessionHolder) throws
                                                                      Exception
    {
        if (isSampled()) // Unsampled elements get compacted all the time, to drop old stale samples.
        {
            SamplesCache.StreamNextAction result = filterArchives(sessionHolder.createHelper(DeviceElementSampleRecord.class), null, null, false, (desc) ->
            {
                if (!desc.isKnownSize())
                {
                    return SamplesCache.StreamNextAction.Exit;
                }

                return SamplesCache.StreamNextAction.Continue;
            }, null);

            if (result != SamplesCache.StreamNextAction.Exit)
            {
                return 0;
            }
        }

        return compactTimeSeries(sessionHolder);
    }

    public int compactTimeSeries(SessionHolder sessionHolder) throws
                                                              Exception
    {
        try (var state = new ArchiveCompactor(sessionHolder.createHelper(DeviceElementSampleRecord.class), this))
        {
            state.execute();

            return state.m_count;
        }
    }

    public static void compactAllTimeSeries(SessionProvider sessionProvider,
                                            boolean force) throws
                                                           Exception
    {
        final int batchSize = 100;

        class State
        {
            private int            elements;
            private int            archives;
            private int            elementsPrevious;
            private int            archivesPrevious;
            private MonotonousTime nextProgressReport;
            private MonotonousTime lastProgressReport;

            void reportProgress()
            {
                if (TimeUtils.isTimeoutExpired(nextProgressReport))
                {
                    MonotonousTime now = MonotonousTime.now();

                    if (lastProgressReport != null)
                    {
                        long timeDiff = lastProgressReport.between(now)
                                                          .toSeconds();

                        int elementsRate = (int) ((elements - elementsPrevious) / timeDiff);
                        int archivesRate = (int) ((archives - archivesPrevious) / timeDiff);
                        ArchiveCompactor.LoggerInstance.info("Compacting TimeSeries... %,d elements (%,d/sec), %,d archives (%,d/sec)", elements, elementsRate, archives, archivesRate);

                        elementsPrevious = elements;
                        archivesPrevious = archives;
                    }

                    lastProgressReport = now;
                    nextProgressReport = TimeUtils.computeTimeoutExpiration(30, TimeUnit.SECONDS);
                }
            }
        }

        ArchiveCompactor.LoggerInstance.info("Compacting TimeSeries...");

        List<String> deviceElements;

        try (SessionHolder sessionHolderSub = sessionProvider.newReadOnlySession())
        {
            RecordHelper<DeviceElementRecord> helper = sessionHolderSub.createHelper(DeviceElementRecord.class);

            deviceElements = QueryHelperWithCommonFields.listRaw(helper, null);
        }

        List<List<String>> deviceElementsBatches = Lists.newArrayList();
        List<String>       deviceElementsBatch   = null;

        for (String sysId : deviceElements)
        {
            if (deviceElementsBatch == null || deviceElementsBatch.size() >= batchSize)
            {
                deviceElementsBatch = Lists.newArrayList();
                deviceElementsBatches.add(deviceElementsBatch);
            }

            deviceElementsBatch.add(sysId);
        }

        ArchiveCompactor.LoggerInstance.info("Processing %,d elements...", deviceElements.size());

        State state = new State();

        CollectionUtils.transformInParallel(deviceElementsBatches, HubApplication.GlobalRateLimiter, (batch) ->
        {
            try (SessionHolder sessionHolderSub = sessionProvider.newSessionWithTransaction())
            {
                RecordHelper<DeviceElementRecord> helper_de = sessionHolderSub.createHelper(DeviceElementRecord.class);

                for (String sysId : batch)
                {
                    DeviceElementRecord rec_object = helper_de.getOrNull(sysId);
                    if (rec_object != null)
                    {
                        int archives;

                        try
                        {
                            if (force)
                            {
                                archives = rec_object.compactTimeSeries(sessionHolderSub);
                            }
                            else
                            {
                                archives = rec_object.compactTimeSeriesIfNeeded(sessionHolderSub);
                            }
                        }
                        catch (Throwable t)
                        {
                            ArchiveCompactor.LoggerInstance.error("Failed compaction of '%s' with %s", sysId, t);
                            throw t;
                        }

                        //
                        // We process batches of 100 elements across multiple threads.
                        // Commit frequently to avoid hitting DB deadlock due to insert lock on pages.
                        //
                        sessionHolderSub.commitAndBeginNewTransactionIfNeeded(5);

                        synchronized (state)
                        {
                            state.archives += archives;
                            state.elements++;
                            state.reportProgress();
                        }
                    }
                }

                sessionHolderSub.commit();
            }

            return batch;
        });

        ArchiveCompactor.LoggerInstance.info("Compacted TimeSeries: %d elements, %d archives", state.elements, state.archives);
    }

    //--//

    public static class DeviceElementIndexingHelper extends HibernateIndexingContext
    {
        public final Map<String, Class<? extends PropertyTypeExtractor>> propertyTypeExtractorLookup = Maps.newHashMap();

        @Override
        public void initialize(AbstractApplicationWithDatabase<?> app,
                               String databaseId,
                               Memoizer memoizer)
        {
            try (SessionHolder sessionHolder = SessionHolder.createWithNewReadOnlySession(app, databaseId, Optio3DbRateLimiter.Normal))
            {
                for (Class<?> entityClass : app.getDataSourceEntities(databaseId))
                {
                    if (!Reflection.isAbstractClass(entityClass) && Reflection.isSubclassOf(AssetRecord.class, entityClass))
                    {
                        AssetRecord                           asset    = (AssetRecord) Reflection.newInstance(entityClass);
                        IProviderOfPropertyTypeExtractorClass provider = Reflection.as(asset, IProviderOfPropertyTypeExtractorClass.class);
                        if (provider != null)
                        {
                            processClass(sessionHolder, provider.getPropertyTypeExtractorClass(), asset.getClass());
                        }
                    }
                }
            }
        }

        private <T extends AssetRecord> void processClass(SessionHolder sessionHolder,
                                                          Class<? extends PropertyTypeExtractor> propertyTypeExtractorClass,
                                                          Class<T> entityClass)
        {
            RawQueryHelper<T, LogicalAsset> qh = new RawQueryHelper<>(sessionHolder, entityClass);

            qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);

            // Reuse the same instance, since we don't store the individual models.
            final var singletonModel = new LogicalAsset();

            qh.stream(() -> singletonModel, (model) ->
            {
                propertyTypeExtractorLookup.put(model.sysId, propertyTypeExtractorClass);
            });
        }
    }

    //--//

    public static final int MAX_TIME_SEPARATION = 5 * 60;

    //--//

    @Column(name = "identifier", nullable = false)
    @Field
    private String identifier;

    @Lob
    @Column(name = "contents")
    private String contents;

    @Lob
    @Column(name = "sampling_settings")
    private String samplingSettings;

    @Transient
    private final PersistAsJsonHelper<String, List<DeviceElementSampling>> m_samplingSettingsHelper = new PersistAsJsonHelper<>(() -> samplingSettings,
                                                                                                                                (val) -> samplingSettings = val,
                                                                                                                                String.class,
                                                                                                                                s_settingsTypeRef,
                                                                                                                                ObjectMappers.SkipNulls);

    @Lob
    @Column(name = "binary_ranges")
    private byte[] binaryRanges;

    //--//

    @Transient
    private boolean m_rangesParsed;

    @Transient
    private SampleRange m_rangesOldest;

    @Transient
    private SampleRange m_rangesNewest;

    //--//

    public DeviceElementRecord()
    {
    }

    //--//

    public String getIdentifier()
    {
        return identifier;
    }

    public void setIdentifier(String identifier)
    {
        this.identifier = identifier;
    }

    //--//

    boolean contentsMigrationFixup() throws
                                     JsonProcessingException
    {
        if (contents != null)
        {
            JsonNode node = ObjectMappers.SkipNulls.readTree(contents);
            putMetadata(WellKnownMetadata.elementState, node);

            contents = null;
            return true;
        }

        return false;
    }

    public boolean hasContents()
    {
        return contents != null || hasMetadata(WellKnownMetadata.elementState);
    }

    public JsonNode getContents() throws
                                  IOException
    {
        contentsMigrationFixup();

        return getMetadata(WellKnownMetadata.elementState);
    }

    public <T extends BaseObjectModel> T getTypedContents(ObjectMapper om,
                                                          Class<T> clz) throws
                                                                        IOException
    {
        JsonNode node = getContents();

        return node != null ? om.treeToValue(node, clz) : null;
    }

    public boolean setContents(ObjectMapper om,
                               Object value) throws
                                             Exception
    {
        JsonNode newNode = value != null ? om.valueToTree(value) : null;

        return putMetadata(WellKnownMetadata.elementState, newNode);
    }

    public JsonNode getDesiredContents() throws
                                         IOException
    {
        return getMetadata(WellKnownMetadata.elementDesiredState);
    }

    public <T extends BaseObjectModel> T getTypedDesiredContents(ObjectMapper om,
                                                                 Class<T> clz) throws
                                                                               IOException
    {
        JsonNode node = getMetadata(WellKnownMetadata.elementDesiredState);
        return node != null ? om.treeToValue(node, clz) : null;
    }

    public boolean setDesiredContents(SessionHolder sessionHolder,
                                      ObjectMapper om,
                                      Object value) throws
                                                    Exception
    {
        JsonNode newNode = value != null ? om.valueToTree(value) : null;

        NetworkAssetRecord rec_network = findParentAssetRecursively(NetworkAssetRecord.class);
        if (rec_network != null)
        {
            if (!rec_network.hasMetadata(WellKnownMetadata.elementDesiredStateNeeded))
            {
                rec_network.putMetadata(WellKnownMetadata.elementDesiredStateNeeded, TimeUtils.now());
            }

            TaskForPropertyUpdate.scheduleTaskIfNotRunning(sessionHolder, rec_network);
        }

        return modifyMetadata(map ->
                              {
                                  WellKnownMetadata.elementDesiredState.put(map, newNode);

                                  if (newNode != null)
                                  {
                                      WellKnownMetadata.elementDesiredStateNeeded.put(map, TimeUtils.now());
                                  }
                              });
    }

    public BaseObjectModel getContentsAsObject(boolean desiredState) throws
                                                                     IOException
    {
        AssetRecord.PropertyTypeExtractor extractor = getPropertyTypeExtractor();

        return extractor.getContentsAsObject(this, desiredState);
    }

    public boolean isAbleToUpdateState() throws
                                         IOException
    {
        BaseObjectModel obj = getContentsAsObject(false);

        return obj != null && obj.isAbleToUpdateState(getIdentifier());
    }

    //--//

    @Field(analyze = Analyze.NO)
    public boolean isSampled()
    {
        return samplingSettings != null;
    }

    @Field(analyze = Analyze.NO)
    public boolean isClassified()
    {
        MetadataTagsMap tags = accessTags();
        return tags.hasTag(WellKnownTags.pointClassId);
    }

    //--//

    private PropertyTypeExtractor getPropertyTypeExtractorForSearch()
    {
        AssetIndexingHelper         helperAsset   = HibernateSearch.IndexingContext.get(AssetIndexingHelper.class);
        DeviceElementIndexingHelper helperElement = HibernateSearch.IndexingContext.get(DeviceElementIndexingHelper.class);

        PropertyTypeExtractor extractor = null;

        if (helperAsset != null && helperElement != null)
        {
            String parentSysId = helperAsset.structuralParentLookup.get(getSysId());
            if (parentSysId != null)
            {
                Class<? extends PropertyTypeExtractor> clz = helperElement.propertyTypeExtractorLookup.get(parentSysId);
                if (clz != null)
                {
                    extractor = Reflection.newInstance(clz);
                }
            }
        }
        else
        {
            extractor = getPropertyTypeExtractor();
        }

        return extractor;
    }

    @Field
    public String getUnitsForSearch()
    {
        PropertyTypeExtractor extractor = getPropertyTypeExtractorForSearch();
        if (extractor == null)
        {
            return null;
        }

        EngineeringUnitsFactors unitsFactors = extractor.getUnitsFactors(this);
        return unitsFactors != null ? unitsFactors.toIndexingString() : null;
    }

    @Field
    public String getParentProtocolIdentifierForSearch()
    {
        BaseAssetDescriptor desc = null;

        AssetIndexingHelper               helperAsset  = HibernateSearch.IndexingContext.get(AssetIndexingHelper.class);
        DeviceRecord.DeviceIndexingHelper helperDevice = HibernateSearch.IndexingContext.get(DeviceRecord.DeviceIndexingHelper.class);
        if (helperAsset != null && helperDevice != null)
        {
            String parentSysId = helperAsset.structuralParentLookup.get(getSysId());
            if (parentSysId != null)
            {
                desc = helperDevice.identityDescriptorLookup.get(parentSysId);
            }
        }
        else
        {
            AssetRecord rec_parent = getParentAsset();
            if (rec_parent != null)
            {
                desc = rec_parent.getIdentityDescriptor();
            }
        }

        if (desc != null)
        {
            BACnetDeviceDescriptor desc_BACnet = Reflection.as(desc, BACnetDeviceDescriptor.class);
            if (desc_BACnet != null)
            {
                return Integer.toString(desc_BACnet.address.instanceNumber);
            }

            IpnDeviceDescriptor desc_ipn = Reflection.as(desc, IpnDeviceDescriptor.class);
            if (desc_ipn != null)
            {
                return desc_ipn.name;
            }
        }

        return "";
    }

    @Field
    public String getParentEquipmentNameForSearch()
    {
        AssetIndexingHelper helper = HibernateSearch.IndexingContext.get(AssetIndexingHelper.class);
        if (helper != null)
        {
            StringBuilder sb = null;

            String sysId = getSysId();
            while (sysId != null)
            {
                for (String logicalParentSysId = helper.controlsParentLookup.get(sysId); logicalParentSysId != null; logicalParentSysId = helper.controlsParentLookup.get(logicalParentSysId))
                {
                    sb = collectEquipmentName(helper, sb, logicalParentSysId);
                }

                String parentSysId = helper.structuralParentLookup.get(sysId);
                if (parentSysId == null)
                {
                    break;
                }

                sb = collectEquipmentName(helper, sb, parentSysId);

                sysId = parentSysId;
            }

            return sb != null ? sb.toString() : null;
        }

        return StringUtils.join(getMetadata(AssetRecord.WellKnownMetadata.parentEquipmentName), " ");
    }

    private static StringBuilder collectEquipmentName(AssetIndexingHelper helper,
                                                      StringBuilder sb,
                                                      String sysId)
    {
        if (helper.isEquipment.contains(sysId))
        {
            String name = helper.nameLookup.get(sysId);
            if (name != null)
            {
                if (sb == null)
                {
                    sb = new StringBuilder();
                }
                else
                {
                    sb.append(" ");
                }

                sb.append(name);
            }
        }

        return sb;
    }

    @Field
    public String getIndexedValues()
    {
        PropertyTypeExtractor extractor = getPropertyTypeExtractorForSearch();
        if (extractor == null)
        {
            return null;
        }

        return extractor.getIndexedValue(this);
    }

    //--//

    public boolean hasSamplingSettings()
    {
        return samplingSettings != null;
    }

    public List<DeviceElementSampling> getSamplingSettings()
    {
        return sanitizeSamplingSettings(m_samplingSettingsHelper.get());
    }

    public boolean setSamplingSettings(List<DeviceElementSampling> list)
    {
        return m_samplingSettingsHelper.set(sanitizeSamplingSettings(list));
    }

    protected List<DeviceElementSampling> sanitizeSamplingSettings(List<DeviceElementSampling> list)
    {
        list = CollectionUtils.asEmptyCollectionIfNull(list);

        // Skip incorrect entries.
        list = CollectionUtils.filter(list, (o) -> o.samplingPeriod > 0);

        return CollectionUtils.isEmpty(list) ? null : list;
    }

    //--//

    private SampleRange ensureSampleRanges(RecordHelper<DeviceElementSampleRecord> helper,
                                           boolean fromNewest) throws
                                                               Exception
    {
        requireNonNull(helper);

        if (!m_rangesParsed)
        {
            List<SampleRange> lst;
            boolean           writeBack;

            if (binaryRanges != null)
            {
                lst       = ObjectMappers.deserializeFromGzip(ObjectMappers.SkipNulls, binaryRanges, s_rangesTypeRef);
                writeBack = false;
            }
            else
            {
                lst       = Lists.newArrayList();
                writeBack = true;

                QueryHelperWithCommonFields<Tuple, DeviceElementSampleRecord> jh = new QueryHelperWithCommonFields<>(helper, Tuple.class);

                jh.cq.multiselect(jh.root.get(RecordWithCommonFields_.sysId), jh.root.get(DeviceElementSampleRecord_.contents));

                jh.addWhereClauseWithEqual(jh.root, DeviceElementSampleRecord_.owningElement, this);

                jh.setFetchSize(50);

                try (var scroll = jh.scroll(0))
                {
                    while (scroll.next())
                    {
                        Tuple  row      = scroll.get(0);
                        String sysId    = (String) row.get(0);
                        byte[] contents = (byte[]) row.get(1);

                        TimeSeries ts = TimeSeries.decode(contents);
                        if (ts == null || ts.numberOfSamples() == 0)
                        {
                            // Ignore empty archives!!!
                            HubApplication.LoggerInstance.warn("Found an archive with no samples: '%s' for object '%s'", sysId, getSysId());
                            continue;
                        }

                        SampleRange range = new SampleRange();
                        range.sysId      = sysId;
                        range.length     = contents.length;
                        range.rangeStart = ts.getStartTimestamp();
                        range.rangeEnd   = ts.getEndTimestamp();

                        lst.add(range);
                    }
                }
            }

            // Sort in descending start time.
            lst.sort((a, b) -> Double.compare(b.rangeStart, a.rangeStart));

            m_rangesNewest = null;
            m_rangesOldest = null;

            for (SampleRange sampleRange : lst)
            {
                sampleRange.rangeEndUpdated = sampleRange.rangeEnd;

                if (m_rangesNewest == null)
                {
                    m_rangesNewest = sampleRange;
                }

                if (m_rangesOldest != null)
                {
                    m_rangesOldest.linkToOlder = sampleRange;
                }

                sampleRange.linkToNewer = m_rangesOldest;
                m_rangesOldest          = sampleRange;
            }

            m_rangesParsed = true;

            if (writeBack)
            {
                // This call causes a recursion, but it's safe because the ranges have already been parsed.
                encodeSampleRanges(helper, false);
            }
        }

        return fromNewest ? m_rangesNewest : m_rangesOldest;
    }

    private void encodeSampleRanges(RecordHelper<DeviceElementSampleRecord> helper,
                                    boolean invalidateSamplesCache) throws
                                                                    Exception
    {
        List<SampleRange> lst = Lists.newArrayList();

        for (SampleRange ptr = ensureSampleRanges(helper, true); ptr != null; ptr = ptr.linkToOlder)
        {
            //
            // Don't update the rangeEnd for the last range, since it's constantly changing.
            //
            if (!ptr.isNewest())
            {
                ptr.rangeEnd = ptr.rangeEndUpdated;
            }

            lst.add(ptr);
        }

        byte[] newBinaryRanges = ObjectMappers.serializeToGzip(lst);

        if (!Arrays.equals(newBinaryRanges, binaryRanges))
        {
            binaryRanges = newBinaryRanges;

            invalidateSamplesCache = true;
        }

        if (invalidateSamplesCache)
        {
            SamplesCache cache = helper.getServiceNonNull(SamplesCache.class);
            cache.invalidate(this);
        }
    }

    //--//

    public GatewayDiscoveryEntity createRequest(GatewayDiscoveryEntity en_device,
                                                boolean forUpdate)
    {
        AssetRecord parent = getParentAsset();

        if (SessionHolder.isEntityOfClass(parent, BACnetDeviceRecord.class))
        {
            return en_device.createAsRequest(forUpdate ? GatewayDiscoveryEntitySelector.BACnet_ObjectSet : GatewayDiscoveryEntitySelector.BACnet_ObjectConfig, getIdentifier());
        }

        if (SessionHolder.isEntityOfClass(parent, IpnDeviceRecord.class))
        {
            if (forUpdate)
            {
                return null; // Not implemented.
            }

            return en_device.createAsRequest(GatewayDiscoveryEntitySelector.Ipn_ObjectConfig, getIdentifier());
        }

        return null;
    }

    //--//

    private static class DeviceElementJoinHelper<T> extends AssetJoinHelper<T, DeviceElementRecord>
    {
        DeviceElementJoinHelper(RecordHelper<DeviceElementRecord> helper,
                                Class<T> clz)
        {
            super(helper, clz);
        }

        //--//

        void applyFilters(DeviceElementFilterRequest filters)
        {
            super.applyFilters(filters);

            if (filters.hasNoSampling)
            {
                addWhereClause(isNull(root, DeviceElementRecord_.samplingSettings));
            }
            else if (filters.hasAnySampling)
            {
                addWhereClause(isNotNull(root, DeviceElementRecord_.samplingSettings));
            }

            if (filters.sortBy != null)
            {
                for (SortCriteria sort : filters.sortBy)
                {
                    switch (sort.column)
                    {
                        case "identifier":
                        {
                            addOrderBy(root, DeviceElementRecord_.identifier, sort.ascending);
                            break;
                        }

                        case "hasSampling":
                        {
                            addOrderBy(root, DeviceElementRecord_.samplingSettings, sort.ascending);
                            break;
                        }

                        case "description":
                        {
                            addSortExtension(new MetadataSortExtension<String>(root)
                            {
                                @Override
                                protected String extractValue(MetadataMap metadata)
                                {
                                    try
                                    {
                                        JsonNode node = WellKnownMetadata.elementState.get(metadata);
                                        if (node != null)
                                        {
                                            ObjectMapper      objectMapper = BACnetObjectModel.getObjectMapper();
                                            BACnetObjectModel obj          = objectMapper.treeToValue(node, BACnetObjectModel.class);

                                            return (String) obj.getValue(BACnetPropertyIdentifier.description, null);
                                        }
                                    }
                                    catch (Throwable t)
                                    {
                                        // Ignore decoding failures.
                                    }

                                    return "";
                                }

                                @Override
                                protected void sort(List<String> values)
                                {
                                    values.sort((a, b) ->
                                                {
                                                    int diff = StringUtils.compareIgnoreCase(a, b);
                                                    return sort.ascending ? diff : -diff;
                                                });
                                }
                            });

                            break;
                        }
                    }
                }
            }
        }

        @Override
        protected Predicate predicateForLike(List<ParsedLike> likeFilters)
        {
            return or(super.predicateForLike(likeFilters), predicateForLike(root, DeviceElementRecord_.identifier, likeFilters));
        }
    }

    //--//

    public static StreamHelperResult enumerateNoNesting(RecordHelper<DeviceElementRecord> helper,
                                                        DeviceElementFilterRequest filters,
                                                        FunctionWithException<DeviceElementRecord, StreamHelperNextAction> callback) throws
                                                                                                                                     Exception
    {
        final DeviceElementJoinHelper<DeviceElementRecord> jh = new DeviceElementJoinHelper<>(helper, DeviceElementRecord.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return DeviceElementJoinHelper.streamNoNesting(0, jh, callback);
    }

    public static StreamHelperResult enumerate(RecordHelper<DeviceElementRecord> helper,
                                               boolean batchStream,
                                               DeviceElementFilterRequest filters,
                                               FunctionWithException<DeviceElementRecord, StreamHelperNextAction> callback) throws
                                                                                                                            Exception
    {
        final DeviceElementJoinHelper<Tuple> jh = new DeviceElementJoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return DeviceElementJoinHelper.stream(batchStream, jh, callback);
    }

    public static List<RecordIdentity> filterDeviceElements(RecordHelper<DeviceElementRecord> helper,
                                                            DeviceElementFilterRequest filters)
    {
        final DeviceElementJoinHelper<Tuple> jh = new DeviceElementJoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return AssetJoinHelper.returnFilterTuples(helper, jh);
    }

    public static long countDeviceElements(RecordHelper<DeviceElementRecord> helper,
                                           DeviceElementFilterRequest filters)
    {
        final DeviceElementJoinHelper<Tuple> jh = new DeviceElementJoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return jh.count();
    }

    public static LazyRecordFlusher<DeviceElementRecord> ensureIdentifier(RecordHelper<DeviceElementRecord> helper,
                                                                          AssetRecord parent,
                                                                          String identifier)
    {
        DeviceElementRecord rec_element = findByIdentifierOrNull(helper, parent, identifier);
        if (rec_element == null)
        {
            rec_element = new DeviceElementRecord();
            rec_element.setIdentifier(identifier);

            return helper.wrapAsNewRecord(rec_element, (rec_element2) ->
            {
                rec_element2.linkToParent(helper, parent);

                rec_element2.setPropertyTypeExtractorClass(inferPropertyTypeExtractorClass(parent));
            });
        }
        else
        {
            RecordLocked<DeviceElementRecord> lock_element = helper.optimisticallyUpgradeToLocked(rec_element, 10, TimeUnit.SECONDS);
            return helper.wrapAsExistingRecord(lock_element.get());
        }
    }

    public static DeviceElementRecord findByIdentifierOrNull(RecordHelper<DeviceElementRecord> helper,
                                                             AssetRecord parent,
                                                             String identifier) throws
                                                                                NoResultException
    {
        QueryHelperWithCommonFields<DeviceElementRecord, DeviceElementRecord> jh = QueryHelperWithCommonFields.prepareQueryForEntity(helper);

        jh.addWhereClauseWithEqual(jh.root, DeviceElementRecord_.identifier, identifier);

        if (parent != null)
        {
            jh.addWhereClauseWithEqual(jh.root, AssetRecord_.parentAsset, parent);
        }

        return jh.getFirstResultOrNull();
    }

    //--//

    public ArchiveHolder newArchiveHolder(RecordHelper<DeviceElementSampleRecord> helper)
    {
        return new ArchiveHolder(helper);
    }

    public void deleteSamplesOlderThan(RecordHelper<DeviceElementSampleRecord> helper,
                                       ZonedDateTime purgeThreshold) throws
                                                                     Exception
    {
        filterArchives(helper, null, purgeThreshold, false, (desc) ->
        {
            if (purgeThreshold == null || !desc.m_range.isTimestampCoveredByEnd(purgeThreshold))
            {
                desc.remove();
            }

            return null;
        }, null);
    }

    public void invalidateSampleRanges()
    {
        if (binaryRanges != null) // Read before write.
        {
            binaryRanges = null;
        }

        m_rangesParsed = false;
    }

    public SamplesCache.StreamNextAction filterArchives(RecordHelper<DeviceElementSampleRecord> helper,
                                                        ZonedDateTime rangeStart,
                                                        ZonedDateTime rangeEnd,
                                                        boolean fromNewest,
                                                        FunctionWithException<ArchiveDescriptor, SamplesCache.StreamNextAction> filterCallback,
                                                        FunctionWithException<SamplesCache.StreamNextAction, SamplesCache.StreamNextAction> afterEnumerationCallback) throws
                                                                                                                                                                      Exception
    {
        SamplesCache.StreamNextAction result = null;

        for (SampleRange sampleRange = ensureSampleRanges(helper, fromNewest); sampleRange != null; sampleRange = fromNewest ? sampleRange.linkToOlder : sampleRange.linkToNewer)
        {
            if (rangeStart != null && !sampleRange.isTimestampCoveredByEnd(rangeStart))
            {
                continue;
            }

            if (rangeEnd != null && !sampleRange.isTimestampCoveredByStart(rangeEnd))
            {
                continue;
            }

            try (ArchiveDescriptor desc = new ArchiveDescriptor(helper, sampleRange))
            {
                result = filterCallback.apply(desc);

                if (result != SamplesCache.StreamNextAction.Continue)
                {
                    break;
                }
            }
        }

        if (afterEnumerationCallback != null)
        {
            result = afterEnumerationCallback.apply(result);
        }

        // Recheck the ranges, in case they got invalidated.
        ensureSampleRanges(helper, false);

        return result;
    }

    public ArchiveDescriptor ensureArchive(RecordHelper<DeviceElementSampleRecord> helper,
                                           ZonedDateTime time) throws
                                                               Exception
    {
        return ensureArchive(helper, TimeUtils.fromUtcTimeToTimestamp(time));
    }

    public ArchiveDescriptor ensureArchive(RecordHelper<DeviceElementSampleRecord> helper,
                                           double timeEpochSeconds) throws
                                                                    Exception
    {
        return findArchive(helper, timeEpochSeconds, true);
    }

    public ArchiveDescriptor getArchive(RecordHelper<DeviceElementSampleRecord> helper,
                                        ZonedDateTime time) throws
                                                            Exception
    {
        return getArchive(helper, TimeUtils.fromUtcTimeToTimestamp(time));
    }

    public ArchiveDescriptor getArchive(RecordHelper<DeviceElementSampleRecord> helper,
                                        double timeEpochSeconds) throws
                                                                 Exception
    {
        return findArchive(helper, timeEpochSeconds, false);
    }

    private ArchiveDescriptor findArchive(RecordHelper<DeviceElementSampleRecord> helper,
                                          double timeEpochSeconds,
                                          boolean createIfMissing) throws
                                                                   Exception
    {
        for (SampleRange sampleRange = ensureSampleRanges(helper, true); sampleRange != null; sampleRange = sampleRange.linkToOlder)
        {
            if (sampleRange.isTimestampCoveredByStart(timeEpochSeconds))
            {
                //
                // Ranges are sorted in descending start time, which means 'time' is between this range's start and the previous (more recent) range's start.
                //
                return new ArchiveDescriptor(helper, sampleRange);
            }
        }

        if (!createIfMissing)
        {
            return null;
        }

        //
        // If we prepareResult here, the timestamp is older than any range.
        // If we have a range, append samples to it, don't create another range.
        //
        if (m_rangesOldest != null)
        {
            return new ArchiveDescriptor(helper, m_rangesOldest);
        }

        return new ArchiveDescriptor(helper, new SampleRange());
    }

    //--//

    public ArchiveSummary describeArchives(RecordHelper<DeviceElementSampleRecord> helper,
                                           Map<String, TimeSeriesPropertyType> properties,
                                           boolean handlePresentationType) throws
                                                                           Exception
    {
        if (SessionHolder.isEntityOfClass(this, MetricsDeviceElementRecord.class))
        {
            return new ArchiveSummary(properties, handlePresentationType, Collections.emptyList());
        }

        List<ArchiveSummary.Range> ranges = Lists.newArrayList();

        filterArchives(helper, null, null, false, (desc) ->
        {
            ZonedDateTime rangeStart = TimeUtils.fromTimestampToUtcTime(desc.m_range.rangeStart);
            ZonedDateTime rangeEnd   = TimeUtils.fromTimestampToUtcTime(desc.m_range.rangeEnd);

            ranges.add(new ArchiveSummary.Range(getSysId(), desc.m_range.sysId, rangeStart, desc.isNewest() ? null : rangeEnd));
            return SamplesCache.StreamNextAction.Continue;
        }, null);

        return new ArchiveSummary(properties, handlePresentationType, ranges);
    }

    public PropertyTypeExtractor getPropertyTypeExtractor()
    {
        Class<? extends PropertyTypeExtractor> handlerClass = getPropertyTypeExtractorClass();

        if (handlerClass == null)
        {
            handlerClass = inferPropertyTypeExtractorClass(getParentAsset());

            setPropertyTypeExtractorClass(handlerClass);
        }

        return Reflection.newInstance(handlerClass);
    }

    public static Class<? extends PropertyTypeExtractor> inferPropertyTypeExtractorClass(AssetRecord parent)
    {
        Class<? extends PropertyTypeExtractor> clz = null;

        var provider = SessionHolder.asEntityOfClassOrNull(parent, AssetRecord.IProviderOfPropertyTypeExtractorClass.class);
        if (provider != null)
        {
            clz = provider.getPropertyTypeExtractorClass();
        }

        return clz != null ? clz : AssetRecord.PropertyTypeExtractor.None.class;
    }

    public Class<? extends PropertyTypeExtractor> getPropertyTypeExtractorClass()
    {
        try
        {
            String handlerClassName = getMetadata(AssetRecord.WellKnownMetadata.propertyTypeExtractor);
            if (handlerClassName != null)
            {
                @SuppressWarnings("unchecked") Class<? extends PropertyTypeExtractor> clz = (Class<? extends PropertyTypeExtractor>) Class.forName(handlerClassName);
                return clz;
            }
        }
        catch (Throwable t)
        {
            // Just return nothing.
        }

        return null;
    }

    public void setPropertyTypeExtractorClass(Class<? extends PropertyTypeExtractor> handlerClass)
    {
        String handlerClassName = handlerClass.getName();

        if (!StringUtils.equals(handlerClassName, getMetadata(AssetRecord.WellKnownMetadata.propertyTypeExtractor)))
        {
            putMetadata(AssetRecord.WellKnownMetadata.propertyTypeExtractor, handlerClassName);
        }
    }

    //--//

    private static class SampleStatistics
    {
        static class RowForReport
        {
            @TabularField(order = 0, title = "SysId")
            public String col_sysId;

            @TabularField(order = 1, title = "Location")
            public String col_location;

            @TabularField(order = 2, title = "Name")
            public String col_name;

            @TabularField(order = 3, title = "Device ID")
            public String col_deviceId;

            @TabularField(order = 4, title = "Field")
            public String col_field;

            @TabularField(order = 5, title = "Snapshots")
            public int col_snapshots;

            @TabularField(order = 6, title = "Snapshots Per Day")
            public int col_snapshotsPerDay;

            @TabularField(order = 7, title = "First Timestamp", format = "yyyy-MM-dd hh:mm:ss")
            public ZonedDateTime col_firstTimestamp;

            @TabularField(order = 8, title = "Last Timestamp", format = "yyyy-MM-dd hh:mm:ss")
            public ZonedDateTime col_lastTimestamp;

            @TabularField(order = 9, title = "Missed")
            public String col_missed;

            @TabularField(order = 10, title = "Storage Bytes")
            public long col_storage;

            @TabularField(order = 11, title = "Storage Bytes (unpadded)")
            public long col_storageUnpadded;

            @TabularField(order = 12, title = "Bytes per Snapshot")
            public double col_storagePerSnapshot;

            @TabularField(order = 13, title = "Bytes per Day")
            public int col_storagePerDay;

            @TabularField(order = 14, title = "Archives #")
            public int col_archives;

            @TabularField(order = 15, title = "Sampled")
            public int col_sampled;
        }

        class State
        {
            static final int reportFrequency = 30;

            private MonotonousTime nextProgressReport;
            private MonotonousTime lastProgressReport;

            final Set<Double> timestampsSeen    = Sets.newHashSet();
            final AtomicLong  totalObjects      = new AtomicLong();
            final AtomicLong  totalSnapshots    = new AtomicLong();
            final AtomicLong  totalSamples      = new AtomicLong();
            final AtomicLong  totalMissed       = new AtomicLong();
            final AtomicLong  totalSize         = new AtomicLong();
            final AtomicLong  totalSizeUnpadded = new AtomicLong();

            long previous_totalObjects;
            long previous_totalSnapshots;
            long previous_totalSamples;

            synchronized void reportProgress()
            {
                if (TimeUtils.isTimeoutExpired(nextProgressReport))
                {
                    MonotonousTime now = MonotonousTime.now();

                    if (lastProgressReport == null)
                    {
                        logger.info("Progress Report: totalObjects, totalSnapshots, totalSamples, totalMissed, totalSize");
                    }
                    else
                    {
                        long timeDiff = lastProgressReport.between(now)
                                                          .toSeconds();

                        long totalObjects   = this.totalObjects.get();
                        long totalSnapshots = this.totalSnapshots.get();
                        long totalSamples   = this.totalSamples.get();

                        int totalObjectsRate   = (int) ((totalObjects - previous_totalObjects) / timeDiff);
                        int totalSnapshotsRate = (int) ((totalSnapshots - previous_totalSnapshots) / timeDiff);
                        int totalSamplesRate   = (int) ((totalSamples - previous_totalSamples) / timeDiff);

                        logger.info("Progress Report: %,d (%,d/sec) %,d (%,d/sec) %,d (%,d/sec) %,d %,d",
                                    totalObjects,
                                    totalObjectsRate,
                                    totalSnapshots,
                                    totalSnapshotsRate,
                                    totalSamples,
                                    totalSamplesRate,
                                    totalMissed,
                                    totalSize);

                        previous_totalObjects   = totalObjects;
                        previous_totalSnapshots = totalSnapshots;
                        previous_totalSamples   = totalSamples;
                    }

                    lastProgressReport = now;
                    nextProgressReport = TimeUtils.computeTimeoutExpiration(30, TimeUnit.SECONDS);
                }
            }
        }

        final SessionProvider sessionProvider;
        final Logger          logger;
        final boolean         showMissedTimestamps;
        final String          outputFile;
        final boolean         onlyNetworks;
        final boolean         onlyGateways;

        final State state = new State();

        SampleStatistics(SessionProvider sessionProvider,
                         Logger logger,
                         boolean showMissedTimestamps,
                         String outputFile,
                         boolean onlyNetworks,
                         boolean onlyGateways)
        {
            this.sessionProvider      = sessionProvider;
            this.logger               = logger;
            this.showMissedTimestamps = showMissedTimestamps;

            this.outputFile   = outputFile;
            this.onlyNetworks = onlyNetworks;
            this.onlyGateways = onlyGateways;
        }

        void compute() throws
                       Exception
        {
            try (var holder = new TabularReportAsExcel.Holder())
            {
                TabularReportAsExcel<RowForReport> tr = new TabularReportAsExcel<>(RowForReport.class, "Samples", holder);

                List<String> assets;

                try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
                {
                    if (onlyGateways)
                    {
                        RecordHelper<GatewayAssetRecord> helper = sessionHolder.createHelper(GatewayAssetRecord.class);

                        assets = QueryHelperWithCommonFields.listRaw(helper, null);
                    }
                    else if (onlyNetworks)
                    {
                        RecordHelper<NetworkAssetRecord> helper = sessionHolder.createHelper(NetworkAssetRecord.class);

                        assets = QueryHelperWithCommonFields.listRaw(helper, null);
                    }
                    else
                    {
                        RecordHelper<DeviceRecord> helper = sessionHolder.createHelper(DeviceRecord.class);

                        assets = QueryHelperWithCommonFields.listRaw(helper, null);
                    }
                }

                List<List<String>> assetBatches = Lists.newArrayList();
                List<String>       assetBatch   = null;
                final int          batchSize    = 10;

                for (String sysId : assets)
                {
                    if (assetBatch == null || assetBatch.size() >= batchSize)
                    {
                        assetBatch = Lists.newArrayList();
                        assetBatches.add(assetBatch);
                    }

                    assetBatch.add(sysId);
                }

                logger.info("Processing %,d asset groups...", assets.size());

                tr.emit(rowHandler ->
                        {
                            CollectionUtils.transformInParallel(assetBatches, HubApplication.GlobalRateLimiter, (batch) ->
                            {
                                try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
                                {
                                    RecordHelper<AssetRecord> helper_de = sessionHolder.createHelper(AssetRecord.class);

                                    for (String sysId : batch)
                                    {
                                        AssetRecord rec_asset = helper_de.getOrNull(sysId);
                                        if (rec_asset != null)
                                        {
                                            countAsset(rowHandler, sessionHolder, rec_asset);
                                        }
                                    }
                                }

                                return batch;
                            });
                        });

                logger.info("# of Objects           : %,d", state.totalObjects);
                logger.info("# of Snapshots         : %,d", state.totalSnapshots);
                logger.info("# of Samples           : %,d", state.totalSamples);
                logger.info("# of Misses            : %,d", state.totalMissed);
                logger.info("Storage size           : %,d", state.totalSize);
                logger.info("Storage size (unpadded): %,d", state.totalSizeUnpadded);

                try (FileOutputStream outputStream = new FileOutputStream(outputFile))
                {
                    holder.toStream(outputStream);
                }
            }
        }

        private void countAsset(TabularReport<RowForReport>.RowHandler rowHandler,
                                SessionHolder sessionHolder,
                                AssetRecord rec_asset) throws
                                                       Exception
        {
            AssetFilterRequest filters = AssetFilterRequest.createFilterForParent(rec_asset.getSysId());
            AssetRecord.enumerate(sessionHolder.createHelper(DeviceElementRecord.class), true, -1, filters, (rec_object) ->
            {
                countObject(rowHandler, sessionHolder, rec_asset, rec_object);
                return StreamHelperNextAction.Continue_Evict;
            });
        }

        private void countObject(TabularReport<RowForReport>.RowHandler rowHandler,
                                 SessionHolder sessionHolder,
                                 AssetRecord rec_asset,
                                 DeviceElementRecord rec_object) throws
                                                                 Exception
        {
            RecordHelper<DeviceElementSampleRecord> helper_des = sessionHolder.createHelper(DeviceElementSampleRecord.class);

            state.totalObjects.incrementAndGet();

            int                  snapshots       = 0;
            int                  archives        = 0;
            long                 storage         = 0;
            long                 storageUnpadded = 0;
            Map<String, Integer> counts          = Maps.newHashMap();
            double               timestampFirst  = Double.NaN;
            double               timestampLast   = Double.NaN;

            for (SampleRange sampleRange = rec_object.ensureSampleRanges(helper_des, true); sampleRange != null; sampleRange = sampleRange.linkToOlder)
            {
                DeviceElementSampleRecord     rec_sample = helper_des.get(sampleRange.sysId);
                TimeSeries                    ts         = rec_sample.getTimeSeries();
                List<TimeSeries.SampleSchema> schemas    = ts.getSchema();

                ExpandableArrayOfDoubles timestamps    = ts.getTimeStampsAsEpochSeconds();
                int                      numTimestamps = timestamps.size();

                TimeSeries.Encoded tse            = ts.encode();
                int                unpaddedLength = tse != null ? tse.getUnpaddedLength() : 0;
                int                paddedLength   = rec_sample.getSizeOfTimeSeries();

                archives++;
                storage += paddedLength;
                storageUnpadded += unpaddedLength;

                if (numTimestamps > 0)
                {
                    for (int index = 0; index < numTimestamps; index++)
                    {
                        double timestamp = timestamps.get(index, Double.NaN);

                        if (logger.isEnabled(Severity.Debug))
                        {
                            synchronized (state)
                            {
                                if (state.timestampsSeen.add(timestamp))
                                {
                                    logger.debug("Timestamp %d: %s", state.timestampsSeen.size(), TimeUtils.fromTimestampToUtcTime(timestamp));
                                }
                            }
                        }

                        if (Double.isNaN(timestampFirst) || timestampFirst > timestamp)
                        {
                            timestampFirst = timestamp;
                        }

                        if (Double.isNaN(timestampLast) || timestampLast < timestamp)
                        {
                            timestampLast = timestamp;
                        }
                    }

                    state.totalSize.addAndGet(paddedLength);
                    state.totalSizeUnpadded.addAndGet(unpaddedLength);

                    snapshots += numTimestamps;

                    for (TimeSeries.SampleSchema schema : schemas)
                    {
                        int count = 0;

                        if (showMissedTimestamps)
                        {
                            for (int i = 0; i < numTimestamps; i++)
                            {
                                if (schema.hasValue(i))
                                {
                                    count++;
                                    state.totalSamples.incrementAndGet();
                                }
                                else
                                {
                                    logger.info("Missed %s at %s%n", schema.identifier, timestamps.get(i, 0));
                                }
                            }
                        }
                        else
                        {
                            int missed = schema.countMissing();
                            int valid  = numTimestamps - missed;

                            count += valid;
                            state.totalSamples.addAndGet(valid);
                        }

                        Integer countTot = counts.get(schema.identifier);
                        if (countTot == null)
                        {
                            countTot = count;
                        }
                        else
                        {
                            countTot += count;
                        }

                        counts.put(schema.identifier, countTot);
                    }
                }

                helper_des.evict(rec_sample);

                state.reportProgress();
            }

            if (snapshots > 0)
            {
                String id;

                BaseAssetDescriptor desc = rec_asset.getIdentityDescriptor();

                if (desc != null)
                {
                    id = desc.toString();
                }
                else
                {
                    id = String.format("sysId:%s", rec_asset.getSysId());
                }

                LocationRecord rec_loc = rec_object.getLocation();

                double diff = timestampLast - timestampFirst;

                RowForReport row = new RowForReport();
                row.col_sysId              = rec_object.getSysId();
                row.col_location           = rec_loc != null ? rec_loc.getName() : null;
                row.col_name               = rec_object.getName();
                row.col_deviceId           = id;
                row.col_field              = rec_object.getIdentifier();
                row.col_snapshots          = snapshots;
                row.col_snapshotsPerDay    = (int) ((double) snapshots / diff * 86400);
                row.col_firstTimestamp     = TimeUtils.fromTimestampToUtcTime(timestampFirst);
                row.col_lastTimestamp      = TimeUtils.fromTimestampToUtcTime(timestampLast);
                row.col_archives           = archives;
                row.col_storage            = storage;
                row.col_storageUnpadded    = storageUnpadded;
                row.col_storagePerSnapshot = (double) storage / snapshots;
                row.col_storagePerDay      = (int) ((double) storage / diff * 86400);
                row.col_sampled            = rec_object.isSampled() ? 1 : 0;

                List<String> missed = Lists.newArrayList();
                for (String key : counts.keySet())
                {
                    int count = counts.get(key);
                    if (count != snapshots)
                    {
                        missed.add(String.format("%s:%d", key, snapshots - count));

                        state.totalMissed.addAndGet(snapshots - count);
                    }
                }
                row.col_missed = StringUtils.join(missed, " ");

                synchronized (state)
                {
                    rowHandler.emitRow(row);
                }

                state.totalSnapshots.addAndGet(snapshots);
            }
        }
    }

    public static void dumpSampleStatistics(SessionProvider sessionProvider,
                                            Logger logger,
                                            boolean showMissedTimestamps,
                                            String outputFile,
                                            boolean onlyNetworks,
                                            boolean onlyGateways) throws
                                                                  Exception
    {
        SampleStatistics state = new SampleStatistics(sessionProvider, logger, showMissedTimestamps, outputFile, onlyNetworks, onlyGateways);

        state.compute();
    }

    //--//

    static class SampleStatisticsPerDevice
    {
        static class RowForReport
        {
            @TabularField(order = 0, title = "ID")
            public String col_id;

            @TabularField(order = 1, title = "Context")
            public String col_context;

            @TabularField(order = 2, title = "Descriptor")
            public String col_desc;

            @TabularField(order = 3, title = "Objects")
            public int col_objects;

            @TabularField(order = 4, title = "Samples")
            public int col_samples;

            @TabularField(order = 5, title = "Missed")
            public int col_missed;
        }

        final SessionProvider sessionProvider;
        final Logger          logger;
        final boolean         preStreamArchives;
        final String          outputFile;

        static final int reportFrequency = 10;

        MonotonousTime                       report            = TimeUtils.computeTimeoutExpiration(reportFrequency, TimeUnit.SECONDS);
        ConcurrentMap<String, AtomicInteger> samplesPerElement = Maps.newConcurrentMap();
        ConcurrentMap<String, AtomicInteger> missesPerElement  = Maps.newConcurrentMap();
        long                                 totalSamples      = 0;

        SampleStatisticsPerDevice(SessionProvider sessionProvider,
                                  Logger logger,
                                  boolean preStreamArchives,
                                  String outputFile)
        {
            this.sessionProvider   = sessionProvider;
            this.logger            = logger;
            this.preStreamArchives = preStreamArchives;
            this.outputFile        = outputFile;
        }

        private void compute() throws
                               Exception
        {
            try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
            {
                RecordHelper<DeviceElementSampleRecord> helper = sessionHolder.createHelper(DeviceElementSampleRecord.class);

                if (preStreamArchives)
                {
                    logger.info("Start sample streaming...");
                    DeviceElementSampleRecord.fastStreamRawArchives(helper, (deviceElementSysId, contents) ->
                    {
                        return SamplesCache.StreamNextAction.Continue;
                    });
                    logger.info("End sample streaming");
                }
                logger.info("Start sample analysis...");

                DeviceElementSampleRecord.fastExtractSamples(helper, DeviceElementRecord.DEFAULT_PROP_NAME, false, Object.class, (deviceElementSysId, extract) ->
                {
                    final int numSamples = extract.size();

                    totalSamples += numSamples;

                    if (TimeUtils.isTimeoutExpired(report))
                    {
                        logger.info("Progress Report: %,d samples...", totalSamples);
                        report = TimeUtils.computeTimeoutExpiration(reportFrequency, TimeUnit.SECONDS);
                    }

                    increment(samplesPerElement, deviceElementSysId, numSamples);

                    int missed = 0;

                    for (int i = 0; i < numSamples; i++)
                    {
                        double value = extract.getNthValueRaw(i);
                        if (Double.isNaN(value))
                        {
                            missed++;
                        }
                    }

                    increment(missesPerElement, deviceElementSysId, missed);

                    return SamplesCache.StreamNextAction.Continue;
                });
            }
            logger.info("End sample analysis: %,d samples", totalSamples);
        }

        void dump() throws
                    Exception
        {
            try (var holder = new TabularReportAsExcel.Holder())
            {
                TabularReportAsExcel<RowForReport> tr = new TabularReportAsExcel<>(RowForReport.class, "Samples", holder);

                tr.emit(rowHandler ->
                        {
                            try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
                            {
                                RecordHelper<NetworkAssetRecord> helper_network = sessionHolder.createHelper(NetworkAssetRecord.class);
                                RecordHelper<DeviceRecord>       helper_device  = sessionHolder.createHelper(DeviceRecord.class);

                                for (NetworkAssetRecord rec : helper_network.listAll())
                                {
                                    rec.enumerateChildren(helper_device, true, -1, null, (rec_device) ->
                                    {
                                        dumpPerDevice(rowHandler, rec_device.getSysId(), rec.getName(), rec_device.getIdentityDescriptor());

                                        return StreamHelperNextAction.Continue_Evict;
                                    });
                                }
                            }
                            catch (Exception e)
                            {
                                // Ignore
                            }
                        });

                try (FileOutputStream outputStream = new FileOutputStream(outputFile))
                {
                    holder.toStream(outputStream);
                }
            }
        }

        private void dumpPerDevice(TabularReport<RowForReport>.RowHandler rowHandler,
                                   String deviceSysId,
                                   String context,
                                   BaseAssetDescriptor desc) throws
                                                             Exception
        {
            try (SessionHolder holder = sessionProvider.newReadOnlySession())
            {
                RecordHelper<DeviceElementRecord> helper_de = holder.createHelper(DeviceElementRecord.class);

                AtomicInteger objects = new AtomicInteger();
                AtomicInteger count   = new AtomicInteger();
                AtomicInteger missed  = new AtomicInteger();

                final DeviceElementFilterRequest filters = DeviceElementFilterRequest.createFilterForParent(deviceSysId);
                DeviceElementRecord.enumerate(helper_de, true, filters, (rec_object) ->
                {
                    objects.incrementAndGet();

                    AtomicInteger samples = fetch(samplesPerElement, rec_object.getSysId());
                    AtomicInteger misses  = fetch(missesPerElement, rec_object.getSysId());

                    count.addAndGet(samples.get());
                    missed.addAndGet(misses.get());

                    return StreamHelperNextAction.Continue_Evict;
                });

                RowForReport row = new RowForReport();
                row.col_id      = deviceSysId;
                row.col_context = context;
                row.col_desc    = Objects.toString(desc);
                row.col_objects = objects.get();
                row.col_samples = count.get();
                row.col_missed  = missed.get();

                rowHandler.emitRow(row);
            }
        }

        private AtomicInteger fetch(ConcurrentMap<String, AtomicInteger> map,
                                    String id)
        {
            AtomicInteger num = map.get(id);
            if (num != null)
            {
                return num;
            }

            num = new AtomicInteger();
            AtomicInteger numOld = map.put(id, num);
            return numOld != null ? numOld : num;
        }

        private void increment(ConcurrentMap<String, AtomicInteger> map,
                               String id,
                               int amount)
        {
            AtomicInteger num = fetch(map, id);
            num.addAndGet(amount);
        }
    }

    public static void dumpSampleStatisticsPerDevice(SessionProvider sessionProvider,
                                                     Logger logger,
                                                     boolean preStreamArchives,
                                                     String outputFile) throws
                                                                        Exception
    {
        SampleStatisticsPerDevice state = new SampleStatisticsPerDevice(sessionProvider, logger, preStreamArchives, outputFile);

        state.compute();

        state.dump();
    }

    //--//

    @Override
    public void assetPostCreate(SessionHolder sessionHolder)
    {
        // Nothing to do.
    }

    @Override
    protected void assetPostUpdateInner(SessionHolder sessionHolder)
    {
        // Nothing to do.
    }

    @Override
    protected boolean canRemoveChildren()
    {
        // We store trends under a DeviceElement record, so it's fine to delete the record when children are present.
        return true;
    }
}
