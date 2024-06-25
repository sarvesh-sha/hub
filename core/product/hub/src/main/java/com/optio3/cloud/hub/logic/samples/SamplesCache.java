/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.samples;

import java.io.IOException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hpsf.Decimal;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.cloud.hub.logic.metrics.MetricsBindingSpooler;
import com.optio3.cloud.hub.logic.protocol.IProtocolDecoder;
import com.optio3.cloud.hub.logic.spooler.ResultStagingSpooler;
import com.optio3.cloud.hub.model.asset.DeviceElementSampling;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesEnumeratedValue;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesExtract;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesSchemaResponse;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementSampleRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.MetricsDeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.asset.ResultStagingRecord;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.DatabaseActivity;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.collection.ExpandableArrayOfDoubles;
import com.optio3.collection.MapWithSoftValues;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsConverter;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.enums.BACnetEventState;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.service.IServiceProvider;
import com.optio3.util.GcTracker;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.BiFunctionWithException;
import com.optio3.util.function.FunctionWithException;

public class SamplesCache
{
    class LruFetchQueue<T extends RecordWithCommonFields>
    {
        private final Class<T>      m_entityClass;
        private final AtomicInteger m_processing = new AtomicInteger();

        private Map<String, BaseLru<T>> m_pending;

        LruFetchQueue(Class<T> entityClass)
        {
            m_entityClass = entityClass;
        }

        void queue(BaseLru<T> lru)
        {
        	 System.out.println("queue 2");
            Map<String, BaseLru<T>> pendingToFlush = queueInner(lru);
            if (pendingToFlush != null)
            {
           	 System.out.println("pendingToFlush 2");

                flush(pendingToFlush);
            }
        }

        private Map<String, BaseLru<T>> queueInner(BaseLru<T> lru)
        {
            Map<String, BaseLru<T>> pending;

            if (m_processing.get() == 0)
            {
                // When idle, start draining immediately.
                pending = Maps.newHashMap();
                pending.put(lru.sysId, lru);
            }
            else
            {
                synchronized (m_processing)
                {
                    pending = m_pending;
                    if (pending == null)
                    {
                        pending   = Maps.newHashMap();
                        m_pending = pending;

                        Executors.scheduleOnDefaultPool(this::drain, 5, TimeUnit.MILLISECONDS);
                    }

                    pending.put(lru.sysId, lru);

                    if (pending.size() >= 100)
                    {
                        m_pending = null;
                    }
                    else
                    {
                        pending = null;
                    }
                }
            }

            return pending;
        }

        private void drain()
        {
            while (true)
            {
                Map<String, BaseLru<T>> pending;

                synchronized (m_processing)
                {
                    pending = m_pending;
                    if (pending == null)
                    {
                        return;
                    }

                    m_pending = null;
                }

                flush(pending);
            }
        }

        private void flush(Map<String, BaseLru<T>> pending)
        {
        	
        	 System.out.println("flush 2");
            try
            {
                m_processing.incrementAndGet();
                System.out.println("flush 32");
                try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
                {
                	 System.out.println("flush 4");
                    var helper = sessionHolder.createHelper(m_entityClass);
                    System.out.println("flush52");

                    try
                    {
                        QueryHelperWithCommonFields<T, T> jh = new QueryHelperWithCommonFields<>(helper, m_entityClass);
                        System.out.println("flush 6");
                        jh.cq.select(jh.root);
                        jh.cq.where(jh.root.get(RecordWithCommonFields_.sysId)
                                           .in(pending.keySet()));
                        System.out.println("flush 7");

                        for (var rec : jh.list())
                        {
                            var pendingTarget = pending.remove(rec.getSysId());
                            if (pendingTarget != null)
                            {
                            	 System.out.println("flush 28");
                                pendingTarget.completeInitialization(sessionHolder, rec);
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        // Ignore failures.
                    }

                    for (BaseLru<T> pendingFailed : pending.values())
                    {
                        try
                        {
                            pendingFailed.completeInitialization(null, null);
                        }
                        catch (Exception e)
                        {
                            // Ignore failures.
                        }
                    }
                }
            }
            finally
            {
                m_processing.decrementAndGet();
            }
        }
    }

    private static abstract class BaseLru<T extends RecordWithCommonFields>
    {

        final   CompletableFuture<Boolean> ready = new CompletableFuture<>();
        final   String                     sysId;
        private MonotonousTime             m_lastUse;

        protected BaseLru(String sysId)
        {
            this.sysId = sysId;
        }

        void markAsReady(boolean success)
        {
            ready.complete(success);
        }

        boolean waitForReady()
        {
            try
            {
                return ready.get();
            }
            catch (Throwable t)
            {
                return false;
            }
        }

        void touched()
        {
            m_lastUse = MonotonousTime.now();
        }

        private static void purge(MapWithSoftValues<String, ? extends BaseLru<?>> map,
                                  float heapUtilization,
                                  float threshold,
                                  float purgeTarget)
        {
            if (heapUtilization > threshold)
            {
                class Age
                {
                    String         sysId;
                    MonotonousTime lastUse;
                }

                List<Age> entries = Lists.newArrayList();

                for (BaseLru<?> baseLru : map.values())
                {
                    var age = new Age();
                    age.sysId   = baseLru.sysId;
                    age.lastUse = baseLru.m_lastUse;
                    entries.add(age);
                }

                entries.sort(Comparator.comparing(a -> a.lastUse));

                int target = (int) (entries.size() * purgeTarget / 100.0f);
                for (Age entryToPurge : entries)
                {
                    map.remove(entryToPurge.sysId);

                    if (--target <= 0)
                    {
                        break;
                    }
                }
            }
        }

        abstract void completeInitialization(SessionHolder sessionHolder,
                                             T rec) throws
                                                    Exception;
    }

    private class ElementLru extends BaseLru<DeviceElementRecord>
    {
        private DeviceElementRecord.ArchiveSummary  summary;
        private boolean                             isSynthetic;
        private ZoneId                              timeZone;
        private List<DeviceElementSampling>         samplingSettings;
        private AssetRecord.PropertyTypeExtractor   extractor;
        private Map<String, TimeSeriesPropertyType> classifyWithoutPresentationType;
        private Map<String, TimeSeriesPropertyType> classifyAsPresentationType;

        private TimeSeries pendingTimeSeries;
        private boolean    pendingTimeSeriesFetched;

        private ElementLru(String sysId)
        {
            super(sysId);
            System.out.println("ElementLru 2");

        }

        void initialize()
        {
            m_pendingElements.queue(this);
            System.out.println("initialize 2");

        }

        @Override
        void completeInitialization(SessionHolder sessionHolder,
                                    DeviceElementRecord rec) throws
                                                             Exception
        {
        	System.out.println("completeInitialization 1");
            if (rec != null)
            {
            	
            //	TimeSeriesSchemaResponse m = new TimeSeriesSchemaResponse();
				Map<String, TimeSeriesPropertyType> map = new HashMap<String, TimeSeriesPropertyType>();
				TimeSeriesPropertyType a = new TimeSeriesPropertyType();
				a.expectedType = Decimal.class;
				a.name = "present_value";
			//	a.name = "Temprature";
			//	a.displayName = "Evet Type1";
				a.type = TimeSeries.SampleType.Decimal;
						//a.isBoolean=true;
				//0a.unitsFactors.scaling.multiplier;
					
				TimeSeriesEnumeratedValue t = new TimeSeriesEnumeratedValue();
				//EngineeringUnits eb = 
				//EngineeringUnits.latitude;
				//List<EngineeringUnits> el = new ArrayList<EngineeringUnits>();
				//el.add(eb);
				//List<EngineeringUnits> el1 = new ArrayList<EngineeringUnits>();
				//EngineeringUnitsFactors ef  = EngineeringUnitsFactors.fromValues(EngineeringUnitsFactors.Scaling.Identity, el, el1, EngineeringUnits.latitude);
			    //a.setUnitsFactors(ef);
				//a.setPrimaryUnits(EngineeringUnits.latitude);				
				List<TimeSeriesEnumeratedValue> list = new ArrayList<TimeSeriesEnumeratedValue>();
			//	t.name = "Evet Type";
				t.value = 11;
				list.add(t);
				a.values = list;
				//map.put("Temprature",a);
				map.put("present_value",a);
				//map.put("present_value",a);
                samplingSettings = rec.getSamplingSettings();
                extractor        = rec.getPropertyTypeExtractor();
                System.out.println("completeInitialization 222" +extractor);
              //  classifyWithoutPresentationType = map;//extractor.classifyRecord(rec, false);
                classifyWithoutPresentationType = extractor.classifyRecord(rec, false);
                
                System.out.println("completeInitialization 1" +classifyWithoutPresentationType);
               // classifyAsPresentationType      = map;//extractor.classifyRecord(rec, true);
                classifyAsPresentationType      = extractor.classifyRecord(rec, true);
                System.out.println("classifyAsPresentationType 1" +classifyWithoutPresentationType);
                isSynthetic = SessionHolder.isEntityOfClass(rec, MetricsDeviceElementRecord.class);
                if (isSynthetic)
                {
                    timeZone = null;
                }
                else
                {
                    String timeZoneText = rec.getPreferredTimeZone(m_app);
                    timeZone = StringUtils.isNotBlank(timeZoneText) ? ZoneId.of(timeZoneText) : null;
                }

                summary = rec.describeArchives(sessionHolder.createHelper(DeviceElementSampleRecord.class), classifyAsPresentationType, true);
                System.out.println("summary 1" +summary);

            }

            markAsReady(rec != null);
        }

        private TimeSeries fetchPendingSamples(IServiceProvider serviceProvider,
                                               Duration maxWaitForSpooler)
        {
            if (!pendingTimeSeriesFetched)
            {
                try
                {
                    ResultStagingSpooler spooler = serviceProvider.getServiceNonNull(ResultStagingSpooler.class);

                    pendingTimeSeries = spooler.flushSamples(sysId, maxWaitForSpooler);
                    if (pendingTimeSeries != null)
                    {
                        pendingTimeSeriesFetched = true;
                    }
                }
                catch (Throwable e)
                {
                    // Ignore failures.
                }
            }

            return pendingTimeSeries;
        }

        void invalidatePendingTimeSeries()
        {
            if (pendingTimeSeries != null)
            {
                pendingTimeSeries.close();
                pendingTimeSeries = null;
            }

            pendingTimeSeriesFetched = false;
        }

        MetricsEngineValueSeries evaluateSynthetic(SessionProvider sessionProvider,
                                                   ZonedDateTime rangeStart,
                                                   ZonedDateTime rangeEnd)
        {
            MetricsBindingSpooler spooler = sessionProvider.getServiceNonNull(MetricsBindingSpooler.class);
            return spooler.evaluate(sessionProvider, sysId, rangeStart, rangeEnd);
        }
    }

    private class ArchiveLru extends BaseLru<DeviceElementSampleRecord>
    {
        private final String          sysId_element;
        private       TimeSeries.Lazy ts;

        private ArchiveLru(String sysId_element,
                           String sysId)
        {
            super(sysId);

            this.sysId_element = sysId_element;
            System.out.println("ArchiveLru  222" +this.sysId_element);
        }

        void initialize()
        {
            m_pendingArchives.queue(this);
            System.out.println("ArchiveLru  initialize" +this.sysId_element);
        }

        @Override
        void completeInitialization(SessionHolder sessionHolder,
                                    DeviceElementSampleRecord rec)
        {
        	 System.out.println("ArchiveLru  completeInitialization" +this.sysId_element);
        	
            if (rec != null)
            {
           	 System.out.println("ArchiveLru  completeInitialization 1" +this.sysId_element);

                ts = rec.getLazyTimeSeries();
                System.out.println("ArchiveLru  completeInitialization 2" +ts);
            }

            markAsReady(rec != null);
            System.out.println("ArchiveLru  completeInitialization 3" +this.sysId_element);
        }
    }

    public enum StreamNextAction
    {
        Continue,
        Exit,
        Done,
    }

    //--//

    public static final Logger LoggerInstance = new Logger(SamplesCache.class);

    private static final Duration MaxAgeForSummaries = Duration.of(4, ChronoUnit.HOURS);
    private static final Duration MaxAgeForArchives  = Duration.of(30, ChronoUnit.MINUTES);

    private final HubApplication                        m_app;
    private final SessionProvider                       m_sessionProvider;
    private final Object                                m_lock     = new Object();
    private final MapWithSoftValues<String, ElementLru> m_elements = new MapWithSoftValues<>();
    private final MapWithSoftValues<String, ArchiveLru> m_archives = new MapWithSoftValues<>();

    private final LruFetchQueue<DeviceElementRecord>       m_pendingElements = new LruFetchQueue<>(DeviceElementRecord.class);
    private final LruFetchQueue<DeviceElementSampleRecord> m_pendingArchives = new LruFetchQueue<>(DeviceElementSampleRecord.class);

    private DatabaseActivity.LocalSubscriber m_regDbActivity;
    private GcTracker.Holder                 m_gcNotifier;

    //--//

    public SamplesCache(HubApplication app)
    {
        m_app             = app;
        m_sessionProvider = new SessionProvider(app, null, Optio3DbRateLimiter.Normal);

        app.registerService(SamplesCache.class, () -> this);
    }

    public void initialize()
    {
        m_regDbActivity = DatabaseActivity.LocalSubscriber.create(m_app.getServiceNonNull(MessageBusBroker.class));

        m_regDbActivity.subscribeToTable(LocationRecord.class, (dbEvent) ->
        {
            switch (dbEvent.action)
            {
                case UPDATE_DIRECT:
                    // We get the timezone from the location, invalidate everything.
                    invalidateAllSummaries();
                    break;
            }
        });

        m_regDbActivity.subscribeToTable(DeviceElementRecord.class, (dbEvent) ->
        {
            switch (dbEvent.action)
            {
                case UPDATE_DIRECT:
                case UPDATE_INDIRECT:
                case DELETE:
                    invalidateSummary(dbEvent.context.sysId);
                    break;
            }
        });

        m_regDbActivity.subscribeToTable(DeviceElementSampleRecord.class, (dbEvent) ->
        {
            switch (dbEvent.action)
            {
                case UPDATE_DIRECT:
                case DELETE:
                    invalidateArchive(dbEvent.context.sysId);
                    break;
            }
        });

        m_gcNotifier = GcTracker.register((freeMemory, totalMemory, maxMemory) ->
                                          {
                                              float heapUtilization = 100.0f * (totalMemory - freeMemory) / maxMemory;

                                              BaseLru.purge(m_elements, heapUtilization, 75.0f, 10f);
                                              BaseLru.purge(m_archives, heapUtilization, 70.0f, 10f);
                                          });
    }

    public void close()
    {
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

    public <T> T getSample(String sysId,
                           ZonedDateTime timestamp,
                           String prop,
                           EngineeringUnitsFactors convertTo,
                           boolean nearest,
                           boolean onlyBeforeTarget,
                           Class<T> clz,
                           Duration maxWaitForSpooler)
    {
        ElementLru elementLru = fetchElementLru(sysId);
        if (elementLru != null)
        {
            if (elementLru.isSynthetic)
            {
                MetricsEngineValueSeries output = elementLru.evaluateSynthetic(m_sessionProvider, timestamp.minusDays(1), timestamp.plusDays(1));
                if (output != null)
                {
                    double[] timestamps = output.values.timestamps;
                    int      count      = timestamps.length;

                    int pos = Arrays.binarySearch(timestamps, 0, count, TimeUtils.fromUtcTimeToTimestamp(timestamp));
                    if (pos < 0)
                    {
                        pos = ~pos;
                    }

                    if (pos < count)
                    {
                        return elementLru.summary.convertIfNeeded(prop, convertTo, clz, output.fetchNthValue(pos, clz));
                    }
                }
            }
            else
            {
                TimeSeries extraTs = elementLru.fetchPendingSamples(m_app, maxWaitForSpooler);
                if (extraTs != null)
                {
                    T val = extraTs.getSample(timestamp, prop, nearest, onlyBeforeTarget, clz);
                    if (val != null)
                    {
                        return elementLru.summary.convertIfNeeded(prop, convertTo, clz, val);
                    }
                }

                //
                // Iterate from the newest to the oldest archive.
                //
                for (var it = elementLru.summary.getIterator(true); it.hasNext(); )
                {
                    DeviceElementRecord.ArchiveSummary.Range summaryRange = it.next();

                    if (summaryRange.isTimestampCoveredByStart(timestamp) && summaryRange.isTimestampCoveredByEnd(timestamp))
                    {
                        TimeSeries ts = fetchArchive(summaryRange.sysId_element, summaryRange.sysId_archive);
                        if (ts != null)
                        {
                            T val = ts.getSample(timestamp, prop, nearest, onlyBeforeTarget, clz);
                            if (val != null)
                            {
                                return elementLru.summary.convertIfNeeded(prop, convertTo, clz, val);
                            }
                        }

                        return null;
                    }
                }
            }
        }

        return null;
    }

    //--//

    public interface IInjector
    {
        boolean inject(ZonedDateTime timestamp,
                       JsonNode value,
                       EngineeringUnitsFactors convertFrom) throws
                                                            Exception;

        boolean flush() throws
                        Exception;
    }

    @FunctionalInterface
    public interface InjectorValidator
    {
        boolean accept(DeviceElementRecord rec_element,
                       TimeSeriesPropertyType pt,
                       BaseObjectModel model);
    }

    public IInjector buildSampleInjector(InjectorValidator validator,
                                         String sysId,
                                         String prop) throws
                                                      IOException
    {
    	System.out.println("nbuildSampleInjector 1" );
        ElementLru elementLru = fetchElementLru(sysId);
        if (elementLru != null && !elementLru.isSynthetic && elementLru.extractor != null)
        {
        	System.out.println("nbuildSampleInjector 2 " +prop);
        	
        
            TimeSeriesPropertyType pt = elementLru.classifyWithoutPresentationType.get(prop);
            System.out.println("nbuildSampleInjector 2" +elementLru.classifyWithoutPresentationType );
            if (pt != null)
            {
            	System.out.println("nbuildSampleInjector 3" );
                IProtocolDecoder decoder = elementLru.extractor.getProtocolDecoder();
             //   if (decoder != null && decoder.getRootSelector() == GatewayDiscoveryEntitySelector.Network)
                {
                	System.out.println("nbuildSampleInjector 4" );
                    try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
                    {
                    	System.out.println("nbuildSampleInjector 6" );
                        DeviceElementRecord rec_element = sessionHolder.getEntityOrNull(DeviceElementRecord.class, sysId);
                        System.out.println("nbuildSampleInjector 7" );
                        if (rec_element != null)
                        {
                        	System.out.println("nbuildSampleInjector 8" );
                            BaseObjectModel currentModel = rec_element.getContentsAsObject(false);
                            System.out.println("nbuildSampleInjector 8"+currentModel );
                            if (currentModel != null)
                            {
                            	System.out.println("nbuildSampleInjector 9" );
                                if (validator == null || validator.accept(rec_element, pt, currentModel))
                                {
                                	System.out.println("nbuildSampleInjector 10" );
                                    DeviceRecord rec_device = rec_element.findParentAssetRecursively(DeviceRecord.class);
                                    if (rec_device != null)
                                    {
                                    	System.out.println("nbuildSampleInjector 11" );
                                        NetworkAssetRecord rec_network = rec_device.findParentAssetRecursively(NetworkAssetRecord.class);
                                        if (rec_network != null)
                                        {
                                        	System.out.println("nbuildSampleInjector 12" );
                                            String              network_SysId     = rec_network.getSysId();
                                            BaseAssetDescriptor deviceDesc        = rec_device.getIdentityDescriptor();
                                            String              elementIdentifier = rec_element.getIdentifier();

                                            Class<?>       expectedClass = pt.getExpectedBoxedType();
                                            TypeDescriptor td            = Reflection.getDescriptor(expectedClass);

                                            return new IInjector()
                                            {
                                            	
                                                private final List<GatewayDiscoveryEntity> m_batch = Lists.newArrayList();
                                                private GatewayDiscoveryEntity m_en_network;
                                                private GatewayDiscoveryEntity m_en_protocol;
                                                private GatewayDiscoveryEntity m_en_device;

                                                @Override
                                                public boolean inject(ZonedDateTime timestamp,
                                                                      JsonNode value,
                                                                      EngineeringUnitsFactors convertFrom) throws
                                                                                                           Exception
                                                {
                                                	
                                                    System.out.println("new value 0");
                                                    prepareBatch();
                                                    System.out.println("new value 1");
                                                    double timestampEpochSeconds = TimeUtils.fromUtcTimeToTimestamp(timestamp);
                                                    Object valueRaw;

                                                    if (td != null)
                                                    {
                                                    	System.out.println("new value 2");
                                                        double valueDouble = value.asDouble();

                                                        if (convertFrom != null && !EngineeringUnitsFactors.areIdentical(convertFrom, pt.unitsFactors))
                                                        {
                                                        	System.out.println("new value 3");
                                                        	EngineeringUnitsConverter converter = EngineeringUnits.buildConverter(convertFrom, pt.unitsFactors);

                                                            valueDouble = converter.convert(valueDouble);
                                                        }

                                                        valueRaw = Reflection.coerceNumber(valueDouble, expectedClass);
                                                    }
                                                    else if (expectedClass.isEnum())
                                                    {
                                                    	System.out.println("new value 4");
                                                        valueRaw = resolveEnum(value.asText(), expectedClass);
                                                    }
                                                    else if (expectedClass == String.class)
                                                    {
                                                    	System.out.println("new value 5");
                                                        valueRaw = value.asText();
                                                    }
                                                    else
                                                    {
                                                    	System.out.println("new value 6");
                                                        return false;
                                                    }

                                                    BaseObjectModel obj = BaseObjectModel.copySingleProperty(currentModel, null);

                                                    BACnetObjectModel objBACnet = Reflection.as(obj, BACnetObjectModel.class);
                                                    if (objBACnet != null)
                                                    {
                                                    	System.out.println("new value 7");
                                                        var parsedProp = BACnetPropertyIdentifier.parse(prop);
                                                        if (parsedProp == BACnetPropertyIdentifier.present_value)
                                                        {
                                                            objBACnet.setValue(parsedProp, valueRaw);
                                                            objBACnet.setValue(BACnetPropertyIdentifier.event_state, BACnetEventState.normal);
                                                            objBACnet.setValue(BACnetPropertyIdentifier.out_of_service, false);
                                                            objBACnet.setValue(BACnetPropertyIdentifier.status_flags, new BACnetStatusFlags());
                                                        }

                                                        obj = objBACnet;
                                                    }
                                                    else
                                                    {
                                                    	System.out.println("new value 8");
                                                        obj.setField(pt.targetField, valueRaw);
                                                    }

                                                    GatewayDiscoveryEntity en_object       = InstanceConfiguration.newDiscoveryEntry(m_en_device, decoder.getObjectSelector(), elementIdentifier);
                                                    GatewayDiscoveryEntity en_objectSample = InstanceConfiguration.newDiscoveryEntry(en_object, decoder.getSampleSelector(), null);
                                                    en_objectSample.setTimestampEpoch(timestampEpochSeconds);
                                                    en_objectSample.contents = obj.serializeToJson();
                                                    System.out.println("new value 9");
                                                    return true;
                                                }

                                                @Override
                                                public boolean flush() throws
                                                                       Exception
                                                {
                                                    if (m_en_network != null)
                                                    {
                                                        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
                                                        {
                                                            ResultStagingRecord.queue(sessionHolder.createHelper(ResultStagingRecord.class), m_batch);

                                                            sessionHolder.commit();
                                                        }

                                                        m_batch.clear();
                                                        m_en_network  = null;
                                                        m_en_protocol = null;
                                                        m_en_device   = null;
                                                    }

                                                    return true;
                                                }

                                                private void prepareBatch() throws
                                                                            Exception
                                                {
                                                    if (m_en_network == null)
                                                    {
                                                        m_en_network  = InstanceConfiguration.newDiscoveryEntry(null, GatewayDiscoveryEntitySelector.Network, network_SysId);
                                                        m_en_protocol = InstanceConfiguration.newDiscoveryEntry(m_en_network, GatewayDiscoveryEntitySelector.Protocol, decoder.getProtocolValue());
                                                        m_en_device   = InstanceConfiguration.newDiscoveryEntry(m_en_protocol, decoder.getDeviceSelector(), deviceDesc);

                                                        m_batch.add(m_en_network);
                                                    }
                                                }

                                                private <T extends Enum<T>> T resolveEnum(String enumValue,
                                                                                          Class<?> clz)
                                                {
                                                    @SuppressWarnings("unchecked") Class<T> clzEnum = (Class<T>) clz;

                                                    return Enum.valueOf(clzEnum, enumValue);
                                                }
                                            };
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    //--//

    public <T> TimeSeriesExtract<T> extractSamples(String sysId,
                                                   String prop,
                                                   EngineeringUnitsFactors convertTo,
                                                   boolean skipMissingValues,
                                                   ZonedDateTime rangeStart,
                                                   ZonedDateTime rangeEnd,
                                                   Class<T> expectedType,
                                                   Duration maxWaitForSpooler)
    {
    	
    	System.out.println("extractSamples 1");
        TimeSeriesExtract<T> extract = new TimeSeriesExtract<>(expectedType);

        ElementLru elementLru = fetchElementLru(sysId);
        if (elementLru == null)
        {
            extract.unknownProperty = true;
        }
        else
        {
        	System.out.println("extractSamples 2");
            if (elementLru.isSynthetic)
            {
                MetricsEngineValueSeries output = elementLru.evaluateSynthetic(m_sessionProvider, rangeStart, rangeEnd);
                if (output != null)
                {
                    output.toExtract(extract);
                }
            }
            else
            {
            	System.out.println("extractSamples 3");
                extract.timeZone = elementLru.timeZone;

                streamArchiveRanges(elementLru.summary, rangeStart, rangeEnd, false, (summaryRange) ->
                {
                    ArchiveLru lru = fetchArchiveLru(sysId, summaryRange.sysId_archive);
                    if (lru != null)
                    {
                        TimeSeries ts = lru.ts.get();
                        if (ts != null)
                        {
                            ts.extractSamples(extract, prop, skipMissingValues, rangeStart, rangeEnd);
                        }
                    }

                    return StreamNextAction.Continue;
                });

                TimeSeries extraTs = elementLru.fetchPendingSamples(m_app, maxWaitForSpooler);
            	System.out.println("extractSamples 4");
                if (extraTs != null)
                {
                	System.out.println("extractSamples 5");
                    ZonedDateTime extraRangeStart = rangeStart;
                    ZonedDateTime extraRangeEnd   = rangeEnd;

                    //
                    // If we already collected samples, we don't want to have duplicates.
                    // So restrict the extra range to be outside the already collected one.
                    //
                    ZonedDateTime lastTimestamp = TimeUtils.fromTimestampToUtcTime(extract.getLastTimestamp());
                    if (lastTimestamp != null)
                    {
                        // Since ranges are inclusive, move to one seconds after the last timestamp.
                        lastTimestamp = lastTimestamp.plus(1, ChronoUnit.SECONDS);

                        extraRangeStart = TimeUtils.max(extraRangeStart, lastTimestamp);

                        if (extraRangeEnd != null)
                        {
                            extraRangeEnd = TimeUtils.max(extraRangeEnd, lastTimestamp);
                        }
                    }

                    extraTs.extractSamples(extract, prop, skipMissingValues, extraRangeStart, extraRangeEnd);
                }
            }

            elementLru.summary.convertIfNeeded(prop, convertTo, extract);
        }

        return extract;
    }

    //--//

    public TimeSeries.NumericValueRanges collectSampleRanges(String sysId,
                                                             String prop,
                                                             EngineeringUnitsFactors convertTo,
                                                             ZonedDateTime rangeStart,
                                                             ZonedDateTime rangeEnd,
                                                             Duration maxWaitForSpooler)
    {
        Class<?> expectedType = extractExpectedType(sysId, prop, false);
        if (expectedType == null)
        {
            return null;
        }

        try (TimeSeriesExtract<?> extract = extractSamples(sysId, prop, convertTo, false, rangeStart, rangeEnd, expectedType, maxWaitForSpooler))
        {
            TimeSeries.NumericValueRanges res = new TimeSeries.NumericValueRanges();
            res.importValues(extract);

            return res;
        }
    }

    //--//

    public Map<String, TimeSeriesPropertyType> extractClassification(String sysId,
                                                                     boolean handlePresentationType)
    {
    	
    	System.out.println("TimeSeriesSchemaResponse extractClassification" +sysId); 
        ElementLru elementLru = fetchElementLru(sysId);
        System.out.println("TimeSeriesSchemaResponse elementLru" +elementLru); 
        
        return elementLru != null ? (handlePresentationType ? elementLru.classifyAsPresentationType : elementLru.classifyWithoutPresentationType) : null;
    }

    public int extractSamplingPeriod(String sysId,
                                     String prop)
    {
        ElementLru elementLru = fetchElementLru(sysId);
        if (elementLru != null && elementLru.samplingSettings != null)
        {
            for (DeviceElementSampling samplingSetting : elementLru.samplingSettings)
            {
                if (StringUtils.equals(samplingSetting.propertyName, prop))
                {
                    return samplingSetting.samplingPeriod;
                }
            }
        }

        return -1;
    }

    public Class<?> extractExpectedType(String sysId,
                                        String prop,
                                        boolean handlePresentationType)
    {
        Map<String, TimeSeriesPropertyType> map = extractClassification(sysId, handlePresentationType);
        return AssetRecord.PropertyTypeExtractor.inferExpectedType(map, prop);
    }

    //--//

    public ExpandableArrayOfDoubles extractTimestamps(String sysId,
                                                      ZonedDateTime rangeStart,
                                                      ZonedDateTime rangeEnd,
                                                      Duration maxWaitForSpooler)
    {
        ExpandableArrayOfDoubles timestamps = ExpandableArrayOfDoubles.create();

        double start = rangeStart != null ? TimeUtils.fromUtcTimeToTimestamp(rangeStart) : TimeUtils.minEpochSeconds();
        double end   = rangeEnd != null ? TimeUtils.fromUtcTimeToTimestamp(rangeEnd) : TimeUtils.maxEpochSeconds();

        ElementLru elementLru = fetchElementLru(sysId);
        if (elementLru != null)
        {
            streamArchives(elementLru.summary, rangeStart, rangeEnd, false, (rec, ts) ->
            {
                collectTimestamps(timestamps, start, end, ts);

                return SamplesCache.StreamNextAction.Continue;
            });

            TimeSeries extraTs = elementLru.fetchPendingSamples(m_app, maxWaitForSpooler);
            if (extraTs != null)
            {
                collectTimestamps(timestamps, start, end, extraTs);
            }
        }

        return timestamps;
    }

    private void collectTimestamps(ExpandableArrayOfDoubles unsortedTimestamps,
                                   double start,
                                   double end,
                                   TimeSeries ts)
    {
        double newestTimestamp = unsortedTimestamps.get(unsortedTimestamps.size() - 1, TimeUtils.minEpochSeconds());

        ExpandableArrayOfDoubles timestamps    = ts.getTimeStampsAsEpochSeconds();
        int                      numTimestamps = timestamps.size();

        for (int index = 0; index < numTimestamps; index++)
        {
            double timeStampsAsEpochSecond = timestamps.get(index, Double.NaN);

            if (start <= timeStampsAsEpochSecond && timeStampsAsEpochSecond <= end)
            {
                if (newestTimestamp < timeStampsAsEpochSecond)
                {
                    unsortedTimestamps.add(timeStampsAsEpochSecond);
                    newestTimestamp = timeStampsAsEpochSecond;
                }
                else if (newestTimestamp > timeStampsAsEpochSecond)
                {
                    int insertionPos = unsortedTimestamps.binarySearch(timeStampsAsEpochSecond);
                    if (insertionPos < 0) // If pos is non-negative, the value is already in the array.
                    {
                        unsortedTimestamps.insert(~insertionPos, timeStampsAsEpochSecond);
                    }
                }
            }
        }
    }

    public StreamNextAction streamArchives(String sysId,
                                           ZonedDateTime rangeStart,
                                           ZonedDateTime rangeEnd,
                                           boolean reverseOrder,
                                           BiFunctionWithException<String, TimeSeries, StreamNextAction> callback)
    {
        ElementLru elementLru = fetchElementLru(sysId);
        if (elementLru != null)
        {
            return streamArchives(elementLru.summary, rangeStart, rangeEnd, reverseOrder, callback);
        }

        return null;
    }

    private StreamNextAction streamArchives(DeviceElementRecord.ArchiveSummary summary,
                                            ZonedDateTime rangeStart,
                                            ZonedDateTime rangeEnd,
                                            boolean reverseOrder,
                                            BiFunctionWithException<String, TimeSeries, StreamNextAction> callback)
    {
        return streamArchiveRanges(summary, rangeStart, rangeEnd, reverseOrder, (summaryRange) ->
        {
            TimeSeries ts = fetchArchive(summaryRange.sysId_element, summaryRange.sysId_archive);
            if (ts != null)
            {
                try
                {
                    StreamNextAction result = callback.apply(summaryRange.sysId_archive, ts);
                    if (result != StreamNextAction.Continue)
                    {
                        return result;
                    }
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }

            return StreamNextAction.Continue;
        });
    }

    //--//

    public StreamNextAction streamArchiveRanges(String sysId,
                                                ZonedDateTime rangeStart,
                                                ZonedDateTime rangeEnd,
                                                boolean reverseOrder,
                                                FunctionWithException<DeviceElementRecord.ArchiveSummary.Range, StreamNextAction> callback)
    {
        ElementLru elementLru = fetchElementLru(sysId);
        if (elementLru != null)
        {
            return streamArchiveRanges(elementLru.summary, rangeStart, rangeEnd, reverseOrder, callback);
        }

        return null;
    }

    private StreamNextAction streamArchiveRanges(DeviceElementRecord.ArchiveSummary summary,
                                                 ZonedDateTime rangeStart,
                                                 ZonedDateTime rangeEnd,
                                                 boolean reverseOrder,
                                                 FunctionWithException<DeviceElementRecord.ArchiveSummary.Range, StreamNextAction> callback)
    {
        for (var it = summary.getIterator(reverseOrder); it.hasNext(); )
        {
            DeviceElementRecord.ArchiveSummary.Range summaryRange = it.next();

            if (rangeStart != null && !summaryRange.isTimestampCoveredByEnd(rangeStart))
            {
                continue;
            }

            if (rangeEnd != null && !summaryRange.isTimestampCoveredByStart(rangeEnd))
            {
                continue;
            }

            try
            {
                StreamNextAction result = callback.apply(summaryRange);
                if (result != StreamNextAction.Continue)
                {
                    return result;
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    //--//
    
    private void fetchElementLru()
    {
        ElementLru lru;
        System.out.println("099090");

        synchronized (m_lock)
        {
        	
        	System.out.println("099090");
        	Set<String> keyset = m_elements.keySet();
        	for(String s : keyset)
        	System.out.println(s);
            
        }
        

        	
    }

    private ElementLru fetchElementLru(String sysId)
    {
    	
    	fetchElementLru();
        ElementLru lru;

        synchronized (m_lock)
        {
        	
            lru = m_elements.get(sysId);
            if (lru == null)
            {
                lru = new ElementLru(sysId);
                lru.initialize();

                m_elements.put(sysId, lru);
            }
        }

        if (!lru.waitForReady())
        {
            return null;
        }

        lru.touched();

        return lru;
    }

    private TimeSeries fetchArchive(String sysId_element,
                                    String sysId_archive)
    {
        ArchiveLru lru = fetchArchiveLru(sysId_element, sysId_archive);

        return lru != null ? lru.ts.get() : null;
    }

    private ArchiveLru fetchArchiveLru(String sysId_element,
                                       String sysId_archive)
    {
        ArchiveLru lru;

        synchronized (m_lock)
        {
        	
            lru = m_archives.get(sysId_archive);
            if (lru == null)
            {
                lru = new ArchiveLru(sysId_element, sysId_archive);
                lru.initialize();

                m_archives.put(sysId_archive, lru);
            }
        }

        if (!lru.waitForReady())
        {
            return null;
        }

        lru.touched();

        return lru;
    }

    //--//

    public void invalidate(DeviceElementRecord rec)
    {
        invalidateSummary(rec.getSysId());
    }

    public void invalidate(DeviceElementSampleRecord rec)
    {
        invalidateArchive(rec.getSysId());
    }

    //--//

    private void invalidateAllSummaries()
    {
        synchronized (m_lock)
        {
            m_elements.clear();
        }
    }

    private void invalidateSummary(String sysId)
    {
        synchronized (m_lock)
        {
            ElementLru element = removeFromLru(m_elements, sysId);
            if (element != null)
            {
                element.invalidatePendingTimeSeries();
            }
        }
    }

    private void invalidateArchive(String sysId)
    {
        synchronized (m_lock)
        {
            removeFromLru(m_archives, sysId);
        }
    }

    private <T extends BaseLru<?>> T removeFromLru(MapWithSoftValues<String, T> map,
                                                   String sysId)
    {
        T val = map.remove(sysId);
        if (val != null)
        {
            val.markAsReady(false);

            if (val.waitForReady())
            {
                return val;
            }
        }

        return null;
    }
}
