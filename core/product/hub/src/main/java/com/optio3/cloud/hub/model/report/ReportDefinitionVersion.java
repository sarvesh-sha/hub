/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.persistence.report.ReportDefinitionRecord;
import com.optio3.cloud.hub.persistence.report.ReportDefinitionVersionRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class ReportDefinitionVersion extends BaseModel
{
    @Optio3MapAsReadOnly
    public int version;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<ReportDefinitionRecord> definition;

    @Optio3MapAsReadOnly
    public ReportDefinitionDetails details;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<ReportDefinitionVersionRecord> predecessor;

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<ReportDefinitionVersionRecord> successors;
}
