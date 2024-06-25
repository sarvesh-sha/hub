/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.HubDataDefinition;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementSampleRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import com.optio3.util.TimeUtils;
import org.junit.ClassRule;
import org.junit.Test;

public class TimeSeriesInDb2Test extends Optio3Test
{
    static String id_root;
    static String id_child;

    @ClassRule
    public static final TestApplicationWithDbRule<HubApplication, HubConfiguration> applicationRule = new TestApplicationWithDbRule<>(HubApplication.class, "hub-test.yml", (configuration) ->
    {
        configuration.data = new HubDataDefinition[] { new HubDataDefinition("demodata/defaultUsers.json", false, false) };
    }, null);

    @Test
    @TestOrder(10)
    public void testSetup() throws
                            Exception
    {
        try (SessionHolder sessionHolder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<AssetRecord> helper_asset = sessionHolder.createHelper(AssetRecord.class);

            DeviceRecord rec_root = new DeviceRecord();
            rec_root.setFirmwareVersion("root1");

            sessionHolder.persistEntity(rec_root);
            id_root = rec_root.getSysId();
            System.out.printf("id_root: %s%n", id_root);
            ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_root);

            DeviceElementRecord rec_child = new DeviceElementRecord();
            rec_child.setIdentifier("child");
            sessionHolder.persistEntity(rec_child);
            id_child = rec_child.getSysId();
            System.out.printf("id_child1: %s%n", id_child);
            ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_child);
            rec_child.linkToParent(helper_asset, rec_root);

            sessionHolder.commit();
        }
    }

    static ZonedDateTime sample_time;

    @Test
    @TestOrder(20)
    public void testCreateSeries() throws
                                   Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<DeviceElementRecord>       helper       = holder.createHelper(DeviceElementRecord.class);
            RecordHelper<DeviceElementSampleRecord> helperSample = holder.createHelper(DeviceElementSampleRecord.class);

            DeviceElementRecord rec_child = helper.get(id_child);

            sample_time = TimeUtils.now();

            try (DeviceElementRecord.ArchiveDescriptor lazy_archive = rec_child.ensureArchive(helperSample, TimeUtils.fromUtcTimeToTimestamp(sample_time)))
            {
                TimeSeries ts = lazy_archive.getTimeSeries();
                generateSeries(sample_time, ts, 0);
            }

            holder.commit();
        }
    }

    @Test
    @TestOrder(21)
    public void testCheckSeries() throws
                                  Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<DeviceElementRecord>       helper       = holder.createHelper(DeviceElementRecord.class);
            RecordHelper<DeviceElementSampleRecord> helperSample = holder.createHelper(DeviceElementSampleRecord.class);

            DeviceElementRecord rec_child = helper.get(id_child);

            // There's an open archive at this timestamp, this should return non-null.
            DeviceElementRecord.ArchiveDescriptor lazy_archive = rec_child.getArchive(helperSample, sample_time);
            assertNotNull(lazy_archive);
        }
    }

    @Test
    @TestOrder(22)
    public void testFindSeries() throws
                                 Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<DeviceElementRecord>       helper       = holder.createHelper(DeviceElementRecord.class);
            RecordHelper<DeviceElementSampleRecord> helperSample = holder.createHelper(DeviceElementSampleRecord.class);

            DeviceElementRecord rec_child = helper.get(id_child);

            ZonedDateTime sample_time__minus1Hour = sample_time.minus(1, ChronoUnit.HOURS);
            ZonedDateTime sample_time__minus1Sec  = sample_time.minus(1, ChronoUnit.SECONDS);
            ZonedDateTime sample_time__plus1Hour  = sample_time.plus(1, ChronoUnit.HOURS);

            // There's an open archive after this timestamp, this should return 1 archive.
            assertEquals(1, countArchives(rec_child, helperSample, sample_time__minus1Hour, null));

            // The start of an open archive matches this timestamp, this should return 1 archive.
            assertEquals(1, countArchives(rec_child, helperSample, sample_time__minus1Hour, sample_time));

            // The start of an open archive is just after this timestamp, this should return 0 archive.
            assertEquals(0, countArchives(rec_child, helperSample, sample_time__minus1Hour, sample_time__minus1Sec));

            // There's an open archive at this timestamp, this should return 1 archive.
            assertEquals(1, countArchives(rec_child, helperSample, sample_time, null));

            // There's an open archive, this should return 1 archive.
            assertEquals(1, countArchives(rec_child, helperSample, sample_time__plus1Hour, null));
        }
    }

    @Test
    @TestOrder(30)
    public void testCloseSeries() throws
                                  Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<DeviceElementRecord>       helper       = holder.createHelper(DeviceElementRecord.class);
            RecordHelper<DeviceElementSampleRecord> helperSample = holder.createHelper(DeviceElementSampleRecord.class);

            DeviceElementRecord rec_child = helper.get(id_child);

            try (DeviceElementRecord.ArchiveDescriptor lazy_archive = rec_child.getArchive(helperSample, sample_time))
            {
                assertNotNull(lazy_archive);
                DeviceElementSampleRecord rec_archive = lazy_archive.getRecord();
                assertNotNull(rec_archive);
            }

            holder.commit();
        }
    }

    @Test
    @TestOrder(31)
    public void testFindSeries2() throws
                                  Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<DeviceElementRecord>       helper       = holder.createHelper(DeviceElementRecord.class);
            RecordHelper<DeviceElementSampleRecord> helperSample = holder.createHelper(DeviceElementSampleRecord.class);

            DeviceElementRecord rec_child = helper.get(id_child);

            ZonedDateTime sample_time__minus1Hour = sample_time.minus(1, ChronoUnit.HOURS);
            ZonedDateTime sample_time__minus1Sec  = sample_time.minus(1, ChronoUnit.SECONDS);
            ZonedDateTime sample_time__plus1Hour  = sample_time.plus(1, ChronoUnit.HOURS);

            // There's an archive after this timestamp, this should return 1 archive.
            assertEquals(1, countArchives(rec_child, helperSample, sample_time__minus1Hour, null));

            // The start of an archive matches this timestamp, this should return 1 archive.
            assertEquals(1, countArchives(rec_child, helperSample, sample_time__minus1Hour, sample_time));

            // The start of an archive is just after this timestamp, this should return 0 archive.
            assertEquals(0, countArchives(rec_child, helperSample, sample_time__minus1Hour, sample_time__minus1Sec));

            // There's an archive at this timestamp, this should return 1 archive.
            assertEquals(1, countArchives(rec_child, helperSample, sample_time, null));

            // There's an archive before this range, this should return 1 archive.
            assertEquals(1, countArchives(rec_child, helperSample, sample_time__plus1Hour, null));
        }
    }

    //--//

    private void generateSeries(ZonedDateTime t,
                                TimeSeries ts,
                                int bias)
    {
        BitSet bs = new BitSet();

        for (int num = 0; num < 100; num++)
        {
            int bitPosition = num % 11 + (bias % 5);

            if ((num % 3) == 0)
            {
                bs.clear(bitPosition);
            }
            else
            {
                bs.set(bitPosition);
            }

            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propFloat", TimeSeries.SampleType.Decimal, 3, 1.01f + num * 19.7f + bias);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propDouble", TimeSeries.SampleType.Decimal, 3, 1.001 + num * 19.7 + bias);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propSigned", TimeSeries.SampleType.Integer, 0, -1200 + num * 30 + bias);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propUnsigned", TimeSeries.SampleType.Integer, 0, 1200 + num * 30 + bias);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propBitSet", TimeSeries.SampleType.BitSet, 0, bs);

            if (num == 10)
            {
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t.plus(3, ChronoUnit.SECONDS), "propFloat", TimeSeries.SampleType.Decimal, 0, 200000);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t.plus(3, ChronoUnit.SECONDS), "propDouble", TimeSeries.SampleType.Decimal, 0, 200000);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t.plus(4, ChronoUnit.SECONDS), "propFloat", TimeSeries.SampleType.Decimal, 0, 200000);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t.plus(4, ChronoUnit.SECONDS), "propDouble", TimeSeries.SampleType.Decimal, 0, 200000);
            }

            if (num == 20)
            {
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t.plus(3, ChronoUnit.SECONDS), "propSigned", TimeSeries.SampleType.Integer, 0, -1200 + num * 30 + bias);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t.plus(3, ChronoUnit.SECONDS), "propUnsigned", TimeSeries.SampleType.Integer, 0, 1200 + num * 30 + bias);
            }

            t = t.plus(10, ChronoUnit.SECONDS);
        }
    }

    private int countArchives(DeviceElementRecord rec,
                              RecordHelper<DeviceElementSampleRecord> helper,
                              ZonedDateTime rangeStart,
                              ZonedDateTime rangeEnd) throws
                                                      Exception
    {
        AtomicInteger count = new AtomicInteger();

        rec.filterArchives(helper, rangeStart, rangeEnd, false, (desc) ->
        {
            count.incrementAndGet();
            return null;
        }, null);

        return count.get();
    }
}
