/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.github.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Changes extends HashMap<String, Object>
{
    private static final long serialVersionUID = 1L;

    public Set<String> enumerate(String... path)
    {
        Map<String, Object> loc = findMap(path);

        return loc != null ? Sets.newHashSet(loc.keySet()) : Collections.emptySet();
    }

    public Map<String, String> getValues(String... path)
    {
        Map<String, Object> loc = findMap(path);
        if (loc == null)
        {
            return Collections.emptyMap();
        }

        Map<String, String> res = Maps.newHashMap();
        for (String key : loc.keySet())
        {
            Object val = loc.get(key);
            if (val instanceof String)
            {
                res.put(key, (String) val);
            }
        }
        return res;
    }

    //--//

    private Map<String, Object> findMap(String... path)
    {
        Map<String, Object> loc = this;

        for (String key : path)
        {
            Object val = loc.get(key);
            if (val instanceof Map<?, ?>)
            {
                @SuppressWarnings("unchecked") Map<String, Object> loc2 = (Map<String, Object>) val;

                loc = loc2;
            }
            else
            {
                break;
            }
        }
        return loc;
    }
}
