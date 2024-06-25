/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = CustomReportElementAggregatedValue.class),
                @JsonSubTypes.Type(value = CustomReportElementAggregationTable.class),
                @JsonSubTypes.Type(value = CustomReportElementAggregationTrend.class),
                @JsonSubTypes.Type(value = CustomReportElementAlertFeed.class),
                @JsonSubTypes.Type(value = CustomReportElementAlertTable.class),
                @JsonSubTypes.Type(value = CustomReportElementAlertExecution.class),
                @JsonSubTypes.Type(value = CustomReportElementChartSet.class),
                @JsonSubTypes.Type(value = CustomReportElementDeviceElementList.class),
                @JsonSubTypes.Type(value = CustomReportElementPageBreak.class),
                @JsonSubTypes.Type(value = CustomReportElementRichText.class) })
public abstract class CustomReportElement
{
}
