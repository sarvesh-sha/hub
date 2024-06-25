/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.tags;

import com.optio3.collection.ExpandableArrayOf;
import org.apache.commons.lang3.StringUtils;

class KeyValuePairArray extends ExpandableArrayOf<KeyValuePair>
{
    KeyValuePairArray()
    {
        super(KeyValuePair.class);
    }

    @Override
    protected ExpandableArrayOf<KeyValuePair> allocate()
    {
        return new KeyValuePairArray();
    }

    @Override
    protected int compare(KeyValuePair o1,
                          KeyValuePair o2)
    {
        int diff = StringUtils.compareIgnoreCase(o1.key, o2.key);
        if (diff == 0)
        {
            diff = StringUtils.compareIgnoreCase(o1.value, o2.value);
        }

        return diff;
    }
}
