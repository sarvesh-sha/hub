/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.SummaryFlavor;
import com.optio3.cloud.hub.model.alert.AlertSeverity;
import com.optio3.cloud.hub.model.alert.AlertStatus;
import com.optio3.cloud.hub.model.alert.AlertType;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.model.schedule.FilterableTimeRange;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;

@JsonTypeName("AlertTableWidgetConfiguration")
public class AlertTableWidgetConfiguration extends WidgetConfiguration
{
    public List<FilterableTimeRange>                      filterableRanges;
    public SummaryFlavor                                  groupBy;
    public LocationType                                   rollupType;
    public List<AlertStatus>                              alertStatusIDs;
    public List<AlertType>                                alertTypeIDs;
    public List<AlertSeverity>                            alertSeverityIDs;
    public List<AlertMapSeverityColor>                    severityColors;
    public TypedRecordIdentityList<AlertDefinitionRecord> alertRules;
}
