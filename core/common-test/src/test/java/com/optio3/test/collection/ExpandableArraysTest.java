/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.optio3.collection.ExpandableArrayOf;
import com.optio3.collection.ExpandableArrayOfDoubles;
import com.optio3.collection.ExpandableArrayOfFloats;
import com.optio3.collection.ExpandableArrayOfInts;
import com.optio3.collection.ExpandableArrayOfLongs;
import com.optio3.test.common.Optio3Test;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class ExpandableArraysTest extends Optio3Test
{
    @Test
    public void testInt()
    {
        ExpandableArrayOfInts array = ExpandableArrayOfInts.create();

        assertEquals(-1, array.binarySearch(20));

        array.prepareForGrowth(32);

        for (int i = 0; i < 60; i++)
        {
            array.add(10 + 2 * i);
        }

        assertEquals(60, array.size());

        for (int i = 0; i < 60; i++)
        {
            assertEquals(10 + 2 * i, array.get(i, Integer.MAX_VALUE));
        }

        assertEquals(Integer.MAX_VALUE, array.get(-1, Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, array.get(60, Integer.MAX_VALUE));

        //--//

        for (int i = 0; i < 60; i++)
        {
            assertEquals(i, array.binarySearch(10 + 2 * i));
            assertEquals(-(i + 1), array.binarySearch(9 + 2 * i));
            assertEquals(-(i + 1 + 1), array.binarySearch(11 + 2 * i));
        }

        array = array.copy();

        for (int i = 0; i < 60; i++)
        {
            assertEquals(i, array.binarySearch(10 + 2 * i));
            assertEquals(-(i + 1), array.binarySearch(9 + 2 * i));
            assertEquals(-(i + 1 + 1), array.binarySearch(11 + 2 * i));
        }

        //--//

        array.insert(23, -1);

        for (int i = 0; i < 23; i++)
        {
            assertEquals(10 + 2 * i, array.get(i, Integer.MAX_VALUE));
        }

        assertEquals(-1, array.get(23, Integer.MAX_VALUE));

        for (int i = 24; i < 60 + 1; i++)
        {
            assertEquals(10 + 2 * (i - 1), array.get(i, Integer.MAX_VALUE));
        }

        //--//

        array = array.copy();

        for (int i = 0; i < 23; i++)
        {
            assertEquals(10 + 2 * i, array.get(i, Integer.MAX_VALUE));
        }

        assertEquals(-1, array.get(23, Integer.MAX_VALUE));

        for (int i = 24; i < 60 + 1; i++)
        {
            assertEquals(10 + 2 * (i - 1), array.get(i, Integer.MAX_VALUE));
        }

        //--//

        array.remove(23, false);

        for (int i = 0; i < 60; i++)
        {
            assertEquals(10 + 2 * i, array.get(i, Integer.MAX_VALUE));
        }

        array.set(-1, 120);
        array.set(58, 120);
        assertEquals(120, array.get(58, Integer.MAX_VALUE));
        array = array.copy();
        array.set(57, 120);

        //--//

        array.clear();
        assertEquals(0, array.size());

        assertEquals(0, array.toArray().length);

        //--//

        array.fromArray(new int[] { 1, 2, 3, 4 });
        assertEquals(4, array.size());
        assertEquals(3, array.get(2, Integer.MAX_VALUE));
    }

    @Test
    public void testLong()
    {
        ExpandableArrayOfLongs array = ExpandableArrayOfLongs.create();

        assertEquals(-1, array.binarySearch(20));

        array.prepareForGrowth(32);

        for (int i = 0; i < 60; i++)
        {
            array.add(10 + 2 * i);
        }

        assertEquals(60, array.size());

        for (int i = 0; i < 60; i++)
        {
            assertEquals(10 + 2 * i, array.get(i, Long.MAX_VALUE));
        }

        assertEquals(Long.MAX_VALUE, array.get(-1, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, array.get(60, Long.MAX_VALUE));

        //--//

        for (int i = 0; i < 60; i++)
        {
            assertEquals(i, array.binarySearch(10 + 2 * i));
            assertEquals(-(i + 1), array.binarySearch(9 + 2 * i));
            assertEquals(-(i + 1 + 1), array.binarySearch(11 + 2 * i));
        }

        array = array.copy();

        for (int i = 0; i < 60; i++)
        {
            assertEquals(i, array.binarySearch(10 + 2 * i));
            assertEquals(-(i + 1), array.binarySearch(9 + 2 * i));
            assertEquals(-(i + 1 + 1), array.binarySearch(11 + 2 * i));
        }

        //--//

        array.insert(23, -1);

        for (int i = 0; i < 23; i++)
        {
            assertEquals(10 + 2 * i, array.get(i, Long.MAX_VALUE));
        }

        assertEquals(-1, array.get(23, Long.MAX_VALUE));

        for (int i = 24; i < 60 + 1; i++)
        {
            assertEquals(10 + 2 * (i - 1), array.get(i, Long.MAX_VALUE));
        }

        //--//

        array = array.copy();

        for (int i = 0; i < 23; i++)
        {
            assertEquals(10 + 2 * i, array.get(i, Long.MAX_VALUE));
        }

        assertEquals(-1, array.get(23, Long.MAX_VALUE));

        for (int i = 24; i < 60 + 1; i++)
        {
            assertEquals(10 + 2 * (i - 1), array.get(i, Long.MAX_VALUE));
        }

        //--//

        array.remove(23, false);

        for (int i = 0; i < 60; i++)
        {
            assertEquals(10 + 2 * i, array.get(i, Long.MAX_VALUE));
        }

        array.set(-1, 120);
        array.set(58, 120);
        assertEquals(120, array.get(58, Long.MAX_VALUE));
        array = array.copy();
        array.set(57, 120);

        //--//

        array.clear();
        assertEquals(0, array.size());

        assertEquals(0, array.toArray().length);

        //--//

        array.fromArray(new long[] { 1, 2, 3, 4 });
        assertEquals(4, array.size());
        assertEquals(3, array.get(2, Long.MAX_VALUE));
    }

    @Test
    public void testFloat()
    {
        ExpandableArrayOfFloats array = ExpandableArrayOfFloats.create();

        assertEquals(-1, array.binarySearch(20));

        array.prepareForGrowth(32);

        for (int i = 0; i < 60; i++)
        {
            array.add(10.4f + 2 * i);
        }

        assertEquals(60, array.size());

        for (int i = 0; i < 60; i++)
        {
            assertEquals(10.4f + 2 * i, array.get(i, Float.NaN), 0.0f);
        }

        assertEquals(Float.NaN, array.get(-1, Float.NaN), 0.0f);
        assertEquals(Float.NaN, array.get(60, Float.NaN), 0.0f);

        //--//

        for (int i = 0; i < 60; i++)
        {
            assertEquals(i, array.binarySearch(10.4f + 2 * i));
            assertEquals(-(i + 1), array.binarySearch(9.4f + 2 * i));
            assertEquals(-(i + 1 + 1), array.binarySearch(11.4f + 2 * i));
        }

        array = array.copy();

        for (int i = 0; i < 60; i++)
        {
            assertEquals(i, array.binarySearch(10.4f + 2 * i));
            assertEquals(-(i + 1), array.binarySearch(9.4f + 2 * i));
            assertEquals(-(i + 1 + 1), array.binarySearch(11.4f + 2 * i));
        }

        //--//

        array.insert(23, -1.2f);

        for (int i = 0; i < 23; i++)
        {
            assertEquals(10.4f + 2 * i, array.get(i, Float.NaN), 0.0f);
        }

        assertEquals(-1.2f, array.get(23, Float.NaN), 0.0f);

        for (int i = 24; i < 60 + 1; i++)
        {
            assertEquals(10.4f + 2 * (i - 1), array.get(i, Float.NaN), 0.0f);
        }

        //--//

        array = array.copy();

        for (int i = 0; i < 23; i++)
        {
            assertEquals(10.4f + 2 * i, array.get(i, Float.NaN), 0.0f);
        }

        assertEquals(-1.2f, array.get(23, Float.NaN), 0.0f);

        for (int i = 24; i < 60 + 1; i++)
        {
            assertEquals(10.4f + 2 * (i - 1), array.get(i, Float.NaN), 0.0f);
        }

        //--//

        array.remove(23, false);

        for (int i = 0; i < 60; i++)
        {
            assertEquals(10.4f + 2 * i, array.get(i, Float.NaN), 0.0f);
        }

        array.set(-1, 120.2f);
        array.set(58, 120.2f);
        assertEquals(120.2f, array.get(58, Float.NaN), 0.0f);
        array = array.copy();
        array.set(57, 120.2f);

        //--//

        array.clear();
        assertEquals(0, array.size());

        assertEquals(0, array.toArray().length);

        //--//

        array.fromArray(new float[] { 1f, 2f, 3f, 4f });
        assertEquals(4, array.size());
        assertEquals(3f, array.get(2, Float.NaN), 0.0f);
    }

    @Test
    public void testDouble()
    {
        ExpandableArrayOfDoubles array = ExpandableArrayOfDoubles.create();

        assertEquals(-1, array.binarySearch(20));

        array.prepareForGrowth(32);

        for (int i = 0; i < 60; i++)
        {
            array.add(10.4 + 2 * i);
        }

        assertEquals(60, array.size());

        for (int i = 0; i < 60; i++)
        {
            assertEquals(10.4 + 2 * i, array.get(i, Double.NaN), 0.0);
        }

        assertEquals(Double.NaN, array.get(-1, Double.NaN), 0.0);
        assertEquals(Double.NaN, array.get(60, Double.NaN), 0.0);

        //--//

        for (int i = 0; i < 60; i++)
        {
            assertEquals(i, array.binarySearch(10.4 + 2 * i));
            assertEquals(-(i + 1), array.binarySearch(9.4 + 2 * i));
            assertEquals(-(i + 1 + 1), array.binarySearch(11.4 + 2 * i));
        }

        array = array.copy();

        for (int i = 0; i < 60; i++)
        {
            assertEquals(i, array.binarySearch(10.4 + 2 * i));
            assertEquals(-(i + 1), array.binarySearch(9.4 + 2 * i));
            assertEquals(-(i + 1 + 1), array.binarySearch(11.4 + 2 * i));
        }

        //--//

        array.insert(23, -1.2);

        for (int i = 0; i < 23; i++)
        {
            assertEquals(10.4 + 2 * i, array.get(i, Double.NaN), 0.0);
        }

        assertEquals(-1.2, array.get(23, Double.NaN), 0.0);

        for (int i = 24; i < 60 + 1; i++)
        {
            assertEquals(10.4 + 2 * (i - 1), array.get(i, Double.NaN), 0.0);
        }

        //--//

        array = array.copy();

        // To test background cleaner.
        System.gc();

        for (int i = 0; i < 23; i++)
        {
            assertEquals(10.4 + 2 * i, array.get(i, Double.NaN), 0.0);
        }

        assertEquals(-1.2, array.get(23, Double.NaN), 0.0);

        for (int i = 24; i < 60 + 1; i++)
        {
            assertEquals(10.4 + 2 * (i - 1), array.get(i, Double.NaN), 0.0);
        }

        //--//

        array.remove(23, false);

        for (int i = 0; i < 60; i++)
        {
            assertEquals(10.4 + 2 * i, array.get(i, Double.NaN), 0.0);
        }

        array.set(-1, 120.2);
        array.set(58, 120.2);
        assertEquals(120.2, array.get(58, Double.NaN), 0.0);
        array = array.copy();
        array.set(57, 120.2);

        // To test background cleaner.
        System.gc();

        //--//

        array.clear();
        assertEquals(0, array.size());

        assertEquals(0, array.toArray().length);

        //--//

        array.fromArray(new double[] { 1.0, 2.0, 3.0, 4.0 });
        assertEquals(4, array.size());
        assertEquals(3.0, array.get(2, Double.NaN), 0.0);
    }

    @Test
    public void testString()
    {
        class ExpandableArrayOfStrings extends ExpandableArrayOf<String>
        {
            public ExpandableArrayOfStrings()
            {
                super(String.class);
            }

            @Override
            protected ExpandableArrayOf<String> allocate()
            {
                return new ExpandableArrayOfStrings();
            }

            @Override
            protected int compare(String o1,
                                  String o2)
            {
                return StringUtils.compare(o1, o2);
            }
        }

        ExpandableArrayOf<String> array = new ExpandableArrayOfStrings();

        assertEquals(-1, array.binarySearch("T1"));

        array.prepareForGrowth(32);

        for (int i = 0; i < 60; i++)
        {
            array.add(String.format("V_%04d", 2 * i));
        }

        assertEquals(60, array.size());

        for (int i = 0; i < 60; i++)
        {
            assertEquals(String.format("V_%04d", 2 * i), array.get(i, null));
        }

        assertNull(array.get(-1, null));
        assertNull(array.get(60, null));

        //--//

        for (int i = 0; i < 60; i++)
        {
            assertEquals(i, array.binarySearch(String.format("V_%04d", 2 * i)));
            assertEquals(-(i + 1), array.binarySearch(String.format("V_%04d", 2 * i - 1)));
            assertEquals(-(i + 1 + 1), array.binarySearch(String.format("V_%04d", 2 * i) + 1));
        }

        array = array.copy();

        for (int i = 0; i < 60; i++)
        {
            assertEquals(i, array.binarySearch(String.format("V_%04d", 2 * i)));
            assertEquals(-(i + 1), array.binarySearch(String.format("V_%04d", 2 * i - 1)));
            assertEquals(-(i + 1 + 1), array.binarySearch(String.format("V_%04d", 2 * i) + 1));
        }

        //--//

        array.insert(23, "New");

        for (int i = 0; i < 23; i++)
        {
            assertEquals(String.format("V_%04d", 2 * i), array.get(i, null));
        }

        assertEquals("New", array.get(23, null));

        for (int i = 24; i < 60 + 1; i++)
        {
            assertEquals(String.format("V_%04d", 2 * (i - 1)), array.get(i, null));
        }

        //--//

        array = array.copy();

        for (int i = 0; i < 23; i++)
        {
            assertEquals(String.format("V_%04d", 2 * i), array.get(i, null));
        }

        assertEquals("New", array.get(23, null));

        for (int i = 24; i < 60 + 1; i++)
        {
            assertEquals(String.format("V_%04d", 2 * (i - 1)), array.get(i, null));
        }

        //--//

        array.remove(23, false);

        for (int i = 0; i < 60; i++)
        {
            assertEquals(String.format("V_%04d", 2 * i), array.get(i, null));
        }

        array.set(-1, "Skip");
        array.set(58, "New2");
        assertEquals("New2", array.get(58, null));
        array = array.copy();
        array.set(57, "New3");

        //--//

        array.clear();
        assertEquals(0, array.size());

        assertEquals(0, array.toArray().length);

        //--//

        array.fromArray(new String[] { "A1", "A2", "A3", "A4" });
        assertEquals(4, array.size());
        assertEquals("A3", array.get(2, null));
    }
}
