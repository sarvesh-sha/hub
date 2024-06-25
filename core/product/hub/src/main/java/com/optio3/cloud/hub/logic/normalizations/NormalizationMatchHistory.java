/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.normalizations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NormalizationMatchHistory
{
    public final NormalizationMatch match;
    public final String             before;
    public final String             after;

    @JsonCreator
    public NormalizationMatchHistory(@JsonProperty("match") NormalizationMatch match,
                                     @JsonProperty("before") String before,
                                     @JsonProperty("after") String after)
    {
        this.match  = match;
        this.before = before;
        this.after  = after;
    }
}
