/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import com.optio3.cloud.model.RecordIdentity;
import com.optio3.util.TimeUtils;

public class DbEvent
{
    public DbAction       action;
    public RecordIdentity context;

    //--//

    public int compareTo(DbEvent other)
    {
        // Higher priority means put in front.
        int diff = -Integer.compare(this.action.getPriority(), other.action.getPriority());
        if (diff == 0)
        {
            // Use descending ordering.
            diff = -TimeUtils.compare(context.lastUpdate, other.context.lastUpdate);
        }

        return diff;
    }
}
