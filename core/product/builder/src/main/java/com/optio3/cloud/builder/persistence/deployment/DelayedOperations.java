/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment;

import java.util.List;

import com.google.common.collect.Lists;

public class DelayedOperations
{
    public final List<DelayedOperation> ops = Lists.newArrayList();

    public static boolean isValid(DelayedOperations obj)
    {
        return obj != null && !obj.ops.isEmpty();
    }
}
