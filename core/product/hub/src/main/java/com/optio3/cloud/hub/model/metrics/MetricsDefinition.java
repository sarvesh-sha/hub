/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.metrics;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.persistence.metrics.MetricsDefinitionVersionRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class MetricsDefinition extends BaseModel
{
    public String title;
    public String description;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<MetricsDefinitionVersionRecord> headVersion;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<MetricsDefinitionVersionRecord> releaseVersion;

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<MetricsDefinitionVersionRecord> versions = new TypedRecordIdentityList<>();
}
