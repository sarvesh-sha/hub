/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import com.optio3.util.TimeUtils;

@MappedSuperclass
public abstract class RecordWithHeartbeat extends RecordWithMetadata
{
    @Column(name = "last_heartbeat")
    private ZonedDateTime lastHeartbeat;

    //--//

    public ZonedDateTime getLastHeartbeat()
    {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(ZonedDateTime lastHeartbeat)
    {
        this.lastHeartbeat = lastHeartbeat;
    }

    //--//

    public boolean gotHeartbeatRecently(int amount,
                                        TimeUnit unit)
    {
        return TimeUtils.wasUpdatedRecently(getLastHeartbeat(), amount, unit);
    }
}
