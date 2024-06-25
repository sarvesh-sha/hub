/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.visualization;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;

@Optio3IncludeInApiDefinitions
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = TimeSeriesChartConfiguration.class) })
public abstract class TimeSeriesConfigurationBase
{
    @Optio3MapAsReadOnly
    public int version = 1;
}
