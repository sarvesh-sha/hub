/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionRecord;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionVersionRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeName("CustomReportElementAlertExecution")
public class CustomReportElementAlertExecution extends CustomReportElement
{
    public TypedRecordIdentity<AlertDefinitionRecord>        definition;
    public TypedRecordIdentity<AlertDefinitionVersionRecord> version;
}
