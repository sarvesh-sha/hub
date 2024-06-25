/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.visualization.TimeSeriesChartConfiguration;

@JsonTypeName("CustomReportElementChartSet")
public class CustomReportElementChartSet extends CustomReportElement
{
    public List<TimeSeriesChartConfiguration> charts;
}
