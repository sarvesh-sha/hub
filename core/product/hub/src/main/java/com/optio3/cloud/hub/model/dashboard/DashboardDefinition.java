/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.dashboard.DashboardDefinitionVersionRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class DashboardDefinition extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<UserRecord> user;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<DashboardDefinitionVersionRecord> headVersion;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<DashboardDefinitionVersionRecord> releaseVersion;
}
