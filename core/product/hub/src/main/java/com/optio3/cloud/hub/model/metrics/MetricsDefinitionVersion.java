/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.metrics;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.engine.metrics.MetricsDefinitionDetails;
import com.optio3.cloud.hub.persistence.metrics.MetricsDefinitionRecord;
import com.optio3.cloud.hub.persistence.metrics.MetricsDefinitionVersionRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class MetricsDefinitionVersion extends BaseModel
{
    @Optio3MapAsReadOnly
    public int version;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<MetricsDefinitionRecord> definition;

    @Optio3MapAsReadOnly
    public MetricsDefinitionDetails details;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<MetricsDefinitionVersionRecord> predecessor;

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<MetricsDefinitionVersionRecord> successors;
}
