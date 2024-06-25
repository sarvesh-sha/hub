/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.bookmark;

import java.util.Map;

import com.google.common.collect.Maps;

public class ViewStateSerialized
{
    public Map<String, ViewStateItem> state = Maps.newHashMap();

    public Map<String, ViewStateSerialized> subStates = Maps.newHashMap();
}
