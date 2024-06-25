/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.normalizations;

import java.util.List;

import com.google.common.collect.Lists;

public class ValidationPointClassRule
{
    public String pointClassId;

    public boolean isBinary;

    public double minValue;

    public double maxValue;

    public final List<String> allowableObjectTypes = Lists.newArrayList();
}
