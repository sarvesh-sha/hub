/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

public class DeploymentCellularCommunications
{
    public static class Details
    {
        public int   totalBytes;
        public int[] bytesByDay;
    }

    public final Map<String, Details> sessions = Maps.newHashMap();

    //--//

    public void addEntries(String address,
                           int[] bytes)
    {
        synchronized (sessions)
        {
            var details = new Details();
            details.bytesByDay = bytes;

            for (int i : bytes)
            {
                details.totalBytes += i;
            }

            sessions.put(address, details);
        }
    }
}
