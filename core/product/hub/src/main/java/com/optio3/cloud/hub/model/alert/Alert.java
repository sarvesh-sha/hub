/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.alert;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.model.event.Event;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionVersionRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeName("Alert")
public class Alert extends Event
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<AlertDefinitionVersionRecord> alertDefinitionVersion;

    @Optio3MapAsReadOnly // Handled explicitly.
    public AlertStatus status;

    @Optio3MapAsReadOnly
    public AlertType type;

    public AlertSeverity severity;
}
