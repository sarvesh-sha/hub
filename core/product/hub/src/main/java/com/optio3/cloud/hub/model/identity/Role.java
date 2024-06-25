/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.identity;

import com.optio3.cloud.annotation.Optio3AutoTrim;
import com.optio3.cloud.model.BaseModel;

public class Role extends BaseModel
{
    @Optio3AutoTrim()
    public String name;

    @Optio3AutoTrim()
    public String displayName;

    public boolean addAllowed;

    public boolean removeAllowed;
}
