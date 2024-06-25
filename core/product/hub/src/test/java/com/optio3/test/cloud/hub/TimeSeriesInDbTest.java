/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.BitSet;

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
import org.junit.ClassRule;
import org.junit.Test;

public class TimeSeriesInDbTest extends Optio3Test
{
    static String id_root;
    static String id_child;

    static final ZonedDateTime baseTime = ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 1238, ZoneId.systemDefault());

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

            DeviceElementSampleRecord rec_archive;

            try (DeviceElementRecord.ArchiveDescriptor lazy_archive = rec_child.ensureArchive(helperSample, baseTime))
            {
                rec_archive = lazy_archive.getRecord();

                TimeSeries ts = lazy_archive.getTimeSeries();
                generateSeries(baseTime, ts, 0);
            }

            DeviceElementSampleRecord rec_archive2 = helperSample.get(rec_archive.getSysId());
            assertNotSame(rec_archive, rec_archive2);

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

            // Look for a timestamp that is not at the beginning of the range.
            ZonedDateTime t2 = baseTime.plus(10, ChronoUnit.MINUTES);

            try (DeviceElementRecord.ArchiveDescriptor lazy_archive = rec_child.getArchive(helperSample, t2))
            {
                // There's an open archive at this timestamp, this should return non-null.
                assertNotNull(lazy_archive);

                TimeSeries ts = lazy_archive.getTimeSeries();
                assertEquals(103,
                             ts.getTimeStamps()
                               .size());

                checkSeries(baseTime, ts, 0);
            }
        }
    }

    @Test
    @TestOrder(30)
    public void testAddToSeries() throws
                                  Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<DeviceElementRecord>       helper       = holder.createHelper(DeviceElementRecord.class);
            RecordHelper<DeviceElementSampleRecord> helperSample = holder.createHelper(DeviceElementSampleRecord.class);

            DeviceElementRecord rec_child = helper.get(id_child);

            ZonedDateTime t = baseTime.plus(1, ChronoUnit.DAYS);
            try (DeviceElementRecord.ArchiveDescriptor lazy_archive = rec_child.getArchive(helperSample, t))
            {
                assertNotNull(lazy_archive);

                TimeSeries ts = lazy_archive.getTimeSeries();

                generateSeries(t, ts, -10);
            }

            holder.commit();
        }
    }

    @Test
    @TestOrder(31)
    public void testCheckSeries2() throws
                                   Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<DeviceElementRecord>       helper       = holder.createHelper(DeviceElementRecord.class);
            RecordHelper<DeviceElementSampleRecord> helperSample = holder.createHelper(DeviceElementSampleRecord.class);

            DeviceElementRecord rec_child = helper.get(id_child);

            ZonedDateTime t = baseTime.plus(1, ChronoUnit.DAYS);

            // There's an open archive at this timestamp, this should return non-null.
            DeviceElementRecord.ArchiveDescriptor lazy_archive = rec_child.getArchive(helperSample, t);
            assertNotNull(lazy_archive);

            TimeSeries ts = lazy_archive.getTimeSeries();
            assertEquals(206,
                         ts.getTimeStamps()
                           .size());

            checkSeries(t, ts, -10);
        }
    }

    @Test
    @TestOrder(40)
    public void testAddAgainToSeries() throws
                                       Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<DeviceElementRecord>       helper       = holder.createHelper(DeviceElementRecord.class);
            RecordHelper<DeviceElementSampleRecord> helperSample = holder.createHelper(DeviceElementSampleRecord.class);

            DeviceElementRecord rec_child = helper.get(id_child);

            ZonedDateTime t = baseTime.plus(3, ChronoUnit.DAYS);

            try (DeviceElementRecord.ArchiveDescriptor lazy_archive2 = rec_child.ensureArchive(helperSample, t))
            {
                assertNotNull(lazy_archive2);

                TimeSeries ts = lazy_archive2.getTimeSeries();
                generateSeries(t, ts, 10);
            }

            holder.commit();
        }
    }

    @Test
    @TestOrder(41)
    public void testCheckSeries3() throws
                                   Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<DeviceElementRecord>       helper       = holder.createHelper(DeviceElementRecord.class);
            RecordHelper<DeviceElementSampleRecord> helperSample = holder.createHelper(DeviceElementSampleRecord.class);

            DeviceElementRecord rec_child = helper.get(id_child);

            ZonedDateTime                         t            = baseTime.plus(3, ChronoUnit.DAYS);
            DeviceElementRecord.ArchiveDescriptor lazy_archive = rec_child.getArchive(helperSample, t);
            assertNotNull(lazy_archive);

            TimeSeries ts = lazy_archive.getTimeSeries();
            assertEquals(309,
                         ts.getTimeStamps()
                           .size());

            checkSeries(t, ts, 10);
        }
    }

    @Test
    @TestOrder(50)
    public void testAddAgainToSeriesButInThePast() throws
                                                   Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<DeviceElementRecord>       helper       = holder.createHelper(DeviceElementRecord.class);
            RecordHelper<DeviceElementSampleRecord> helperSample = holder.createHelper(DeviceElementSampleRecord.class);

            DeviceElementRecord rec_child = helper.get(id_child);

            ZonedDateTime t = baseTime.plus(2, ChronoUnit.DAYS);

            try (DeviceElementRecord.ArchiveDescriptor lazy_archive = rec_child.ensureArchive(helperSample, t))
            {
                assertNotNull(lazy_archive);

                TimeSeries ts = lazy_archive.getTimeSeries();

                generateSeries(t, ts, 100);
            }

            holder.commit();
        }
    }

    @Test
    @TestOrder(51)
    public void testCheckSeries4() throws
                                   Exception
    {
        try (SessionHolder holder = applicationRule.openSessionWithoutTransaction())
        {
            RecordHelper<DeviceElementRecord>       helper       = holder.createHelper(DeviceElementRecord.class);
            RecordHelper<DeviceElementSampleRecord> helperSample = holder.createHelper(DeviceElementSampleRecord.class);

            DeviceElementRecord rec_child = helper.get(id_child);

            ZonedDateTime                         t            = baseTime.plus(2, ChronoUnit.DAYS);
            DeviceElementRecord.ArchiveDescriptor lazy_archive = rec_child.getArchive(helperSample, t);
            assertNotNull(lazy_archive);

            TimeSeries ts = lazy_archive.getTimeSeries();
            assertEquals(412,
                         ts.getTimeStamps()
                           .size());

            checkSeries(t, ts, 100);
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

    private void checkSeries(ZonedDateTime t,
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

            assertEquals(1.01f + num * 19.7f + bias, ts.getSample(t, "propFloat", false, false, Double.class), 0.002);
            assertEquals(1.001 + num * 19.7 + bias, ts.getSample(t, "propDouble", false, false, Double.class), 0.002);
            assertEquals(-1200L + num * 30 + bias, (long) ts.getSample(t, "propSigned", false, false, Long.class));
            assertEquals(1200L + num * 30 + bias, (long) ts.getSample(t, "propUnsigned", false, false, Long.class));
            assertEquals(bs, ts.getSample(t, "propBitSet", false, false, BitSet.class));

            t = t.plus(10, ChronoUnit.SECONDS);
        }
    }
}
