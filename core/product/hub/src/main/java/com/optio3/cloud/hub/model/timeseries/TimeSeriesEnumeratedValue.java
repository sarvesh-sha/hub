/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.timeseries;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TimeSeriesEnumeratedValue
{
    public String name;
    public int    value;

    @JsonIgnore
    public Enum<?> typedValue;
}
