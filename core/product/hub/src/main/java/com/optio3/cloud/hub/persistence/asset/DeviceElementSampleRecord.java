/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.asset;

import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Tuple;

import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.logic.samples.SamplesCache;
import com.optio3.cloud.hub.model.asset.DeviceElementFilterRequest;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesExtract;
import com.optio3.cloud.hub.persistence.FixupProcessingRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.persistence.AbstractSelectHelper;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;
import com.optio3.util.Exceptions;
import com.optio3.util.function.BiFunctionWithException;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "ASSET_DEVICE_ELEMENT_SAMPLE")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "DeviceElementSample", model = BaseModel.class, metamodel = DeviceElementSampleRecord_.class)
public class DeviceElementSampleRecord extends RecordWithCommonFields
{
    //
    // This handler goes through all coordinate samples, making sure they are using the variable precision encoding.
    //
    public static class ConvertCoordinatesToVariablePrecision extends FixupProcessingRecord.Handler
    {
        @Override
        public Result process(Logger logger,
                              SessionHolder sessionHolder) throws
                                                           Exception
        {
            final int batchSize = 100;

            class State
            {
                private int elements;
                private int archives;
                private int nextFlush = batchSize;
            }

            RecordHelper<NetworkAssetRecord>  helper_network = sessionHolder.createHelper(NetworkAssetRecord.class);
            RecordHelper<DeviceElementRecord> helper_de      = sessionHolder.createHelper(DeviceElementRecord.class);
            State                             state          = new State();

            for (NetworkAssetRecord rec : helper_network.listAll())
            {
                rec.enumerateChildren(sessionHolder.createHelper(IpnDeviceRecord.class), true, -1, null, (rec_device) ->
                {
                    final DeviceElementFilterRequest filters = DeviceElementFilterRequest.createFilterForParent(rec_device.getSysId());
                    DeviceElementRecord.enumerate(helper_de, true, filters, (rec_object) ->
                    {
                        switch (rec_object.getName())
                        {
                            case "Latitude":
                            case "Longitude":
                                state.archives += convertSamples(sessionHolder, rec_object);
                                state.elements++;

                                if (state.elements + state.archives > state.nextFlush)
                                {
                                    state.nextFlush = state.elements + state.archives + batchSize;

                                    HubApplication.LoggerInstance.info("Converting Coordinates: %d elements, %d archives", state.elements, state.archives);
                                    return StreamHelperNextAction.Continue_Flush_Evict_Commit;
                                }
                                break;
                        }

                        return StreamHelperNextAction.Continue_Flush_Evict;
                    });

                    return StreamHelperNextAction.Continue_Flush_Evict;
                });
            }

            HubApplication.LoggerInstance.info("Converted Coordinates: %d elements, %d archives", state.elements, state.archives);

            return Result.Done;
        }

        private int convertSamples(SessionHolder holder,
                                   DeviceElementRecord rec_object) throws
                                                                   Exception
        {
            class State
            {
                private DeviceElementSampleRecord rec_newSample;
                private int                       count;

                private TimeSeries ensureSample()
                {
                    if (rec_newSample == null)
                    {
                        count++;
                        rec_newSample               = new DeviceElementSampleRecord();
                        rec_newSample.owningElement = rec_object;
                    }

                    return rec_newSample.getTimeSeries();
                }

                private void flush(RecordHelper<DeviceElementSampleRecord> helper_sample)
                {
                    if (rec_newSample != null)
                    {
                        TimeSeries ts = rec_newSample.getTimeSeries();
                        if (ts.numberOfSamples() > 0)
                        {
                            rec_newSample.setTimeSeries(ts);
                            helper_sample.persist(rec_newSample);
                            helper_sample.flushAndEvict(rec_newSample);
                            rec_newSample.unloadTimeSeries();
                        }

                        rec_newSample = null;
                    }
                }
            }

            RecordHelper<DeviceElementSampleRecord> helper = holder.createHelper(DeviceElementSampleRecord.class);

            final State state = new State();

            rec_object.filterArchives(helper, null, null, false, (desc) ->
            {
                TimeSeries ts_dst = state.ensureSample();
                TimeSeries ts_src = desc.getTimeSeries();

                ts_src.copySnapshots(0, ts_src.numberOfSamples(), ts_dst);

                state.rec_newSample.setTimeSeries(ts_dst);

                desc.remove();

                return SamplesCache.StreamNextAction.Continue;
            }, (result) ->
                                      {
                                          state.flush(helper);
                                          return result;
                                      });

            return state.count;
        }
    }

    //--//

    /**
     * Bound to this device element.
     */
    @Optio3ControlNotifications(reason = "We notify the device element explicitly, no need to run queries for this", direct = Notify.NEVER, reverse = Notify.NEVER)
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getOwningElement")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "owning_element", nullable = false, foreignKey = @ForeignKey(name = "OWNING_ELEMENT__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private DeviceElementRecord owningElement;

    @Lob
    @Column(name = "contents")
    private byte[] contents;

    @Transient
    private TimeSeries m_timeSeries;

    //--//

    public DeviceElementSampleRecord()
    {
    }

    public static DeviceElementSampleRecord newInstance(DeviceElementRecord deviceElement)
    {
        DeviceElementSampleRecord rec = new DeviceElementSampleRecord();
        rec.owningElement = deviceElement;

        return rec;
    }

    @Override
    public void onEviction()
    {
        super.onEviction();

        contents = null;

        releaseTimeSeries();
    }

    private void releaseTimeSeries()
    {
        if (m_timeSeries != null)
        {
            m_timeSeries.close();
            m_timeSeries = null;
        }
    }

    //--//

    public DeviceElementRecord getOwningElement()
    {
        return owningElement;
    }

    public int getSizeOfTimeSeries()
    {
        return contents != null ? contents.length : 0;
    }

    public TimeSeries getTimeSeries()
    {
        if (m_timeSeries == null)
        {
            m_timeSeries = TimeSeries.decode(contents);
            if (m_timeSeries == null)
            {
                m_timeSeries = TimeSeries.newInstance();
            }
        }

        return m_timeSeries;
    }

    public boolean setTimeSeries(TimeSeries timeSeries)
    {
        if (m_timeSeries != timeSeries)
        {
            releaseTimeSeries();

            m_timeSeries = timeSeries;
        }

        TimeSeries.Encoded tse         = timeSeries.encode();
        byte[]             newContents = tse != null ? tse.toByteArray() : null;

        timeSeries.resetModified();

        if (!Arrays.equals(newContents, contents))
        {
            contents = newContents;
            return true;
        }

        return false;
    }

    public TimeSeries getReadonlyTimeSeries()
    {
        TimeSeries ts = TimeSeries.decode(contents);
        if (ts == null)
        {
            ts = TimeSeries.newInstance();
        }

        ts.setReadonly();

        return ts;
    }

    public TimeSeries.Lazy getLazyTimeSeries()
    {
        return new TimeSeries.Lazy(contents);
    }

    void unloadTimeSeries()
    {
        if (m_timeSeries != null)
        {
            if (m_timeSeries.wasModified())
            {
                throw Exceptions.newRuntimeException("Can't unload time series '%s', it's dirty", getSysId());
            }

            m_timeSeries = null;
        }
    }

    //--//

    @FunctionalInterface
    public interface FastSampleEnumerationCallback
    {
        SamplesCache.StreamNextAction process(String deviceElementSysId,
                                              TimeSeriesExtract<?> extract);
    }

    public static <T> SamplesCache.StreamNextAction fastExtractSamples(RecordHelper<DeviceElementSampleRecord> helper,
                                                                       String prop,
                                                                       boolean skipMissingValues,
                                                                       Class<T> expectedType,
                                                                       FastSampleEnumerationCallback callback) throws
                                                                                                               Exception
    {
        final int permits = Runtime.getRuntime()
                                   .availableProcessors();

        AtomicReference<SamplesCache.StreamNextAction> nextAction = new AtomicReference<>(SamplesCache.StreamNextAction.Continue);
        Semaphore                                      throttle   = new Semaphore(permits);

        SamplesCache.StreamNextAction finalResult = fastStreamRawArchives(helper, (sysId, contents) ->
        {
            throttle.acquire();

            Executors.getDefaultThreadPool()
                     .execute(() ->
                              {
                                  TimeSeries ts = TimeSeries.decode(contents);
                                  if (ts != null)
                                  {
                                      try (TimeSeriesExtract<?> extract = new TimeSeriesExtract<>(expectedType))
                                      {
                                          ts.extractSamples(extract, prop, skipMissingValues, null, null);

                                          SamplesCache.StreamNextAction result = callback.process(sysId, extract);
                                          if (result != SamplesCache.StreamNextAction.Continue)
                                          {
                                              nextAction.set(result);
                                          }
                                      }
                                  }

                                  throttle.release();
                              });

            return nextAction.get();
        });

        // Drain the queue.
        throttle.acquire(permits);

        return finalResult;
    }

    public static <T> SamplesCache.StreamNextAction fastStreamRawArchives(RecordHelper<DeviceElementSampleRecord> helper,
                                                                          BiFunctionWithException<String, byte[], SamplesCache.StreamNextAction> callback) throws
                                                                                                                                                           Exception
    {
        QueryHelperWithCommonFields<Tuple, DeviceElementSampleRecord> jh = new QueryHelperWithCommonFields<>(helper, Tuple.class);

        jh.cq.multiselect(jh.root.get(DeviceElementSampleRecord_.owningElement)
                                 .get(RecordWithCommonFields_.sysId), jh.root.get(DeviceElementSampleRecord_.contents));

        jh.addOrderBy(jh.root, DeviceElementSampleRecord_.owningElement, true);

        jh.setFetchSize(200);

        try (var scroll = jh.scroll(0))
        {
            while (scroll.next())
            {
                Tuple  row      = scroll.get(0);
                String sysId    = (String) row.get(0);
                byte[] contents = (byte[]) row.get(1);

                switch (callback.apply(sysId, contents))
                {
                    case Done:
                        return SamplesCache.StreamNextAction.Done;

                    case Exit:
                        return SamplesCache.StreamNextAction.Exit;
                }
            }
        }

        return SamplesCache.StreamNextAction.Done;
    }
}
