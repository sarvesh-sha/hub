/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model;

import java.util.List;

import com.google.common.collect.Lists;

public class ValidationResults
{
    public List<ValidationResult> entries = Lists.newArrayList();

    //--//

    @Override
    public String toString()
    {
        return "ValidationResults{" + "entries=" + entries + '}';
    }
}
