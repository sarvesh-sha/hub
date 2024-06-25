/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Objects;

import com.optio3.collection.MapWithWeakValues;
import com.optio3.serialization.Reflection;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import org.junit.Test;

public class MapWithWeakValuesTest extends Optio3Test
{
    static class TestValue
    {
        final int num;

        public TestValue(int num)
        {
            this.num = num;
        }

        @Override
        public boolean equals(Object o)
        {
            TestValue that = Reflection.as(o, TestValue.class);
            if (that == null)
            {
                return false;
            }

            return that.num == num;
        }

        @Override
        public int hashCode()
        {
            return num;
        }

        @Override
        public String toString()
        {
            return "TestValue{" + "num=" + num + '}';
        }
    }

    static transient TestValue val1;
    static transient TestValue val2;
    static transient TestValue val3;

    static MapWithWeakValues<String, TestValue> map = new MapWithWeakValues<>();

    @Test
    @TestOrder(10)
    public void create()
    {
        val1 = new TestValue(1);
        val2 = new TestValue(2);
        val3 = new TestValue(3);

        map.put("el1", val1);
        map.put("el2", val2);
        map.put("el3", val3);

        assertTrue(map.containsKey("el1"));
        assertTrue(map.containsKey("el2"));
        assertTrue(map.containsKey("el3"));

        //--//

        forceGC();

        boolean[] got = checkEntries();
        assertTrue(got[0]);
        assertTrue(got[1]);
        assertTrue(got[2]);

        //--//

        got = checkKeys();
        assertTrue(got[0]);
        assertTrue(got[1]);
        assertTrue(got[2]);

        //--//

        got = checkValues();
        assertTrue(got[0]);
        assertTrue(got[1]);
        assertTrue(got[2]);
    }

    @Test
    @TestOrder(20)
    public void removeVal1()
    {
        map.remove("el1");

        assertFalse(map.containsKey("el1"));
        assertTrue(map.containsKey("el2"));
        assertTrue(map.containsKey("el3"));

        //--//

        forceGC();

        boolean[] got = checkEntries();
        assertFalse(got[0]);
        assertTrue(got[1]);
        assertTrue(got[2]);

        //--//

        got = checkKeys();
        assertFalse(got[0]);
        assertTrue(got[1]);
        assertTrue(got[2]);

        //--//

        got = checkValues();
        assertFalse(got[0]);
        assertTrue(got[1]);
        assertTrue(got[2]);
    }

    @Test
    @TestOrder(30)
    public void dropVal2()
    {
        val2 = null;

        //--//

        forceGC();

        boolean[] got = checkEntries();
        assertFalse(got[0]);
        assertFalse(got[1]);
        assertTrue(got[2]);

        //--//

        got = checkKeys();
        assertFalse(got[0]);
        assertFalse(got[1]);
        assertTrue(got[2]);

        //--//

        got = checkValues();
        assertFalse(got[0]);
        assertFalse(got[1]);
        assertTrue(got[2]);
    }

    //--//

    private boolean[] checkEntries()
    {
        boolean[] got = new boolean[3];

        for (Map.Entry<String, TestValue> entry : map.entrySet())
        {
            String    key = entry.getKey();
            TestValue val = entry.getValue();
            System.out.printf("checkEntries: Key: %s, Value: %s%n", key, val);

            if ("el1".equals(key))
            {
                got[0] = true;
                assertEquals(val1, val);
            }

            if ("el2".equals(key))
            {
                got[1] = true;
                assertEquals(val2, val);
            }

            if ("el3".equals(key))
            {
                got[2] = true;
                assertEquals(val3, val);
            }
        }

        return got;
    }

    private boolean[] checkKeys()
    {
        boolean[] got = new boolean[3];

        for (String key : map.keySet())
        {
            System.out.printf("checkKeys: %s%n", key);

            if ("el1".equals(key))
            {
                got[0] = true;
            }

            if ("el2".equals(key))
            {
                got[1] = true;
            }

            if ("el3".equals(key))
            {
                got[2] = true;
            }
        }

        return got;
    }

    private boolean[] checkValues()
    {
        boolean[] got = new boolean[3];

        for (TestValue v : map.values())
        {
            System.out.printf("checkValues: %s%n", v);

            if (val1 != null && Objects.equals(val1, v))
            {
                got[0] = true;
            }

            if (val2 != null && Objects.equals(val2, v))
            {
                got[1] = true;
            }

            if (val3 != null && Objects.equals(val3, v))
            {
                got[2] = true;
            }
        }

        return got;
    }

    private static void forceGC()
    {
        for (int i = 0; i < 10; i++)
        {
            Runtime.getRuntime()
                   .gc();
        }
    }
}
