/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.model.alert.Alert;
import com.optio3.cloud.hub.model.audit.Audit;
import com.optio3.cloud.hub.model.workflow.Workflow;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = Alert.class), @JsonSubTypes.Type(value = Audit.class), @JsonSubTypes.Type(value = Workflow.class) })
public class Event extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<AssetRecord> asset;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<LocationRecord> location;

    @Optio3MapAsReadOnly
    public int sequenceNumber;

    public String description;
    public String extendedDescription;
}
