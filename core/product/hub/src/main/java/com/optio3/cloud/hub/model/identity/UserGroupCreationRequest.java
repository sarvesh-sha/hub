/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.identity;

import com.optio3.cloud.annotation.Optio3AutoTrim;

public class UserGroupCreationRequest
{
    @Optio3AutoTrim()
    public String name;

    @Optio3AutoTrim()
    public String description;
}
