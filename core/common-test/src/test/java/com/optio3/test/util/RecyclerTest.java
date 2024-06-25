/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.util;

import static org.junit.Assert.assertTrue;

import com.optio3.test.common.Optio3Test;
import com.optio3.util.ObjectRecycler;
import org.junit.Test;

public class RecyclerTest extends Optio3Test
{
    @Test
    public void testArrayRecycler()
    {
        ObjectRecycler<byte[]> ar = ObjectRecycler.build(2, 0, byte[].class, () -> new byte[128], null);

        ObjectRecycler<byte[]>.Holder h1 = ar.acquire();
        byte[]                        b1 = h1.get();

        ObjectRecycler<byte[]>.Holder h2 = ar.acquire();
        byte[]                        b2 = h2.get();

        ObjectRecycler<byte[]>.Holder h3 = ar.acquire();
        byte[]                        b3 = h3.get();

        h1.close();
        h2.close();
        h3.close();

        h1 = ar.acquire();
        assertTrue(b2 == h1.get());

        h2 = ar.acquire();
        assertTrue(b1 == h2.get());

        h3 = ar.acquire();
        assertTrue(b3 != h3.get());
    }
}
