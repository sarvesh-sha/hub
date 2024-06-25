/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.timeseries;

public class TimeSeriesMultiPropertyResponse extends TimeSeriesBaseResponse
{
    public TimeSeriesPropertyResponse[] results;

    public boolean deltaEncoded;
}
