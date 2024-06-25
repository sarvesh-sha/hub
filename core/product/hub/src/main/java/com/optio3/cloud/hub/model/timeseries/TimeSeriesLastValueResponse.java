/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.timeseries;

import java.time.ZonedDateTime;

public class TimeSeriesLastValueResponse
{
    public ZonedDateTime timestamp;
    public Object        value;
}
