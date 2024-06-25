/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.normalization;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.logic.normalizations.NormalizationRules;
import com.optio3.cloud.model.BaseModel;

public class Normalization extends BaseModel
{
    @Optio3MapAsReadOnly
    public int version;

    @Optio3MapAsReadOnly
    public boolean active;

    public NormalizationRules rules;
}
