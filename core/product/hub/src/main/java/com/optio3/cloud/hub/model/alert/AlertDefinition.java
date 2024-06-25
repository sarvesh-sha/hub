/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.alert;

import java.time.ZonedDateTime;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionVersionRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class AlertDefinition extends BaseModel
{
    public String title;
    public String description;

    public boolean active;

    @Optio3MapAsReadOnly
    public AlertDefinitionPurpose purpose;

    @Optio3MapAsReadOnly
    public ZonedDateTime lastOutput;

    @Optio3MapAsReadOnly
    public int lastOffset;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<AlertDefinitionVersionRecord> headVersion;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<AlertDefinitionVersionRecord> releaseVersion;

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<AlertDefinitionVersionRecord> versions = new TypedRecordIdentityList<>();
}
