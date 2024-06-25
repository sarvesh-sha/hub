/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.optio3.lang.RunnableWithException;
import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned32;
import com.optio3.lang.Unsigned8;
import com.optio3.util.Exceptions;
import com.optio3.util.Resources;
import org.junit.runner.RunWith;

@RunWith(Optio3Runner.class)
public abstract class Optio3Test
{
    private int m_runCounter;

    protected boolean failedOnFirstRun()
    {
        return m_runCounter > 0;
    }

    protected int getRunCounter()
    {
        return m_runCounter;
    }

    void setRunCounter(int retry)
    {
        m_runCounter = retry;
    }

    //--//

    protected BufferedReader openResourceAsBufferedReader(String resource)
    {
        return Resources.openResourceAsBufferedReader(this.getClass(), resource);
    }

    protected URL openResource(String resource)
    {
        return Resources.openResource(this.getClass(), resource);
    }

    protected List<String> loadLines(BufferedReader reader,
                                     boolean skipLinesStartingWithComments) throws
                                                                            IOException
    {
        return Resources.loadLines(reader, skipLinesStartingWithComments);
    }

    protected String loadResourceAsText(String resource,
                                        boolean skipLinesStartingWithComments) throws
                                                                               IOException
    {
        return Resources.loadResourceAsText(this.getClass(), resource, skipLinesStartingWithComments);
    }

    protected List<String> loadResourceAsLines(String resource,
                                               boolean skipLinesStartingWithComments) throws
                                                                                      IOException
    {
        return Resources.loadResourceAsLines(this.getClass(), resource, skipLinesStartingWithComments);
    }

    //--//

    protected static <T extends Exception> void assertFailure(Class<T> cls,
                                                              RunnableWithException callback)
    {
        try
        {
            callback.run();
            assertFalse(String.format("Expecting exception of type %s, got nothing", cls.getTypeName()), true);
        }
        catch (Throwable e)
        {
            if (cls.isInstance(e))
            {
                return;
            }

            e = Exceptions.unwrapException(e);

            if (e instanceof AssertionError)
            {
                throw (AssertionError) e;
            }

            assertTrue(String.format("Expecting exception of type %s, got %s",
                                     cls.getTypeName(),
                                     e.getClass()
                                      .getTypeName()), cls.isInstance(e));
        }
    }

    protected static <T> T assertCast(Class<T> clz,
                                      Object value)
    {
        assertNotNull(value);
        assertEquals(clz, value.getClass());

        return clz.cast(value);
    }

    protected static void assertArrayEquals(byte[] expected,
                                            byte[] got)
    {
        if (expected != null)
        {
            assertNotNull(got);
        }

        if (expected == null)
        {
            assertNull(got);
        }

        assertEquals(expected.length, got.length);

        for (int i = 0; i < expected.length; i++)
        {
            if (expected[i] != got[i])
            {
                fail(String.format("Values at offset %d differ: Expected=%d Got=%d", i, expected[i], got[i]));
            }
        }
    }

    protected static <T> void assertArrayEquals(T[] expected,
                                                T[] got)
    {
        if (expected != null)
        {
            assertNotNull(got);
        }

        if (expected == null)
        {
            assertNull(got);
        }

        assertEquals(expected.length, got.length);

        for (int i = 0; i < expected.length; i++)
        {
            if (!Objects.equals(expected[i], got[i]))
            {
                fail(String.format("Values at offset %d differ: Expected=%s Got=%s", i, expected[i], got[i]));
            }
        }
    }

    protected static <E> void assertCollectionEquals(Collection<E> expected,
                                                     Collection<E> got)
    {
        if (expected != null)
        {
            assertNotNull(got);
        }

        if (expected == null)
        {
            assertNull(got);
        }

        assertEquals("Different number of elements in collections", expected.size(), got.size());

        Iterator<E> expectedIt = expected.iterator();
        Iterator<E> gotIt      = got.iterator();
        int         index      = 0;

        while (expectedIt.hasNext() && gotIt.hasNext())
        {
            E expectedVal = expectedIt.next();
            E gotVal      = gotIt.next();

            assertEquals(String.format("Values at index %d differ", index), expectedVal, gotVal);

            index++;
        }
    }

    protected void assertUnsignedEquals(Number val,
                                        Unsigned8 val2)
    {
        assertNotNull(val2);

        assertEquals(val.longValue(), val2.unboxUnsigned());
    }

    protected void assertUnsignedEquals(Number val,
                                        Unsigned16 val2)
    {
        assertNotNull(val2);

        assertEquals(val.longValue(), val2.unboxUnsigned());
    }

    protected void assertUnsignedEquals(Number val,
                                        Unsigned32 val2)
    {
        assertNotNull(val2);

        assertEquals(val.longValue(), val2.unboxUnsigned());
    }
}
