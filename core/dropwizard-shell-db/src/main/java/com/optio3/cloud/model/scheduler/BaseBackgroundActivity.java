/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model.scheduler;

import java.time.ZonedDateTime;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.annotation.Optio3MapToPersistence;
import com.optio3.cloud.logic.BackgroundActivityStatus;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.RecordIdentity;

public abstract class BaseBackgroundActivity extends BaseModel
{
    @Optio3MapAsReadOnly
    public String title;

    @Optio3MapAsReadOnly
    public RecordIdentity context;

    @Optio3MapAsReadOnly
    public BackgroundActivityStatus status;

    @Optio3MapToPersistence("timeoutAsSeconds")
    public long timeout;

    @Optio3MapAsReadOnly
    public ZonedDateTime nextActivation;

    //--//

    @Optio3MapAsReadOnly
    public ZonedDateTime lastActivation;

    @Optio3MapAsReadOnly
    public BackgroundActivityStatus lastActivationStatus;

    @Optio3MapAsReadOnly
    public String lastActivationFailure;

    @Optio3MapAsReadOnly
    public String lastActivationFailureTrace;
}
