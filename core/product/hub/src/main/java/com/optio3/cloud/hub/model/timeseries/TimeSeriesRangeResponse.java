/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.timeseries;

import java.time.ZonedDateTime;

public class TimeSeriesRangeResponse
{
    public ZonedDateTime firstTimestamp;

    public ZonedDateTime lastTimestamp;

    public int numberOfSamples;
    public int numberOfMissingSamples;

    public double minValue;
    public double maxValue;
    public double averageValue;
}
