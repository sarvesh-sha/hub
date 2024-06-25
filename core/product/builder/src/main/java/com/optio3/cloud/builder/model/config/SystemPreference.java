/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.config;

import java.time.ZonedDateTime;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;

public class SystemPreference
{
    @Optio3MapAsReadOnly
    public String sysId;

    @Optio3MapAsReadOnly
    public ZonedDateTime createdOn;

    @Optio3MapAsReadOnly
    public ZonedDateTime updatedOn;

    //--//

    public String key;

    public String value;
}
