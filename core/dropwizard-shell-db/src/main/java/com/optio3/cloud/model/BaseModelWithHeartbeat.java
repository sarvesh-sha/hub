/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model;

import java.time.ZonedDateTime;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;

public abstract class BaseModelWithHeartbeat extends BaseModelWithMetadata
{
    @Optio3MapAsReadOnly
    public ZonedDateTime lastHeartbeat;
}
