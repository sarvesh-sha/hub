/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.persistence.dashboard.DashboardDefinitionRecord;
import com.optio3.cloud.hub.persistence.dashboard.DashboardDefinitionVersionRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class DashboardDefinitionVersion extends BaseModel
{
    @Optio3MapAsReadOnly
    public int version;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<DashboardDefinitionRecord> definition;

    @Optio3MapAsReadOnly
    public DashboardConfiguration details;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<DashboardDefinitionVersionRecord> predecessor;
}
