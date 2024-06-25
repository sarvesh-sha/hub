/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;

import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.protocol.model.ipn.enums.Ipn_PalFinger_DisplayCode;
import com.optio3.test.common.Optio3Test;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Resources;
import org.junit.Test;

public class TimeSeriesDecodingTest extends Optio3Test
{
    @Test
    public void testSample1() throws
                              Exception
    {
        TimeSeries ts = parse("timeseries/Centre425-SW.tsv");

        List<ZonedDateTime> timestamps = ts.getTimeStamps();
        assertEquals(TimeSeries.Version.V1, ts.getSourceVersion());
        assertEquals(30012, timestamps.size());

        assertEquals("2018-05-17T04:00Z",
                     CollectionUtils.firstElement(timestamps)
                                    .toString());

        assertEquals("2019-04-06T09:00Z",
                     CollectionUtils.lastElement(timestamps)
                                    .toString());
    }

    @Test
    public void testSample2() throws
                              Exception
    {
        TimeSeries ts = parse("timeseries/Occidental.tsv");

        List<ZonedDateTime> timestamps = ts.getTimeStamps();
        assertEquals(TimeSeries.Version.V1, ts.getSourceVersion());
        assertEquals(9542, timestamps.size());

        assertEquals("2018-02-09T17:45Z",
                     CollectionUtils.firstElement(timestamps)
                                    .toString());

        assertEquals("2018-09-15T18:15Z",
                     CollectionUtils.lastElement(timestamps)
                                    .toString());
    }

    @Test
    public void testSample3() throws
                              Exception
    {
        TimeSeries ts = parse("timeseries/PilotMcK.tsv");

        List<ZonedDateTime> timestamps = ts.getTimeStamps();
        assertEquals(TimeSeries.Version.V2, ts.getSourceVersion());
        assertEquals(24000, timestamps.size());

        assertEquals("2017-11-27T04:45:02Z",
                     CollectionUtils.firstElement(timestamps)
                                    .toString());

        assertEquals("2018-10-01T23:45Z",
                     CollectionUtils.lastElement(timestamps)
                                    .toString());

        var schema = getSchema(ts, "present_value");
        assertEquals(0, schema.getMinimumPrecision());
    }

    @Test
    public void testSample4() throws
                              Exception
    {
        TimeSeries ts = parse("timeseries/MTA_latitude.tsv");

        List<ZonedDateTime> timestamps = ts.getTimeStamps();
        assertEquals(TimeSeries.Version.V1, ts.getSourceVersion());
        assertEquals(7858, timestamps.size());

        assertEquals("2020-04-02T15:41:02Z",
                     CollectionUtils.firstElement(timestamps)
                                    .toString());

        assertEquals("2020-04-30T12:37:40Z",
                     CollectionUtils.lastElement(timestamps)
                                    .toString());

        var schema = getSchema(ts, "present_value");
        assertEquals(5, schema.getMinimumPrecision());
    }

    @Test
    public void testSample5() throws
                              Exception
    {
        TimeSeries ts = parse("timeseries/FDNY_faultcodes.tsv");

        List<ZonedDateTime> timestamps = ts.getTimeStamps();
        assertEquals(TimeSeries.Version.V1, ts.getSourceVersion());
        assertEquals(8657, timestamps.size());

        assertEquals("2020-09-26T12:29:51Z",
                     CollectionUtils.firstElement(timestamps)
                                    .toString());

        assertEquals("2021-03-05T23:00Z",
                     CollectionUtils.lastElement(timestamps)
                                    .toString());

        var schema = getSchema(ts, "present_value");
        assertEquals(0, schema.getMinimumPrecision());

        String[] faults = ts.getSample(ZonedDateTime.parse("2020-10-23T16:59:59Z"), "present_value", false, false, String[].class);
        assertNotNull(faults);
        assertEquals(4, faults.length);
        assertArrayEquals(new String[] { "P0272", "P0284", "P0278", "P0275" }, faults);
    }

    @Test
    public void testSample6() throws
                              Exception
    {
        TimeSeries ts = parse("timeseries/Autozone_display.tsv");

        List<ZonedDateTime> timestamps = ts.getTimeStamps();
        assertEquals(TimeSeries.Version.V1, ts.getSourceVersion());
        assertEquals(11880, timestamps.size());

        assertEquals("2020-01-31T18:38:35Z",
                     CollectionUtils.firstElement(timestamps)
                                    .toString());

        assertEquals("2020-10-10T06:00Z",
                     CollectionUtils.lastElement(timestamps)
                                    .toString());

        var schema = getSchema(ts, "present_value");
        assertEquals(0, schema.getMinimumPrecision());

        Ipn_PalFinger_DisplayCode display = ts.getSample(ZonedDateTime.parse("2020-02-17T18:00Z"), "present_value", false, false, Ipn_PalFinger_DisplayCode.class);
        assertEquals(Ipn_PalFinger_DisplayCode.Undervoltage, display);
    }

    private TimeSeries parse(String resourceName) throws
                                                  IOException
    {
        try (InputStream stream = Resources.openResourceAsStream(TimeSeriesDecodingTest.class, resourceName))
        {
            var array = ExpandableArrayOfBytes.create(stream, -1);

            return TimeSeries.decode(array.toArray());
        }
    }

    private TimeSeries.SampleSchema getSchema(TimeSeries ts,
                                              String prop)
    {
        for (TimeSeries.SampleSchema schema : ts.getSchema())
        {
            if (schema.identifier.equals(prop))
            {
                return schema;
            }
        }

        fail(String.format("Can't find property '%s'", prop));
        return null;
    }
}
