/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Reactions
{
    public int total_count;

    @JsonProperty("+1")
    public int plus_one;

    @JsonProperty("-1")
    public int minus_one;

    public int laugh;
    public int confused;
    public int heart;
    public int hooray;

    public String url;
}
