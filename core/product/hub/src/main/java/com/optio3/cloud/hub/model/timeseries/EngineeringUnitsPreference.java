/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.timeseries;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;
import com.optio3.protocol.model.EngineeringUnitsFactors;

@Optio3IncludeInApiDefinitions
public class EngineeringUnitsPreference
{
    public static class Pair
    {
        public EngineeringUnitsFactors key;
        public EngineeringUnitsFactors selected;
    }

    public final List<Pair> units = Lists.newArrayList();
}

