/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.orchestration.state;

public class SamplingRequest
{
    public final long nowEpochSeconds;
    public final int  period;

    public SamplingRequest(long nowEpochSeconds,
                           int period)
    {
        this.nowEpochSeconds = nowEpochSeconds;
        this.period          = period;
    }
}
