/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.alert.AlertType;

@JsonTypeName("CustomReportElementAlertFeed")
public class CustomReportElementAlertFeed extends CustomReportElement
{
    public String          label;
    public List<AlertType> alertTypes;
    public List<String>    locations;
}
