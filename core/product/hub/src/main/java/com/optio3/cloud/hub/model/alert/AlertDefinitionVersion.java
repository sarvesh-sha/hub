/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.alert;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.engine.alerts.AlertDefinitionDetails;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionRecord;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionVersionRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class AlertDefinitionVersion extends BaseModel
{
    @Optio3MapAsReadOnly
    public int version;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<AlertDefinitionRecord> definition;

    @Optio3MapAsReadOnly
    public AlertDefinitionDetails details;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<AlertDefinitionVersionRecord> predecessor;

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<AlertDefinitionVersionRecord> successors;
}
