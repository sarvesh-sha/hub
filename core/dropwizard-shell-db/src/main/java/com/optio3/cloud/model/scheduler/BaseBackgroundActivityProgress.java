/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model.scheduler;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.logic.BackgroundActivityStatus;

public abstract class BaseBackgroundActivityProgress
{
    @Optio3MapAsReadOnly
    public BackgroundActivityStatus status;
}
