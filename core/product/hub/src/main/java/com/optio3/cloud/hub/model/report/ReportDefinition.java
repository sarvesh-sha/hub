/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import java.time.ZonedDateTime;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.report.ReportDefinitionVersionRecord;
import com.optio3.cloud.hub.persistence.report.ReportRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class ReportDefinition extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<UserRecord> user;
    public String                          title;
    public String                          description;
    public boolean                         active;

    public ZonedDateTime autoDelete;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<ReportDefinitionVersionRecord> headVersion;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<ReportDefinitionVersionRecord> releaseVersion;

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<ReportRecord> reports = new TypedRecordIdentityList<>();

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<ReportDefinitionVersionRecord> versions = new TypedRecordIdentityList<>();
}
