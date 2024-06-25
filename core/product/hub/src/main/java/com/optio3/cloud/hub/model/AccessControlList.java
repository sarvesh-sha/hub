/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model;

import java.time.ZonedDateTime;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;

public class AccessControlList
{
    @Optio3MapAsReadOnly
    public int sequence;

    @Optio3MapAsReadOnly
    public ZonedDateTime createdOn;

    @Optio3MapAsReadOnly
    public ZonedDateTime updatedOn;

    public AccessControlListPolicy policy;
}
