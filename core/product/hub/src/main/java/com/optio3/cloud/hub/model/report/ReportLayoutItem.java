/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("ReportLayoutItem")
public class ReportLayoutItem extends ReportLayoutBase
{
    public CustomReportElement element;
}
