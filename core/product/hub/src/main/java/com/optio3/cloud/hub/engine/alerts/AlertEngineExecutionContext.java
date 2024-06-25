/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionProgram;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlert;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAsset;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAssets;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueControlPointCoordinates;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueTravelEntry;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueTravelLog;
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTime;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveNumber;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.logic.location.LongitudeAndLatitudeRecords;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.model.ControlPointsSelection;
import com.optio3.cloud.hub.model.DeliveryOptions;
import com.optio3.cloud.hub.model.alert.AlertSeverity;
import com.optio3.cloud.hub.model.alert.AlertStatus;
import com.optio3.cloud.hub.model.alert.AlertType;
import com.optio3.cloud.hub.model.asset.AssetTravelLog;
import com.optio3.cloud.hub.model.asset.graph.AssetGraph;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphNode;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphRequest;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphResponse;
import com.optio3.cloud.hub.model.dashboard.AggregationRequest;
import com.optio3.cloud.hub.model.dashboard.AggregationResponse;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionLogRecord;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionRecord;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionVersionRecord;
import com.optio3.cloud.hub.persistence.alert.AlertHistoryRecord;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.LogHandler;
import com.optio3.cloud.persistence.LogHolder;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.collection.ExpandableArrayOfDoubles;
import com.optio3.collection.MapWithSoftValues;
import com.optio3.logging.ILogger;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import com.optio3.util.IdGenerator;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.BiConsumerWithException;
import com.optio3.util.function.BiFunctionWithException;
import org.apache.commons.lang3.StringUtils;

public class AlertEngineExecutionContext extends EngineExecutionContext<AlertDefinitionDetails, AlertEngineExecutionStep> implements AutoCloseable
{
    public static class State
    {
        public String version = IdGenerator.newGuid();

        public final TreeMap<String, ZonedDateTime>           lastSeenSamplesPerDeviceElement = new TreeMap<>();
        public final TreeMap<String, TreeMap<String, Object>> deviceElementState              = new TreeMap<>();

        public boolean containsDeviceElement(String sysId)
        {
            return lastSeenSamplesPerDeviceElement.containsKey(sysId) || deviceElementState.containsKey(sysId);
        }
    }

    public class SamplesSnapshot implements AutoCloseable
    {
        private final String m_sysId;

        private ZonedDateTime            m_cachedRangeStart;
        private ExpandableArrayOfDoubles m_timestamps;

        SamplesSnapshot(String sysId)
        {
            m_sysId = sysId;
        }

        @Override
        public void close()
        {
            if (m_timestamps != null)
            {
                m_timestamps.close();
                m_timestamps = null;
            }
        }

        private void ensureTimestamps(ZonedDateTime rangeStart,
                                      Duration maxWaitForSpooler)
        {
            if (m_timestamps != null)
            {
                if (m_cachedRangeStart != null && TimeUtils.isBeforeOrNull(rangeStart, m_cachedRangeStart))
                {
                    m_timestamps = null;
                }
            }

            if (m_timestamps == null)
            {
                m_timestamps       = getSamplesCache().extractTimestamps(m_sysId, rangeStart, null, maxWaitForSpooler);
                m_cachedRangeStart = rangeStart;
            }
        }

        public ZonedDateTime getLastTimestamp()
        {
            ensureTimestamps(null, Duration.of(10, ChronoUnit.SECONDS));

            if (m_timestamps.size() == 0)
            {
                return null;
            }

            double rawThresholdTimestamp = TimeUtils.fromUtcTimeToTimestamp(thresholdTimestamp);
            double lastTimestamp         = m_timestamps.get(m_timestamps.size() - 1, Double.NaN);

            if (rawThresholdTimestamp < lastTimestamp)
            {
                int pos = m_timestamps.binarySearch(rawThresholdTimestamp);
                if (pos < 0)
                {
                    pos = ~pos;
                }

                lastTimestamp = m_timestamps.get(pos, Double.NaN);
            }

            return TimeUtils.fromTimestampToUtcTime(lastTimestamp);
        }

        public List<ZonedDateTime> getTimestamps(ZonedDateTime rangeStart,
                                                 ZonedDateTime rangeEnd,
                                                 Duration maxWaitForSpooler)
        {
            ensureTimestamps(rangeStart, maxWaitForSpooler);

            List<ZonedDateTime> lst         = Lists.newArrayList();
            double              rawRangeEnd = TimeUtils.fromUtcTimeToTimestamp(limitTimestamp(rangeEnd));
            int                 position;

            if (rangeStart != null)
            {
                //
                // Use binary search to find the first timestamp in the range.
                //
                position = m_timestamps.binarySearch(TimeUtils.fromUtcTimeToTimestamp(rangeStart));
                if (position < 0)
                {
                    position = ~position;
                }
            }
            else
            {
                position = 0;
            }

            while (position < m_timestamps.size())
            {
                double timestamp = m_timestamps.get(position++, TimeUtils.maxEpochSeconds());

                if (timestamp > rawRangeEnd)
                {
                    break;
                }

                lst.add(TimeUtils.fromTimestampToUtcTime(timestamp));
            }

            return lst;
        }

        public <T> T getSample(ZonedDateTime timestamp,
                               String prop,
                               EngineeringUnitsFactors convertTo,
                               boolean nearest,
                               boolean onlyBeforeTarget,
                               Class<T> clz,
                               Duration maxWaitForSpooler)
        {
            return getSamplesCache().getSample(m_sysId, limitTimestamp(timestamp), prop, convertTo, nearest, onlyBeforeTarget, clz, maxWaitForSpooler);
        }

        public TimeSeries.NumericValueRanges collectSampleRanges(String prop,
                                                                 EngineeringUnitsFactors convertTo,
                                                                 ZonedDateTime start,
                                                                 ZonedDateTime end,
                                                                 Duration maxWaitForSpooler)
        {
            return getSamplesCache().collectSampleRanges(m_sysId, prop, convertTo, start, end, maxWaitForSpooler);
        }

        public double computeAggregation(AggregationRequest req)
        {
            req.selections = new ControlPointsSelection();
            req.selections.identities.add(TypedRecordIdentity.newTypedInstance(DeviceElementRecord.class, m_sysId));

            AggregationResponse res = req.execute(sessionProvider);
            if (res != null)
            {
                double[] results = CollectionUtils.getNthElement(res.resultsPerRange, 0);
                if (results != null && results.length > 0)
                {
                    return results[0];
                }
            }

            return Double.NaN;
        }
    }

    public class CoordinatesSnapshot
    {
        private final LongitudeAndLatitudeRecords m_records;
        private       ZonedDateTime               m_cachedRangeStart;
        private       ZonedDateTime               m_cachedRangeEnd;
        private       AssetTravelLog.Raw          m_samples;

        CoordinatesSnapshot(LongitudeAndLatitudeRecords records)
        {
            m_records = records;
        }

        private void ensureTimestamps(ZonedDateTime rangeStart,
                                      Duration maxWaitForSpooler)
        {
            if (m_cachedRangeStart != null && TimeUtils.isBeforeOrNull(rangeStart, m_cachedRangeStart))
            {
                m_samples = null;
            }

            if (m_samples == null)
            {
                m_samples          = AssetTravelLog.collect(getSamplesCache(), m_records.longitude, m_records.latitude, rangeStart, thresholdTimestamp);
                m_cachedRangeStart = rangeStart;
            }
        }

        public AlertEngineValueControlPointCoordinates getCoordinates()
        {
            return AlertEngineValueControlPointCoordinates.create(m_records.longitude, m_records.latitude);
        }

        public ZonedDateTime getLastTimestamp()
        {
            ensureTimestamps(null, Duration.of(10, ChronoUnit.SECONDS));

            int length = m_samples.timestamps.length;
            if (length == 0)
            {
                return null;
            }

            double rawThresholdTimestamp = TimeUtils.fromUtcTimeToTimestamp(thresholdTimestamp);
            double lastTimestamp         = m_samples.timestamps[length - 1];

            if (rawThresholdTimestamp < lastTimestamp)
            {
                int pos = Arrays.binarySearch(m_samples.timestamps, rawThresholdTimestamp);
                if (pos < 0)
                {
                    pos = ~pos;
                }

                if (pos == length)
                {
                    return null;
                }

                lastTimestamp = m_samples.timestamps[pos];
            }

            return TimeUtils.fromTimestampToUtcTime(lastTimestamp);
        }

        public AlertEngineValueTravelLog getTravelLog(ZonedDateTime rangeStart,
                                                      ZonedDateTime rangeEnd,
                                                      Duration maxWaitForSpooler)
        {
            ensureTimestamps(rangeStart, maxWaitForSpooler);

            double rawRangeEnd = TimeUtils.fromUtcTimeToTimestamp(limitTimestamp(rangeEnd));
            int    position;

            if (rangeStart != null)
            {
                //
                // Use binary search to find the first timestamp in the range.
                //
                position = Arrays.binarySearch(m_samples.timestamps, TimeUtils.fromUtcTimeToTimestamp(rangeStart));
                if (position < 0)
                {
                    position = ~position;
                }
            }
            else
            {
                position = 0;
            }

            var data = new AlertEngineValueTravelLog();

            while (position < m_samples.timestamps.length)
            {
                double timestamp = m_samples.timestamps[position];
                if (timestamp > rawRangeEnd)
                {
                    break;
                }

                double longitude = m_samples.longitude[position];
                double latitude  = m_samples.latitude[position];
                if (!Double.isNaN(longitude) && !Double.isNaN(latitude))
                {
                    data.elements.add(AlertEngineValueTravelEntry.create(timestamp, longitude, latitude));
                }

                position++;
            }

            return data;
        }

        public ZonedDateTime getLastSeenSample()
        {
            return AlertEngineExecutionContext.this.getLastSeenSample(m_records.longitude);
        }

        public void setLastSeenSample(ZonedDateTime timestamp)
        {
            AlertEngineExecutionContext.this.setLastSeenSample(m_records.longitude, timestamp);
        }

        public void markControlPointAsSeen()
        {
            AlertEngineExecutionContext.this.markControlPointAsSeen(m_records.longitude);
        }
    }

    public static class AlertPlaceHolder
    {
        public AlertEngineValueAlert state;
        public boolean               isRealRecord;
    }

    private static class AlertsByType
    {
        private final Map<AlertType, AlertPlaceHolder> m_lookup = Maps.newHashMap();
    }

    public class AlertHolder
    {
        private final Map<String, AlertsByType> m_controlPoints = Maps.newHashMap();
        private       boolean                   m_ignoreRealAlerts;
        private       ZonedDateTime             m_cutoffTimestamp;

        //--//

        public void ignoreRealAlerts()
        {
            m_ignoreRealAlerts = true;
        }

        public void setCutoffTimestamp(ZonedDateTime timestamp)
        {
            m_cutoffTimestamp = timestamp;
        }

        public AlertEngineValueAlert createAlert(TypedRecordIdentity<DeviceElementRecord> controlPoint,
                                                 AlertType type) throws
                                                                 Exception
        {
            AlertPlaceHolder placeHolder = ensureAlertPlaceHolder(controlPoint, type);

            AlertEngineValueAlert state = new AlertEngineValueAlert();
            state.controlPoint = controlPoint;
            state.type         = type;
            state.status       = AlertStatus.active;
            state.severity     = AlertSeverity.NORMAL;
            state.shouldNotify = true;

            if (placeHolder.state != null && placeHolder.state.status != AlertStatus.closed)
            {
                // Reuse record for non-closed alerts.
                state.record = placeHolder.state.record;
            }
            else
            {
                state.record            = TypedRecordIdentity.newTypedInstance(AlertRecord.class, IdGenerator.newGuid());
                state.record.lastUpdate = thresholdTimestamp;

                placeHolder.isRealRecord = false;
            }

            placeHolder.state = state;

            return state;
        }

        public AlertEngineValueAlert getAlert(TypedRecordIdentity<DeviceElementRecord> controlPoint,
                                              AlertType type) throws
                                                              Exception
        {
            AlertPlaceHolder placeHolder = ensureAlertPlaceHolder(controlPoint, type);
            return placeHolder.state;
        }

        public void resetNotificationFlags()
        {
            for (AlertsByType value : m_controlPoints.values())
            {
                for (AlertPlaceHolder placeHolder : value.m_lookup.values())
                {
                    AlertEngineValueAlert state = placeHolder.state;
                    if (state != null)
                    {
                        state.shouldNotify = false;
                        state.timestamp    = null;
                    }
                }
            }
        }

        //--//

        private AlertPlaceHolder ensureAlertPlaceHolder(TypedRecordIdentity<DeviceElementRecord> controlPoint,
                                                        AlertType type) throws
                                                                        Exception
        {
            AlertsByType     byType      = m_controlPoints.computeIfAbsent(controlPoint.sysId, (sysId) -> new AlertsByType());
            AlertPlaceHolder placeHolder = byType.m_lookup.get(type);

            if (placeHolder == null)
            {
                placeHolder = new AlertPlaceHolder();

                if (!m_ignoreRealAlerts)
                {
                    AlertEngineValueAlert state = findExistingAlert(controlPoint, type);
                    if (state != null)
                    {
                        placeHolder.state        = state;
                        placeHolder.isRealRecord = true;
                    }
                }

                byType.m_lookup.put(type, placeHolder);
            }

            return placeHolder;
        }

        private AlertEngineValueAlert findExistingAlert(TypedRecordIdentity<DeviceElementRecord> controlPoint,
                                                        AlertType type) throws
                                                                        Exception
        {
            return withControlPointReadonly(controlPoint, (subSessionHolder, rec_controlPoint) ->
            {
                if (rec_controlPoint != null)
                {
                    for (AlertRecord rec_alert : rec_controlPoint.getAlerts())
                    {
                        if (rec_alert.getStatus() == AlertStatus.closed)
                        {
                            continue;
                        }

                        if (rec_alert.getType() != type)
                        {
                            continue;
                        }

                        if (m_cutoffTimestamp != null && !m_cutoffTimestamp.isAfter(rec_alert.getCreatedOn()))
                        {
                            // Ignore alerts created after the cutoff timestamp.
                            continue;
                        }

                        AlertDefinitionVersionRecord rec_ver = rec_alert.getAlertDefinitionVersion();
                        if (rec_ver != null)
                        {
                            // Match on the definition, not the version, or we could recreate all the alerts.
                            AlertDefinitionRecord rec_def = rec_ver.getDefinition();
                            if (rec_def != null && StringUtils.equals(rec_def.getSysId(), m_alertDefinitionSysId))
                            {
                                AlertEngineValueAlert state = new AlertEngineValueAlert();
                                state.controlPoint = controlPoint;
                                state.type         = type;
                                state.severity     = rec_alert.getSeverity();
                                state.status       = rec_alert.getStatus();
                                state.description  = rec_alert.getExtendedDescription();
                                state.record       = RecordIdentity.newTypedInstance(rec_alert);

                                AlertHistoryRecord rec_history = rec_alert.getLastHistory(subSessionHolder.createHelper(AlertHistoryRecord.class));
                                if (rec_history != null)
                                {
                                    state.statusText = rec_history.getText();
                                }

                                return state;
                            }
                        }
                    }
                }

                return null;
            });
        }
    }

    //--//

    public <T> T withControlPoint(TypedRecordIdentity<DeviceElementRecord> ri,
                                  BiFunctionWithException<SessionHolder, DeviceElementRecord, T> callback) throws
                                                                                                           Exception
    {
        try (SessionHolder subSessionHolder = sessionProvider.newSessionWithTransaction())
        {
            DeviceElementRecord rec = subSessionHolder.fromIdentityOrNull(ri);

            T res = callback.apply(subSessionHolder, rec);

            subSessionHolder.commit();

            return res;
        }
    }

    public <T> T withControlPointReadonly(TypedRecordIdentity<DeviceElementRecord> ri,
                                          BiFunctionWithException<SessionHolder, DeviceElementRecord, T> callback) throws
                                                                                                                   Exception
    {
        try (SessionHolder subSessionHolder = sessionProvider.newReadOnlySession())
        {
            DeviceElementRecord rec = subSessionHolder.fromIdentityOrNull(ri);

            return callback.apply(subSessionHolder, rec);
        }
    }

    public void withControlPoint(TypedRecordIdentity<DeviceElementRecord> ri,
                                 BiConsumerWithException<SessionHolder, DeviceElementRecord> callback) throws
                                                                                                       Exception
    {
        try (SessionHolder subSessionHolder = sessionProvider.newSessionWithTransaction())
        {
            DeviceElementRecord rec = subSessionHolder.fromIdentityOrNull(ri);

            callback.accept(subSessionHolder, rec);

            subSessionHolder.commit();
        }
    }

    public void withControlPointReadonly(TypedRecordIdentity<DeviceElementRecord> ri,
                                         BiConsumerWithException<SessionHolder, DeviceElementRecord> callback) throws
                                                                                                               Exception
    {
        try (SessionHolder subSessionHolder = sessionProvider.newReadOnlySession())
        {
            DeviceElementRecord rec = subSessionHolder.fromIdentityOrNull(ri);

            callback.accept(subSessionHolder, rec);
        }
    }

    //--//

    private static final int c_logLimit         = 10_000;
    private static final int c_logAllowedExcess = 2_000;

    public AlertHolder alertHolder = new AlertHolder();

    //--//

    private       Duration                          m_backtracingForUnseenDeviceElement;
    private final String                            m_alertDefinitionSysId;
    private final String                            m_alertDefinitionVersionSysId;
    private final int                               m_alertDefinitionVersion;
    private       List<AssetGraphResponse.Resolved> m_assetGraphEvaluated;
    private       AssetGraphResponse.Resolved       m_assetGraphTuple;

    private SessionHolder                                               m_sessionHolder;
    private AlertDefinitionRecord                                       m_alertDefinitionRecord;
    private AlertDefinitionVersionRecord                                m_alertDefinitionVersionRecord;
    private LogHandler<AlertDefinitionRecord, AlertDefinitionLogRecord> m_alertDefinitionLogHandler;
    private LogHolder                                                   m_alertDefinitionLogHolder;

    private final Map<String, ZonedDateTime>           m_lastSamplePerDeviceElement       = Maps.newHashMap();
    private final Map<String, ZonedDateTime>           m_lastSamplePerDeviceElementFuture = Maps.newHashMap();
    private final Map<String, TreeMap<String, Object>> m_deviceElementState               = Maps.newHashMap();
    private final Map<String, String>                  m_assetToLocation                  = Maps.newHashMap();
    private final Map<String, String>                  m_assetToName                      = Maps.newHashMap();

    private final MapWithSoftValues<String, Class<? extends AssetRecord>> m_lookupAssets      = new MapWithSoftValues<>();
    private final MapWithSoftValues<String, SamplesSnapshot>              m_lookupSamples     = new MapWithSoftValues<>();
    private final MapWithSoftValues<String, CoordinatesSnapshot>          m_lookupCoordinates = new MapWithSoftValues<>();
    private final MapWithSoftValues<String, ZonedDateTime>                m_lookupTimestamps  = new MapWithSoftValues<>();

    //--//

    public AlertEngineExecutionContext(ILogger logger,
                                       SessionHolder sessionHolder,
                                       AlertDefinitionVersionRecord rec_ver,
                                       Duration backtracingForUnseenDeviceElement)
    {
        this(logger,
             sessionHolder.getSessionProvider(),
             rec_ver.getDefinition()
                    .getSysId(),
             rec_ver.getSysId(),
             rec_ver.getVersion(),
             rec_ver.prepareProgram(sessionHolder),
             backtracingForUnseenDeviceElement);
    }

    public AlertEngineExecutionContext(ILogger logger,
                                       SessionProvider sessionProvider,
                                       String alertDefinitionSysId,
                                       String alertDefinitionVersionSysId,
                                       int alertDefinitionVersion,
                                       EngineExecutionProgram<AlertDefinitionDetails> program,
                                       Duration backtracingForUnseenDeviceElement)
    {
        super(logger, sessionProvider, program);

        m_alertDefinitionSysId        = alertDefinitionSysId;
        m_alertDefinitionVersionSysId = alertDefinitionVersionSysId;
        m_alertDefinitionVersion      = alertDefinitionVersion;

        if (backtracingForUnseenDeviceElement != null)
        {
            m_backtracingForUnseenDeviceElement = backtracingForUnseenDeviceElement;
        }
        else
        {
            m_backtracingForUnseenDeviceElement = Duration.of(1, ChronoUnit.HOURS);
        }
    }

    @Override
    public void close()
    {
        for (SamplesSnapshot samplesSnapshot : m_lookupSamples.values())
        {
            samplesSnapshot.close();
        }

        if (m_alertDefinitionLogHolder != null)
        {
            try
            {
                m_alertDefinitionLogHolder.close();
            }
            catch (Throwable t)
            {
                // Ignore logging failures.
            }

            m_alertDefinitionLogHolder = null;
        }

        if (m_alertDefinitionLogHandler != null)
        {
            try
            {
                m_alertDefinitionLogHandler.close();
            }
            catch (Throwable t)
            {
                // Ignore logging failures.
            }

            m_alertDefinitionLogHandler = null;
        }

        if (m_sessionHolder != null)
        {
            m_sessionHolder.commit();
            m_sessionHolder.close();
            m_sessionHolder = null;
        }
    }

    public static void validate(SessionHolder sessionHolder,
                                AlertDefinitionVersionRecord rec)
    {
        EngineExecutionProgram<AlertDefinitionDetails> program = rec.prepareProgram(sessionHolder);

        rec.setDetails(program.definition);
    }

    public void importState(State state)
    {
        m_lastSamplePerDeviceElement.clear();
        m_lastSamplePerDeviceElement.putAll(state.lastSeenSamplesPerDeviceElement);

        m_lastSamplePerDeviceElementFuture.clear();

        m_deviceElementState.clear();
        m_deviceElementState.putAll(state.deviceElementState);
    }

    public void exportState(State state)
    {
        state.version = IdGenerator.newGuid();

        state.lastSeenSamplesPerDeviceElement.clear();
        state.lastSeenSamplesPerDeviceElement.putAll(m_lastSamplePerDeviceElement);
        state.lastSeenSamplesPerDeviceElement.putAll(m_lastSamplePerDeviceElementFuture);

        state.deviceElementState.clear();
        m_deviceElementState.forEach((k, metadata) ->
                                     {
                                         if (metadata != null && metadata.size() > 0)
                                         {
                                             state.deviceElementState.put(k, metadata);
                                         }
                                     });
    }

    @Override
    public void reset(ZonedDateTime when)
    {
        super.reset(when);

        m_lastSamplePerDeviceElement.putAll(m_lastSamplePerDeviceElementFuture);
        m_lastSamplePerDeviceElementFuture.clear();
    }

    //--//

    public SessionHolder ensureSession()
    {
        if (m_sessionHolder == null)
        {
            m_sessionHolder = sessionProvider.newSessionWithTransaction();
        }

        return m_sessionHolder;
    }

    public void logEntry(EngineExecutionStack stack,
                         String line)
    {
        if (m_alertDefinitionLogHolder == null)
        {
            SessionHolder                       sessionHolder = ensureSession();
            RecordLocked<AlertDefinitionRecord> lock_version  = sessionHolder.getEntityWithLock(AlertDefinitionRecord.class, m_alertDefinitionSysId, 2, TimeUnit.SECONDS);

            m_alertDefinitionLogHandler = AlertDefinitionRecord.allocateLogHandler(lock_version);

            //
            // When the log grows too long, only keep the last X lines.
            //
            int lastOffset = m_alertDefinitionLogHandler.getLastOffset();
            if (lastOffset > c_logLimit + c_logAllowedExcess)
            {
                try
                {
                    m_alertDefinitionLogHandler.delete(lastOffset - c_logLimit);
                }
                catch (Throwable t)
                {
                    // Ignore logging failures.
                }
            }

            m_alertDefinitionLogHolder = m_alertDefinitionLogHandler.newLogHolder();
        }

        m_alertDefinitionLogHolder.addLineSync(1, null, null, null, null, null, String.format("[v%d] %s", m_alertDefinitionVersion, line));
    }

    //--//

    private TreeMap<String, Object> getMetadata(TypedRecordIdentity<DeviceElementRecord> ri)
    {
        TreeMap<String, Object> metadata = m_deviceElementState.get(ri.sysId);
        if (metadata == null)
        {
            metadata = new TreeMap<>();
            m_deviceElementState.put(ri.sysId, metadata);
        }

        return metadata;
    }

    private Object getMetadataValueRaw(TypedRecordIdentity<DeviceElementRecord> ri,
                                       String key)
    {
        TreeMap<String, Object> map = getMetadata(ri);
        return map.get(key);
    }

    public String getMetadataString(TypedRecordIdentity<DeviceElementRecord> ri,
                                    String key)
    {
        Object value = getMetadataValueRaw(ri, key);
        if (value == null)
        {
            return null;
        }

        return value.toString();
    }

    public double getMetadataDouble(TypedRecordIdentity<DeviceElementRecord> ri,
                                    String key)
    {
        try
        {
            Object value = getMetadataValueRaw(ri, key);

            if (value instanceof Number)
            {
                return ((Number) value).doubleValue();
            }

            if (value instanceof String)
            {
                return Double.parseDouble((String) value);
            }

            return 0.0;
        }
        catch (Exception e)
        {
            // Parse error, return zero.
            return 0.0;
        }
    }

    public ZonedDateTime getMetadataTimestamp(TypedRecordIdentity<DeviceElementRecord> ri,
                                              String key)
    {
        try
        {
            String value = getMetadataString(ri, key);
            if (value == null)
            {
                return null;
            }

            ZonedDateTime timestamp = m_lookupTimestamps.get(value);
            if (timestamp == null)
            {
                timestamp = ZonedDateTime.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(value));
                m_lookupTimestamps.put(value, timestamp);
            }

            return timestamp;
        }
        catch (Exception e)
        {
            // Parse error, return null.
            return null;
        }
    }

    public void setMetadataValue(TypedRecordIdentity<DeviceElementRecord> ri,
                                 String key,
                                 EngineValue value)
    {
        TreeMap<String, Object> map = getMetadata(ri);

        EngineValuePrimitiveString valueString = Reflection.as(value, EngineValuePrimitiveString.class);
        if (valueString != null)
        {
            map.put(key, valueString.value);
            return;
        }

        EngineValuePrimitiveNumber valueNumber = Reflection.as(value, EngineValuePrimitiveNumber.class);
        if (valueNumber != null)
        {
            map.put(key, valueNumber.value);
            return;
        }

        EngineValueDateTime valueTimestamp = Reflection.as(value, EngineValueDateTime.class);
        if (valueTimestamp != null)
        {
            map.put(key, DateTimeFormatter.ISO_ZONED_DATE_TIME.format(valueTimestamp.value));
            return;
        }

        map.remove(key);
    }

    //--//

    public String getLocationSysId(TypedRecordIdentity<? extends AssetRecord> ri)
    {
        if (!m_assetToLocation.containsKey(ri.sysId))
        {
            String locationSysId = accessAsset(ri, (rec_asset) ->
            {
                LocationRecord rec_loc = SessionHolder.asEntityOfClassOrNull(rec_asset, LocationRecord.class);
                if (rec_loc == null)
                {
                    rec_loc = rec_asset.getLocation();
                }
                return RecordWithCommonFields.getSysIdSafe(rec_loc);
            });

            m_assetToLocation.put(ri.sysId, locationSysId);
        }

        return m_assetToLocation.get(ri.sysId);
    }

    public String getLocationName(TypedRecordIdentity<? extends AssetRecord> ri)
    {
        String sysId = getLocationSysId(ri);

        LocationsEngine.Snapshot snapshot = getLocationsEngineSnapshot();
        return snapshot.getName(sysId);
    }

    public String getAssetName(TypedRecordIdentity<? extends AssetRecord> ri)
    {
        if (!m_assetToName.containsKey(ri.sysId))
        {
            String name = accessAsset(ri, AssetRecord::getName);

            m_assetToName.put(ri.sysId, name);
        }

        return m_assetToName.get(ri.sysId);
    }

    public String getTimeZone(TypedRecordIdentity<DeviceElementRecord> ri)
    {
        String locationSysId = getLocationSysId(ri);

        LocationsEngine.Snapshot snapshot = getLocationsEngineSnapshot();
        return snapshot.getTimeZone(locationSysId);
    }

    public DeliveryOptions getDeliveryOptionsForEmail(TypedRecordIdentity<DeviceElementRecord> ri)
    {
        String locationSysId = getLocationSysId(ri);

        LocationsEngine.Snapshot snapshot = getLocationsEngineSnapshot();
        return snapshot.getEmailSettings(locationSysId, true);
    }

    public DeliveryOptions getDeliveryOptionsForSms(TypedRecordIdentity<DeviceElementRecord> ri)
    {
        String locationSysId = getLocationSysId(ri);
        if (locationSysId != null)
        {
            LocationsEngine.Snapshot snapshot = getLocationsEngineSnapshot();

            return snapshot.getSmsSettings(locationSysId, true);
        }

        return null;
    }

    public ZonedDateTime getLastSeenSample(TypedRecordIdentity<DeviceElementRecord> ri)
    {
        ZonedDateTime timestamp = m_lastSamplePerDeviceElement.get(ri.sysId);

        if (timestamp == null)
        {
            timestamp = thresholdTimestamp.minus(m_backtracingForUnseenDeviceElement);
        }

        return timestamp;
    }

    public void setLastSeenSample(TypedRecordIdentity<DeviceElementRecord> ri,
                                  ZonedDateTime timestamp)
    {
        timestamp = TimeUtils.min(timestamp, thresholdTimestamp);

        m_lastSamplePerDeviceElementFuture.put(ri.sysId, timestamp);
    }

    public void markControlPointAsSeen(TypedRecordIdentity<DeviceElementRecord> ri)
    {
        if (!m_lastSamplePerDeviceElement.containsKey(ri.sysId))
        {
            m_lastSamplePerDeviceElementFuture.putIfAbsent(ri.sysId, null);
        }
    }

    public SamplesSnapshot getSamples(TypedRecordIdentity<DeviceElementRecord> ri)
    {
        SamplesSnapshot snapshot = m_lookupSamples.get(ri.sysId);
        if (snapshot == null)
        {
            snapshot = new SamplesSnapshot(ri.sysId);
            m_lookupSamples.put(ri.sysId, snapshot);
        }

        return snapshot;
    }

    public CoordinatesSnapshot getCoordinates(TypedRecordIdentity<DeviceElementRecord> ri)
    {
        CoordinatesSnapshot snapshot = m_lookupCoordinates.get(ri.sysId);
        if (snapshot == null)
        {
            LongitudeAndLatitudeRecords lookup = new LongitudeAndLatitudeRecords();

            try (SessionHolder subSessionHolder = sessionProvider.newReadOnlySession())
            {
                lookup.locate(subSessionHolder, subSessionHolder.fromIdentityOrNull(ri));
            }

            if (!lookup.isValid())
            {
                return null;
            }

            String sysId_longitude = lookup.longitude.sysId;
            String sysId_latitude  = lookup.latitude.sysId;

            snapshot = m_lookupCoordinates.get(sysId_longitude);
            if (snapshot == null)
            {
                snapshot = m_lookupCoordinates.get(sysId_latitude);
            }

            if (snapshot == null)
            {
                snapshot = new CoordinatesSnapshot(lookup);
                m_lookupCoordinates.put(ri.sysId, snapshot);
                m_lookupCoordinates.put(sysId_longitude, snapshot);
                m_lookupCoordinates.put(sysId_latitude, snapshot);
            }
        }

        return snapshot;
    }

    public TypedRecordIdentity<? extends AssetRecord> resolveAssetIdentity(TypedRecordIdentity<? extends AssetRecord> ri)
    {
        if (!TypedRecordIdentity.isValid(ri))
        {
            return null;
        }

        String                       sysId = ri.sysId;
        Class<? extends AssetRecord> clz   = m_lookupAssets.get(sysId);
        if (clz == null)
        {
            TagsEngine.Snapshot                        snapshot   = getTagsEngineSnapshot();
            TypedRecordIdentity<? extends AssetRecord> riResolved = snapshot.resolveAsset(sysId);
            if (riResolved != null)
            {
                clz = riResolved.resolveEntityClass();
            }
            else
            {
                clz = accessAsset(AssetRecord.class, sysId, AssetRecord::getClass);
            }

            if (clz == null)
            {
                return null;
            }

            m_lookupAssets.put(sysId, clz);
        }

        return TypedRecordIdentity.newTypedInstance(clz, sysId);
    }

    public <T extends AssetRecord, U> U accessAsset(TypedRecordIdentity<T> ri,
                                                    Function<T, U> callback)
    {
        return ri != null ? accessAsset(ri.resolveEntityClass(), ri.sysId, callback) : null;
    }

    public <T extends AssetRecord, U> U accessAsset(Class<T> clz,
                                                    String sysId,
                                                    Function<T, U> callback)
    {
        SessionHolder sessionHolder = ensureSession();

        T rec = sessionHolder.getEntityOrNull(clz, sysId);
        if (rec == null)
        {
            return null;
        }

        U res = callback.apply(rec);

        sessionHolder.evictEntity(rec);

        return res;
    }

    public <T extends RecordWithCommonFields> T loadRecordIdentity(TypedRecordIdentity<T> ri)
    {
        SessionHolder sessionHolder = ensureSession();

        return sessionHolder.fromIdentityOrNull(ri);
    }

    public AlertDefinitionRecord getAlertDefinitionRecord()
    {
        if (m_alertDefinitionRecord == null)
        {
            SessionHolder sessionHolder = ensureSession();

            m_alertDefinitionRecord = sessionHolder.getEntityOrNull(AlertDefinitionRecord.class, m_alertDefinitionSysId);
        }

        return m_alertDefinitionRecord;
    }

    public AlertDefinitionVersionRecord getAlertDefinitionVersionRecord()
    {
        if (m_alertDefinitionVersionRecord == null)
        {
            SessionHolder sessionHolder = ensureSession();

            m_alertDefinitionVersionRecord = sessionHolder.getEntityOrNull(AlertDefinitionVersionRecord.class, m_alertDefinitionVersionSysId);
        }

        return m_alertDefinitionVersionRecord;
    }

    //--//

    public AlertEngineValueAsset<?> resolveGraphNode(String nodeId)
    {
        AlertEngineValueAssets res = resolveGraphNodes(nodeId);
        return res.getLength() == 1 ? res.getNthElement(0) : null;
    }

    public AlertEngineValueAssets resolveGraphNodes(String nodeId)
    {
        AlertEngineValueAssets res = new AlertEngineValueAssets();

        if (m_assetGraphTuple != null && nodeId != null)
        {
            AssetGraphNode.Analyzed nodeAnalyzed = m_assetGraphTuple.graph.lookupNode(nodeId);
            if (nodeAnalyzed != null)
            {
                for (String sysId : m_assetGraphTuple.tuple[nodeAnalyzed.index])
                {
                    TypedRecordIdentity<? extends AssetRecord> ri = TypedRecordIdentity.newTypedInstance(AssetRecord.class, sysId);
                    ri = resolveAssetIdentity(ri);
                    if (ri != null)
                    {
                        res.elements.add(AlertEngineValueAsset.create(this, ri));
                    }
                }
            }
        }

        return res;
    }

    public List<AssetGraphResponse.Resolved> forEachGraphTuple()
    {
        if (m_assetGraphEvaluated == null)
        {
            AssetGraph graph = this.program.definition.graph;
            if (AssetGraph.isValid(graph))
            {
                AssetGraphRequest graphReq = new AssetGraphRequest();
                graphReq.graph = graph;

                AssetGraphResponse tuples = graphReq.evaluate(getTagsEngineSnapshot());

                m_assetGraphEvaluated = tuples.results;
            }
            else
            {
                m_assetGraphEvaluated = Lists.newArrayList((AssetGraphResponse.Resolved) null);
            }
        }

        return m_assetGraphEvaluated;
    }

    public void setGraphTuple(AssetGraphResponse.Resolved tuple)
    {
        m_assetGraphTuple = tuple;
    }

    //--//

    @Nonnull
    public <T> T getServiceNonNull(Class<T> clz)
    {
        return sessionProvider.getServiceNonNull(clz);
    }

    //--//

    @Override
    protected AlertEngineExecutionStep allocateStep()
    {
        return new AlertEngineExecutionStep();
    }
}
