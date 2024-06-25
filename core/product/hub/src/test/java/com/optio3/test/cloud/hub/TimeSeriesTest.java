/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.optio3.cloud.hub.model.schedule.DailySchedule;
import com.optio3.cloud.hub.model.schedule.DailyScheduleWithDayOfWeek;
import com.optio3.cloud.hub.model.schedule.RecurringWeeklySchedule;
import com.optio3.cloud.hub.model.schedule.RelativeTimeRange;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesExtract;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyResponse;
import com.optio3.cloud.hub.model.visualization.RangeSelection;
import com.optio3.cloud.hub.model.visualization.TimeRange;
import com.optio3.cloud.hub.model.visualization.TimeRangeId;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.stream.InputBitBuffer;
import com.optio3.stream.OutputBitBuffer;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import com.optio3.util.Resources;
import com.optio3.util.TimeUtils;
import org.junit.Test;

public class TimeSeriesTest extends Optio3Test
{
    @Test
    @TestOrder(10)
    public void testCreation()
    {
        class Generator
        {
            final BitSet bs = new BitSet();
            int num = -1;
            int offset;

            boolean hasNext()
            {
                if (++num >= 100)
                {
                    return false;
                }

                if ((num / 11) % 2 == 1)
                {
                    bs.clear(num % 11);
                }
                else
                {
                    bs.set(num % 11);
                }

                offset = num;

                if (num > 10)
                {
                    offset += 2;
                }

                if (num > 20)
                {
                    offset += 1;
                }

                return true;
            }
        }

        TimeSeries ts = TimeSeries.newInstance();

        ZonedDateTime t = ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 1238, ZoneId.systemDefault());

        BACnetObjectType[] values  = BACnetObjectType.values();
        String[][]         enumSet = new String[][] { { "Test1" }, { "Test1", "Test2" }, { "Test3" }, { "Test1", "Test4" } };

        for (Generator gs = new Generator(); gs.hasNext(); )
        {
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propFloat", TimeSeries.SampleType.Decimal, 3, 1.01f + gs.num * 19.7f);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propDouble", TimeSeries.SampleType.Decimal, 3, 1.001 - gs.num * 19.7);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propFloat2", TimeSeries.SampleType.Decimal, 3, 1.01f + gs.num * .197f);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propDouble2", TimeSeries.SampleType.Decimal, 3, 1.001 - gs.num * .197);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propSigned", TimeSeries.SampleType.Integer, 0, -1200 + gs.num * 30);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propUnsigned", TimeSeries.SampleType.Integer, 0, 1200 - gs.num * 30);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propSigned2", TimeSeries.SampleType.Integer, 0, -1200 + gs.num * 3);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propUnsigned2", TimeSeries.SampleType.Integer, 0, 1200 - gs.num * 3);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propBitSet", TimeSeries.SampleType.BitSet, 0, gs.bs);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propEnum", TimeSeries.SampleType.Enumerated, 0, values[gs.num % values.length]);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propEnumSet", TimeSeries.SampleType.EnumeratedSet, 0, enumSet[gs.num % enumSet.length]);

            if (gs.num == 10)
            {
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t.plus(3, ChronoUnit.SECONDS), "propFloat", TimeSeries.SampleType.Decimal, 0, 20000000);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t.plus(3, ChronoUnit.SECONDS), "propDouble", TimeSeries.SampleType.Decimal, 0, 20000000);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t.plus(4, ChronoUnit.SECONDS), "propFloat", TimeSeries.SampleType.Decimal, 0, 20000000);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t.plus(4, ChronoUnit.SECONDS), "propDouble", TimeSeries.SampleType.Decimal, 0, 20000000);
            }

            if (gs.num == 20)
            {
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t.plus(3, ChronoUnit.SECONDS), "propSigned", TimeSeries.SampleType.Integer, 0, -1200 + gs.num * 30);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t.plus(3, ChronoUnit.SECONDS), "propUnsigned", TimeSeries.SampleType.Integer, 0, 1200 + gs.num * 30);
            }

            t = t.plus(10, ChronoUnit.SECONDS);
        }

        assertNull(ts.getSample(-1, "propFloat", Double.class));
        assertNull(ts.getSample(10000, "propFloat", Double.class));
        assertNull(ts.getSample(0, "unknown", Double.class));

        assertTrue(ts.wasModified());
        OutputBitBuffer blob = ts.encodeUncompressed();
        System.out.printf("Size of blob of %d samples, %d properties: %d bytes (~%d bits per value)%n%n",
                          ts.numberOfSamples(),
                          ts.numberOfProperties(),
                          blob.sizeInBits(),
                          (int) Math.ceil(((float) blob.sizeInBits()) / (ts.numberOfSamples() * ts.numberOfProperties())));

        TimeSeries.Encoded tse = ts.encode();
        TimeSeries         ts2 = TimeSeries.decode(tse.toByteArray());

        List<ZonedDateTime> timestamps = ts2.getTimeStamps();
        assertCollectionEquals(ts.getTimeStamps(), timestamps);

        //
        // Test fetching by index.
        //
        double maxDecimalError = 0;

        for (Generator gs = new Generator(); gs.hasNext(); )
        {
            assertEquals(1.01f + gs.num * 19.7f, ts2.getSample(gs.offset, "propFloat", Double.class), 0.002);
            assertEquals(1.001 - gs.num * 19.7, ts2.getSample(gs.offset, "propDouble", Double.class), 0.002);
            assertEquals(1.01f + gs.num * .197f, ts2.getSample(gs.offset, "propFloat2", Double.class), 0.002);
            assertEquals(1.001 - gs.num * .197, ts2.getSample(gs.offset, "propDouble2", Double.class), 0.002);
            assertEquals(-1200L + gs.num * 30L, (long) ts2.getSample(gs.offset, "propSigned", Long.class));
            assertEquals(1200L - gs.num * 30L, (long) ts2.getSample(gs.offset, "propUnsigned", Long.class));
            assertEquals(gs.bs, ts2.getSample(gs.offset, "propBitSet", BitSet.class));
            assertEquals(values[gs.num % values.length], ts2.getSample(gs.offset, "propEnum", BACnetObjectType.class));
            assertArrayEquals(enumSet[gs.num % enumSet.length], ts2.getSample(gs.offset, "propEnumSet", String[].class));

            maxDecimalError = Math.max(maxDecimalError, Math.abs(ts.getSample(gs.offset, "propFloat", Double.class) - ts2.getSample(gs.offset, "propFloat", Double.class)));
            maxDecimalError = Math.max(maxDecimalError, Math.abs(ts.getSample(gs.offset, "propDouble", Double.class) - ts2.getSample(gs.offset, "propDouble", Double.class)));
            maxDecimalError = Math.max(maxDecimalError, Math.abs(ts.getSample(gs.offset, "propFloat2", Double.class) - ts2.getSample(gs.offset, "propFloat2", Double.class)));
            maxDecimalError = Math.max(maxDecimalError, Math.abs(ts.getSample(gs.offset, "propDouble2", Double.class) - ts2.getSample(gs.offset, "propDouble2", Double.class)));
        }
        System.out.printf("Max encoding error for doubles: %f%n%n", maxDecimalError);

        //
        // Test fetching by extract.
        //
        {
            try (TimeSeriesExtract<Double> extract = new TimeSeriesExtract<>(Double.class))
            {
                ts.extractSamples(extract, "propFloat", false, null, null);

                for (Generator gs = new Generator(); gs.hasNext(); )
                {
                    assertEquals(1.01f + gs.num * 19.7f, extract.getValue(gs.offset), 0.002);
                }
            }

            try (TimeSeriesExtract<Long> extract = new TimeSeriesExtract<>(Long.class))
            {
                ts.extractSamples(extract, "propSigned", false, null, null);

                for (Generator gs = new Generator(); gs.hasNext(); )
                {
                    assertEquals(-1200L + gs.num * 30L, (long) extract.getValue(gs.offset));
                }
            }

            try (TimeSeriesExtract<BitSet> extract = new TimeSeriesExtract<>(BitSet.class))
            {
                ts.extractSamples(extract, "propBitSet", false, null, null);

                for (Generator gs = new Generator(); gs.hasNext(); )
                {
                    assertEquals(gs.bs, extract.getValue(gs.offset));
                }
            }

            try (TimeSeriesExtract<BACnetObjectType> extract = new TimeSeriesExtract<>(BACnetObjectType.class))
            {
                ts.extractSamples(extract, "propEnum", false, null, null);

                for (Generator gs = new Generator(); gs.hasNext(); )
                {
                    assertEquals(values[gs.num % values.length], extract.getValue(gs.offset));
                }
            }

            try (TimeSeriesExtract<String[]> extract = new TimeSeriesExtract<>(String[].class))
            {
                ts.extractSamples(extract, "propEnumSet", false, null, null);

                for (Generator gs = new Generator(); gs.hasNext(); )
                {
                    assertArrayEquals(enumSet[gs.num % enumSet.length], extract.getValue(gs.offset));
                }
            }
        }

        //--//

        //
        // Test fetching by time, exact.
        //
        for (Generator gs = new Generator(); gs.hasNext(); )
        {
            assertEquals(1.01f + gs.num * 19.7, ts2.getSample(timestamps.get(gs.offset), "propFloat", false, false, Float.class), 0.01);
        }

        //
        // Test fetching by time, nearest, a bit in the future.
        //
        for (Generator gs = new Generator(); gs.hasNext(); )
        {
            ZonedDateTime biasedTimestamp = timestamps.get(gs.offset)
                                                      .plus(1, ChronoUnit.SECONDS);

            assertEquals(1.01f + gs.num * 19.7, ts2.getSample(biasedTimestamp, "propFloat", true, false, Float.class), 0.01);
        }

        //
        // Test fetching by time, nearest, a bit in the past.
        //
        for (Generator gs = new Generator(); gs.hasNext(); )
        {
            ZonedDateTime biasedTimestamp = timestamps.get(gs.offset)
                                                      .minus(1, ChronoUnit.SECONDS);

            assertEquals(1.01f + gs.num * 19.7, ts2.getSample(biasedTimestamp, "propFloat", true, false, Float.class), 0.01);
        }

        //--//

        final int rounds = 1000;
        for (int pass = 0; pass < rounds; pass++)
        {
            OutputBitBuffer ob = ts.encodeUncompressed();

            ts2 = TimeSeries.decode(null, new InputBitBuffer(ob));
        }

        Stopwatch st = Stopwatch.createStarted();
        for (int pass = 0; pass < rounds; pass++)
        {
            OutputBitBuffer ob = ts.encodeUncompressed();

            ts2 = TimeSeries.decode(null, new InputBitBuffer(ob));
        }
        st.stop();
        System.out.printf("TimeSeries encoding/decoding time: %dusec%n", st.elapsed(TimeUnit.MICROSECONDS) / rounds);

        st.reset();
        st.start();
        for (int pass = 0; pass < rounds; pass++)
        {
            ts.encodeUncompressed();
        }
        st.stop();
        System.out.printf("TimeSeries encoding time: %dusec%n", st.elapsed(TimeUnit.MICROSECONDS) / rounds);

        st.reset();
        st.start();
        for (int pass = 0; pass < rounds; pass++)
        {
            ts2 = TimeSeries.decode(null, new InputBitBuffer(blob));
        }
        st.stop();
        System.out.printf("TimeSeries decoding time: %dusec%n", st.elapsed(TimeUnit.MICROSECONDS) / rounds);

        TimeSeries clone = TimeSeries.newInstance();
        ts.copySnapshots(0, ts.numberOfSamples(), clone);

        OutputBitBuffer originalBlob = ts.encodeUncompressed();
        OutputBitBuffer cloneBlob    = clone.encodeUncompressed();

        assertArrayEquals(originalBlob.toByteArray(), cloneBlob.toByteArray());
    }

    @Test
    @TestOrder(20)
    public void testCreation2()
    {
        TimeSeries ts = TimeSeries.newInstance();

        ZonedDateTime t = ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 1238, ZoneId.systemDefault());

        for (int index = 0; index < 1000; index++)
        {
            int offset = index % 75;

            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "float1P", TimeSeries.SampleType.Decimal, 3, 6.013 + offset * 19.70);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "float1N", TimeSeries.SampleType.Decimal, 3, 1.001 - offset * 19.70);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "float2P", TimeSeries.SampleType.Decimal, 3, 6.013 + offset * 1.900);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "float2N", TimeSeries.SampleType.Decimal, 3, 1.001 - offset * 1.900);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "float3P", TimeSeries.SampleType.Decimal, 3, 6.013 + offset * 0.197);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "float3N", TimeSeries.SampleType.Decimal, 3, 1.001 - offset * 0.197);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "int1P", TimeSeries.SampleType.Integer, 0, -1200 + offset * 3);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "int1N", TimeSeries.SampleType.Integer, 0, 1200 - offset * 3);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "int2P", TimeSeries.SampleType.Integer, 0, -1200 + offset * 30);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "int2N", TimeSeries.SampleType.Integer, 0, 1200 - offset * 30);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "int3P", TimeSeries.SampleType.Integer, 0, -1200 + offset * 300);
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "int3N", TimeSeries.SampleType.Integer, 0, 1200 - offset * 300);

            t = t.plus(10, ChronoUnit.SECONDS);
        }

        ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "float1P", TimeSeries.SampleType.Decimal, 0, 1E20);
        ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "float1N", TimeSeries.SampleType.Decimal, 0, 1E-20);
        ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "int1P", TimeSeries.SampleType.Integer, 0, Long.MAX_VALUE - 1);
        ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "int1N", TimeSeries.SampleType.Integer, 0, Long.MIN_VALUE);
        t = t.plus(10, ChronoUnit.SECONDS);
        ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "int1P", TimeSeries.SampleType.Integer, 0, Long.MIN_VALUE);
        ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "int1N", TimeSeries.SampleType.Integer, 0, Long.MAX_VALUE - 1);

        assertTrue(ts.wasModified());
        OutputBitBuffer blob = ts.encodeUncompressed();
        System.out.printf("Size of blob of %d samples, %d properties: %d bytes (~%d bits per value)%n%n",
                          ts.numberOfSamples(),
                          ts.numberOfProperties(),
                          blob.sizeInBits(),
                          (int) Math.ceil(((float) blob.sizeInBits()) / (ts.numberOfSamples() * ts.numberOfProperties())));

        TimeSeries ts2 = TimeSeries.decode(null, new InputBitBuffer(blob));

        List<ZonedDateTime> timestamps = ts2.getTimeStamps();
        assertCollectionEquals(ts.getTimeStamps(), timestamps);

        //
        // Test fetching by index.
        //

        double maxDecimalError = 0;

        for (int index = 0; index < 1000; index++)
        {
            int offset = index % 75;
            assertEquals(6.013 + offset * 19.70, ts2.getSample(index, "float1P", Double.class), 0.002);
            assertEquals(1.001 - offset * 19.70, ts2.getSample(index, "float1N", Double.class), 0.002);
            assertEquals(6.013 + offset * 1.900, ts2.getSample(index, "float2P", Double.class), 0.002);
            assertEquals(1.001 - offset * 1.900, ts2.getSample(index, "float2N", Double.class), 0.002);
            assertEquals(6.013 + offset * 0.197, ts2.getSample(index, "float3P", Double.class), 0.002);
            assertEquals(1.001 - offset * 0.197, ts2.getSample(index, "float3N", Double.class), 0.002);
            assertEquals(-1200L + offset * 3, (long) ts2.getSample(index, "int1P", Long.class));
            assertEquals(1200L - offset * 3, (long) ts2.getSample(index, "int1N", Long.class));
            assertEquals(-1200L + offset * 30, (long) ts2.getSample(index, "int2P", Long.class));
            assertEquals(1200L - offset * 30, (long) ts2.getSample(index, "int2N", Long.class));
            assertEquals(-1200L + offset * 300, (long) ts2.getSample(index, "int3P", Long.class));
            assertEquals(1200L - offset * 300, (long) ts2.getSample(index, "int3N", Long.class));

            maxDecimalError = Math.max(maxDecimalError, Math.abs(ts.getSample(index, "float1P", Double.class) - ts2.getSample(offset, "float1P", Double.class)));
            maxDecimalError = Math.max(maxDecimalError, Math.abs(ts.getSample(index, "float1N", Double.class) - ts2.getSample(offset, "float1N", Double.class)));
            maxDecimalError = Math.max(maxDecimalError, Math.abs(ts.getSample(index, "float2P", Double.class) - ts2.getSample(offset, "float2P", Double.class)));
            maxDecimalError = Math.max(maxDecimalError, Math.abs(ts.getSample(index, "float2N", Double.class) - ts2.getSample(offset, "float2N", Double.class)));
            maxDecimalError = Math.max(maxDecimalError, Math.abs(ts.getSample(index, "float3P", Double.class) - ts2.getSample(offset, "float3P", Double.class)));
            maxDecimalError = Math.max(maxDecimalError, Math.abs(ts.getSample(index, "float3N", Double.class) - ts2.getSample(offset, "float3N", Double.class)));
        }

        assertEquals(1E20, ts2.getSample(1000, "float1P", Double.class), 0.002);
        assertEquals(1E-20, ts2.getSample(1000, "float1N", Double.class), 0.002);
        assertEquals(Long.MAX_VALUE - 1, (long) ts2.getSample(1000, "int1P", Long.class));
        assertEquals(Long.MIN_VALUE, (long) ts2.getSample(1000, "int1N", Long.class));
        assertEquals(Long.MIN_VALUE, (long) ts2.getSample(1001, "int1P", Long.class));
        assertEquals(Long.MAX_VALUE - 1, (long) ts2.getSample(1001, "int1N", Long.class));

        System.out.printf("Max encoding error for doubles: %f%n%n", maxDecimalError);
    }

    @Test
    @TestOrder(30)
    public void testSamples() throws
                              Exception
    {
        TimeSeries ts = TimeSeries.newInstance();

        ZonedDateTime t = ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 1238, ZoneId.systemDefault());

        List<String> list = Resources.loadResourceAsLines(this.getClass(), "delta_encoder_samples.txt", false);
        for (String line : list)
        {
            ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "float", TimeSeries.SampleType.Decimal, 3, Double.valueOf(line));

            t = t.plus(10, ChronoUnit.SECONDS);
        }

        assertTrue(ts.wasModified());
        OutputBitBuffer blob = ts.encodeUncompressed();
        System.out.printf("Size of blob of %d samples, %d properties: %d bytes (~%d bits per value)%n%n",
                          ts.numberOfSamples(),
                          ts.numberOfProperties(),
                          blob.sizeInBits(),
                          (int) Math.ceil(((float) blob.sizeInBits()) / (ts.numberOfSamples() * ts.numberOfProperties())));

        TimeSeries ts2 = TimeSeries.decode(null, new InputBitBuffer(blob));

        List<ZonedDateTime> timestamps = ts2.getTimeStamps();
        assertCollectionEquals(ts.getTimeStamps(), timestamps);

        //
        // Test fetching by index.
        //

        double maxDecimalError         = 0;
        double maxDecimalErrorRelative = 0;
        int    index                   = 0;

        for (String line : list)
        {
            double valueExpected = Double.valueOf(line);
            double value         = ts2.getSample(index, "float", Double.class);

            assertEquals(valueExpected, value, 0.002);

            maxDecimalError = Math.max(maxDecimalError, Math.abs(valueExpected - value));
            if (valueExpected != 0)
            {
                maxDecimalErrorRelative = Math.max(maxDecimalErrorRelative, Math.abs(value / valueExpected) - 1);
            }

            index++;
        }

        System.out.printf("Max absolute encoding error for doubles: %f%n%n", maxDecimalError);
        System.out.printf("Max relative encoding error for doubles: %.3f%%%n%n", maxDecimalErrorRelative * 100);
    }

    @Test
    @TestOrder(40)
    public void testDifferPrecisionLevels()
    {
        for (int pass = 0; pass < 5; pass++)
        {
            TimeSeries ts = TimeSeries.newInstance();

            ZonedDateTime t = ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 123_800_000, ZoneId.systemDefault());

            int    seed = 1238941;
            Random rnd  = new Random(seed);
            double val  = 0;

            for (int num = 0; num < 1000; num++)
            {
                switch (pass)
                {
                    case 0:
                        ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propDouble", TimeSeries.SampleType.Decimal, 0, val);
                        break;

                    case 1:
                        ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propDouble", TimeSeries.SampleType.Decimal, 3, val);
                        break;

                    case 2:
                        ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propDouble", TimeSeries.SampleType.Decimal, 4, val);
                        break;

                    case 3:
                        ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propDouble", TimeSeries.SampleType.Decimal, 5, val);
                        break;

                    case 4:
                        ts.addSample(TimeSeries.SampleResolution.Max1000Hz, t, "propDouble", TimeSeries.SampleType.Decimal, 0, val);
                        break;
                }

                t = t.plus(10, ChronoUnit.SECONDS);
                val += (rnd.nextInt(250) - 125) * 0.0001;
            }

            assertTrue(ts.wasModified());
            OutputBitBuffer blob = ts.encodeUncompressed();

            String label = null;
            switch (pass)
            {
                case 0:
                    label = "Regular Sample";
                    break;

                case 1:
                    label = "3-digit Precision Sample";
                    break;

                case 2:
                    label = "4-digit Precision Sample";
                    break;

                case 3:
                    label = "5-digit Precision Sample";
                    break;

                case 4:
                    label = "Millisecond Resolution Sample";
                    break;
            }

            System.out.printf("%s: size of blob of %d samples, %d properties: %d bytes (~%d bits per value)\n",
                              label,
                              ts.numberOfSamples(),
                              ts.numberOfProperties(),
                              blob.sizeInBits(),
                              (int) Math.ceil(((float) blob.sizeInBits()) / (ts.numberOfSamples() * ts.numberOfProperties())));

            TimeSeries ts2 = TimeSeries.decode(null, new InputBitBuffer(blob));

            List<ZonedDateTime> timestamps = ts2.getTimeStamps();
            assertCollectionEquals(ts.getTimeStamps(), timestamps);

            //
            // Test fetching by index.
            //

            double maxDecimalError = 0;

            rnd = new Random(seed);
            val = 0;

            for (int num = 0; num < 1000; num++)
            {
                assertEquals(val, ts2.getSample(num, "propDouble", Double.class), 0.002);

                maxDecimalError = Math.max(maxDecimalError, Math.abs(ts.getSample(num, "propDouble", Double.class) - ts2.getSample(num, "propDouble", Double.class)));
                val += (rnd.nextInt(250) - 125) * 0.0001;
            }
            System.out.printf("Max encoding error for doubles: %.12f\n\n", maxDecimalError);
        }
    }

    @Test
    @TestOrder(50)
    public void testTimeRange()
    {
        RangeSelection rs = new RangeSelection();

        ZonedDateTime time = ZonedDateTime.of(2019, 10, 11, 14, 43, 12, 0, ZoneId.systemDefault());

        for (TimeRangeId range : TimeRangeId.values())
        {
            rs.range = range;

            TimeRange tr = rs.resolve(time, null, false);
            System.out.printf("%s : %20s : %s <-> %s\n", time, range, tr.start, tr.end);
        }

        for (TimeRangeId range : TimeRangeId.values())
        {
            rs.range = range;

            TimeRange tr = rs.resolve(time, null, true);
            System.out.printf("%s (aligned) : %20s : %s <-> %s\n", time, range, tr.start, tr.end);
        }
    }

    @Test
    @TestOrder(60)
    public void testTimeRangeFilter()
    {
        ZonedDateTime start = ZonedDateTime.of(2019, 9, 3, 8, 43, 12, 0, ZoneId.systemDefault());
        ZonedDateTime end   = ZonedDateTime.of(2019, 10, 11, 8, 43, 12, 0, ZoneId.systemDefault());

        TimeRange tr = new TimeRange(start, end);

        RelativeTimeRange relativeTimeRange1 = new RelativeTimeRange();
        relativeTimeRange1.offsetSeconds   = 8 * 3600;
        relativeTimeRange1.durationSeconds = 1 * 3600;

        RelativeTimeRange relativeTimeRange2 = new RelativeTimeRange();
        relativeTimeRange2.offsetSeconds   = 12 * 3600;
        relativeTimeRange2.durationSeconds = 1 * 3600;

        RelativeTimeRange relativeTimeRange3 = new RelativeTimeRange();
        relativeTimeRange3.offsetSeconds   = 12 * 3600 + 30;
        relativeTimeRange3.durationSeconds = 1800;

        RecurringWeeklySchedule schedule = new RecurringWeeklySchedule();

        DailyScheduleWithDayOfWeek dailyScheduleWithDayOfWeek1 = new DailyScheduleWithDayOfWeek();
        dailyScheduleWithDayOfWeek1.dailySchedule = new DailySchedule();
        dailyScheduleWithDayOfWeek1.dayOfWeek     = DayOfWeek.TUESDAY;
        dailyScheduleWithDayOfWeek1.dailySchedule.ranges.add(relativeTimeRange2);
        dailyScheduleWithDayOfWeek1.dailySchedule.ranges.add(relativeTimeRange3);
        dailyScheduleWithDayOfWeek1.dailySchedule.ranges.add(relativeTimeRange1);
        schedule.days.add(dailyScheduleWithDayOfWeek1);

        DailyScheduleWithDayOfWeek dailyScheduleWithDayOfWeek2 = new DailyScheduleWithDayOfWeek();
        dailyScheduleWithDayOfWeek2.dailySchedule = new DailySchedule();
        dailyScheduleWithDayOfWeek2.dayOfWeek     = DayOfWeek.FRIDAY;
        dailyScheduleWithDayOfWeek2.dailySchedule.ranges.add(relativeTimeRange1);
        schedule.days.add(dailyScheduleWithDayOfWeek2);

        DailyScheduleWithDayOfWeek dailyScheduleWithDayOfWeek3 = new DailyScheduleWithDayOfWeek();
        dailyScheduleWithDayOfWeek3.dailySchedule = new DailySchedule();
        dailyScheduleWithDayOfWeek3.dayOfWeek     = DayOfWeek.WEDNESDAY;
        dailyScheduleWithDayOfWeek3.dailySchedule.ranges.add(relativeTimeRange2);
        schedule.days.add(dailyScheduleWithDayOfWeek3);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss E");

        System.out.printf("From  %s - %s\n", formatter.format(start), formatter.format(end));
        List<TimeRange> results = tr.filter(schedule);
        for (TimeRange result : results)
        {
            System.out.printf("  To %s - %s\n", formatter.format(result.start), formatter.format(result.end));
        }

        ZonedDateTime series1Start = ZonedDateTime.of(2019, 10, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime series1End   = series1Start.plusDays(1);

        TimeSeriesPropertyResponse series1 = newSeries(EngineeringUnits.hours, series1Start, 86400, 0, 86400);
        TimeSeriesPropertyResponse series2 = TimeRange.addTimestampsForTimeSegments(series1, new TimeRange(series1Start, series1End).filter(schedule), Double.MAX_VALUE);

        for (int i = 0; i < series2.timestamps.length; i++)
        {
            double timestamp = series2.timestamps[i];
            double value     = series2.values[i];

            System.out.printf("  Series %s - %s\n", formatter.format(TimeUtils.fromTimestampToUtcTime(timestamp)), value);
        }

        assertEquals(6, series2.timestamps.length);
        assertEquals(3600 * 0, series2.values[0], 0.0001);
        assertEquals(3600 * 8, series2.values[1], 0.0001);
        assertEquals(3600 * 9, series2.values[2], 0.0001);
        assertEquals(3600 * 12, series2.values[3], 0.0001);
        assertEquals(3600 * 13, series2.values[4], 0.0001);
        assertEquals(3600 * 24, series2.values[5], 0.0001);
    }

    @Test
    @TestOrder(70)
    public void testExtractInserts()
    {
        ZonedDateTime tStart = ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 1238, ZoneId.systemDefault());
        ZonedDateTime t      = tStart;

        TimeSeriesExtract<Double> src = new TimeSeriesExtract<>(Double.class);

        for (int i = 0; i < 3 * src.segmentSize(); i++)
        {
            src.add(TimeUtils.fromUtcTimeToTimestamp(t), i);

            t = t.plus(10, ChronoUnit.SECONDS);
        }

        for (int i = 0; i < 3 * src.segmentSize(); i++)
        {
            assertEquals(i, src.getValue(i), 0.0001);
        }

        src.add(TimeUtils.fromUtcTimeToTimestamp(tStart), -100);
        assertEquals(3 * src.segmentSize(), src.size());
        assertEquals(-100, src.getValue(0), 0.0001);
        assertEquals(1, src.getValue(1), 0.0001);

        src.add(TimeUtils.fromUtcTimeToTimestamp(tStart.minus(1, ChronoUnit.SECONDS)), -200);
        assertEquals(3 * src.segmentSize() + 1, src.size());

        for (int i = 0; i < 3 * src.segmentSize() + 1; i++)
        {
            double val;

            switch (i)
            {
                case 0:
                    val = -200;
                    break;

                case 1:
                    val = -100;
                    break;

                default:
                    val = i - 1;
                    break;
            }

            assertEquals(val, src.getValue(i), 0.0001);
        }

        src.remove(0);
        assertEquals(3 * src.segmentSize(), src.size());

        for (int i = 0; i < src.size(); i++)
        {
            assertEquals(i == 0 ? -100 : i, src.getValue(i), 0.0001);
        }

        src.remove(0);
        assertEquals(3 * src.segmentSize() - 1, src.size());
        for (int i = 0; i < src.size(); i++)
        {
            assertEquals(i + 1, src.getValue(i), 0.0001);
        }

        src.remove(1);
        assertEquals(3 * src.segmentSize() - 2, src.size());
        for (int i = 0; i < src.size(); i++)
        {
            assertEquals(i == 0 ? 1 : i + 2, src.getValue(i), 0.0001);
        }
    }

    @Test
    @TestOrder(80)
    public void testRemovalOfIdenticalValues()
    {
        class Generator
        {
            final String[][] valueEnumSets = new String[][] { { "Test1" }, { "Test1", "Test2" }, { "Test3" }, { "Test1", "Test4" } };
            String[]         valueEnumSet;
            BACnetObjectType valueEnum;
            BitSet           valueBitset;
            long             valueInteger;
            double           valueDecimal;

            int num = -1;

            boolean hasNext()
            {
                if (++num >= 1000)
                {
                    return false;
                }

                valueEnumSet = valueEnumSets[0];
                valueEnum    = BACnetObjectType.analog_value;
                valueBitset  = new BitSet();
                valueInteger = 123;
                valueDecimal = 345;

                valueBitset.set(1);

                return true;
            }
        }

        {
            TimeSeries ts = TimeSeries.newInstance();

            ZonedDateTime t = ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 1238, ZoneId.systemDefault());

            Generator gs = new Generator();
            while (gs.hasNext())
            {
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propDecimal", TimeSeries.SampleType.Decimal, 3, gs.valueDecimal);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propInteger", TimeSeries.SampleType.Integer, 0, gs.valueInteger);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propBitSet", TimeSeries.SampleType.BitSet, 0, gs.valueBitset);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propEnum", TimeSeries.SampleType.Enumerated, 0, gs.valueEnum);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propEnumSet", TimeSeries.SampleType.EnumeratedSet, 0, gs.valueEnumSet);

                t = t.plus(10, ChronoUnit.SECONDS);
            }

            ts.removeCloseIdenticalValues(100);
            assertEquals(101, ts.numberOfSamples());
        }

        {
            TimeSeries ts = TimeSeries.newInstance();

            ZonedDateTime t = ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 1238, ZoneId.systemDefault());

            Generator gs = new Generator();
            while (gs.hasNext())
            {
                switch (gs.num)
                {
                    case 15:
                        gs.valueDecimal += 1;
                        break;
                }

                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propDecimal", TimeSeries.SampleType.Decimal, 3, gs.valueDecimal);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propInteger", TimeSeries.SampleType.Integer, 0, gs.valueInteger);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propBitSet", TimeSeries.SampleType.BitSet, 0, gs.valueBitset);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propEnum", TimeSeries.SampleType.Enumerated, 0, gs.valueEnum);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propEnumSet", TimeSeries.SampleType.EnumeratedSet, 0, gs.valueEnumSet);

                t = t.plus(10, ChronoUnit.SECONDS);
            }

            ts.removeCloseIdenticalValues(100);
            assertEquals(101 + 3, ts.numberOfSamples());
        }

        {
            TimeSeries ts = TimeSeries.newInstance();

            ZonedDateTime t = ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 1238, ZoneId.systemDefault());

            Generator gs = new Generator();
            while (gs.hasNext())
            {
                switch (gs.num)
                {
                    case 15:
                        gs.valueDecimal += 1;
                        gs.valueBitset = null;
                        break;
                }

                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propDecimal", TimeSeries.SampleType.Decimal, 3, gs.valueDecimal);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propInteger", TimeSeries.SampleType.Integer, 0, gs.valueInteger);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propBitSet", TimeSeries.SampleType.BitSet, 0, gs.valueBitset);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propEnum", TimeSeries.SampleType.Enumerated, 0, gs.valueEnum);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propEnumSet", TimeSeries.SampleType.EnumeratedSet, 0, gs.valueEnumSet);

                t = t.plus(10, ChronoUnit.SECONDS);
            }

            ts.removeCloseIdenticalValues(100);
            assertEquals(101 + 3, ts.numberOfSamples());
        }

        {
            TimeSeries ts = TimeSeries.newInstance();

            ZonedDateTime t = ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 1238, ZoneId.systemDefault());

            Generator gs = new Generator();
            while (gs.hasNext())
            {
                switch (gs.num)
                {
                    case 10:
                        gs.valueDecimal += 1;
                        gs.valueBitset = null;
                        break;
                }

                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propDecimal", TimeSeries.SampleType.Decimal, 3, gs.valueDecimal);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propInteger", TimeSeries.SampleType.Integer, 0, gs.valueInteger);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propBitSet", TimeSeries.SampleType.BitSet, 0, gs.valueBitset);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propEnum", TimeSeries.SampleType.Enumerated, 0, gs.valueEnum);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propEnumSet", TimeSeries.SampleType.EnumeratedSet, 0, gs.valueEnumSet);

                t = t.plus(10, ChronoUnit.SECONDS);
            }

            ts.removeCloseIdenticalValues(100);

            // The changed value overlaps with the max time separation
            assertEquals(101 + 2, ts.numberOfSamples());
        }

        {
            TimeSeries ts = TimeSeries.newInstance();

            ZonedDateTime t = ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 1238, ZoneId.systemDefault());

            Generator gs = new Generator();
            while (gs.hasNext())
            {
                switch (gs.num)
                {
                    case 10:
                    case 11:
                        gs.valueDecimal += 1;
                        gs.valueBitset = null;
                        break;
                }

                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propDecimal", TimeSeries.SampleType.Decimal, 3, gs.valueDecimal);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propInteger", TimeSeries.SampleType.Integer, 0, gs.valueInteger);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propBitSet", TimeSeries.SampleType.BitSet, 0, gs.valueBitset);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propEnum", TimeSeries.SampleType.Enumerated, 0, gs.valueEnum);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propEnumSet", TimeSeries.SampleType.EnumeratedSet, 0, gs.valueEnumSet);

                t = t.plus(10, ChronoUnit.SECONDS);
            }

            ts.removeCloseIdenticalValues(100);

            // The changed value overlaps with the max time separation and there are just two identical values next to each other.
            assertEquals(101 + 3, ts.numberOfSamples());
        }

        {
            TimeSeries ts = TimeSeries.newInstance();

            ZonedDateTime t = ZonedDateTime.of(1980, 1, 1, 0, 0, 0, 1238, ZoneId.systemDefault());

            Generator gs = new Generator();
            while (gs.hasNext())
            {
                switch (gs.num)
                {
                    case 10:
                    case 11:
                    case 12:
                        gs.valueDecimal += 1;
                        gs.valueBitset = null;
                        break;
                }

                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propDecimal", TimeSeries.SampleType.Decimal, 3, gs.valueDecimal);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propInteger", TimeSeries.SampleType.Integer, 0, gs.valueInteger);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propBitSet", TimeSeries.SampleType.BitSet, 0, gs.valueBitset);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propEnum", TimeSeries.SampleType.Enumerated, 0, gs.valueEnum);
                ts.addSample(TimeSeries.SampleResolution.Max1Hz, t, "propEnumSet", TimeSeries.SampleType.EnumeratedSet, 0, gs.valueEnumSet);

                t = t.plus(10, ChronoUnit.SECONDS);
            }

            ts.removeCloseIdenticalValues(100);

            // The changed value overlaps with the max time separation and there are three identical values next to each other, so the middle one got removed.
            assertEquals(101 + 3, ts.numberOfSamples());
        }
    }

    //--//

    private TimeSeriesPropertyResponse newSeries(EngineeringUnits units,
                                                 ZonedDateTime start,
                                                 int timeStep,
                                                 double... values)
    {
        TimeSeriesPropertyResponse samples = new TimeSeriesPropertyResponse();
        samples.values       = values;
        samples.expectedType = Double.class;
        samples.timestamps   = new double[values.length];

        for (int i = 0; i < values.length; i++)
        {
            samples.timestamps[i] = TimeUtils.fromUtcTimeToTimestamp(start) + i * timeStep;
        }

        return samples;
    }
}
