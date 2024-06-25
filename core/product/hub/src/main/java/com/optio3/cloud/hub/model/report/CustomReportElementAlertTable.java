package com.optio3.cloud.hub.model.report;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.SummaryFlavor;
import com.optio3.cloud.hub.model.alert.AlertSeverity;
import com.optio3.cloud.hub.model.alert.AlertStatus;
import com.optio3.cloud.hub.model.alert.AlertType;
import com.optio3.cloud.hub.model.dashboard.AlertMapSeverityColor;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;

@JsonTypeName("CustomReportElementAlertTable")
public class CustomReportElementAlertTable extends CustomReportElement
{
    public String                                         label;
    public SummaryFlavor                                  groupBy;
    public LocationType                                   rollupType;
    public List<AlertStatus>                              alertStatusIDs;
    public List<AlertType>                                alertTypeIDs;
    public List<AlertSeverity>                            alertSeverityIDs;
    public List<String>                                   locations;
    public List<AlertMapSeverityColor>                    severityColors;
    public TypedRecordIdentityList<AlertDefinitionRecord> alertRules;
}