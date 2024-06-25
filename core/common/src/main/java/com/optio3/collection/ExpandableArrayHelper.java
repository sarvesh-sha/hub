/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.collection;

import java.lang.ref.Cleaner;

import com.optio3.util.ObjectRecycler;

class ExpandableArrayHelper
{
    static <T> void clear(ObjectRecycler<T> recycler,
                          T[] segments)
    {
        for (int i = 0; i < segments.length; i++)
        {
            T segment = segments[i];
            if (segment != null)
            {
                segments[i] = null;

                recycler.releaseRaw(segment);
            }
        }
    }
}
