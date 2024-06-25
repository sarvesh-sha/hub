/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.normalizations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NormalizationMatch
{
    static final NormalizationMatch NoMatchSentinel = new NormalizationMatch(null, null, null, null);

    public final NormalizationMatchKind reason;
    public final String                 match;
    public final String                 input;
    public final String                 output;

    @JsonCreator
    public NormalizationMatch(@JsonProperty("reason") NormalizationMatchKind reason,
                              @JsonProperty("match") String match,
                              @JsonProperty("input") String input,
                              @JsonProperty("output") String output)
    {
        this.reason = reason;
        this.match  = match;
        this.input  = input;
        this.output = output;
    }
}
