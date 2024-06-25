/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.alert;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;

public class AlertHistory extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<AlertRecord> alert;

    @Optio3MapAsReadOnly
    public AlertEventLevel level;

    @Optio3MapAsReadOnly
    public AlertEventType type;

    @Optio3MapAsReadOnly
    public String text;
}
