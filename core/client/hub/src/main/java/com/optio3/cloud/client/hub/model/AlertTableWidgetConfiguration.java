/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Hub APIs
 * APIs and Definitions for the Optio3 Hub product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.cloud.client.hub.model;

import com.optio3.cloud.client.hub.model.AlertMapSeverityColor;
import com.optio3.cloud.client.hub.model.FilterableTimeRange;
import com.optio3.cloud.client.hub.model.RecordIdentity;
import com.optio3.cloud.client.hub.model.WidgetConfiguration;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeName("AlertTableWidgetConfiguration")
public class AlertTableWidgetConfiguration extends WidgetConfiguration
{






  public List<FilterableTimeRange> filterableRanges = new ArrayList<FilterableTimeRange>();
  public SummaryFlavor groupBy = null;
  public LocationType rollupType = null;
  public List<AlertStatus> alertStatusIDs = new ArrayList<AlertStatus>();
  public List<AlertType> alertTypeIDs = new ArrayList<AlertType>();
  public List<AlertSeverity> alertSeverityIDs = new ArrayList<AlertSeverity>();
  public List<AlertMapSeverityColor> severityColors = new ArrayList<AlertMapSeverityColor>();
  public List<RecordIdentity> alertRules = new ArrayList<RecordIdentity>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class AlertTableWidgetConfiguration {\n");
    sb.append("    ").append(super.toString().replace("\n", "\n    ")).append("\n");
    sb.append("    filterableRanges: ").append(toIndentedString(filterableRanges)).append("\n");
    sb.append("    groupBy: ").append(toIndentedString(groupBy)).append("\n");
    sb.append("    rollupType: ").append(toIndentedString(rollupType)).append("\n");
    sb.append("    alertStatusIDs: ").append(toIndentedString(alertStatusIDs)).append("\n");
    sb.append("    alertTypeIDs: ").append(toIndentedString(alertTypeIDs)).append("\n");
    sb.append("    alertSeverityIDs: ").append(toIndentedString(alertSeverityIDs)).append("\n");
    sb.append("    severityColors: ").append(toIndentedString(severityColors)).append("\n");
    sb.append("    alertRules: ").append(toIndentedString(alertRules)).append("\n");
    sb.append("}");
    return sb.toString();
  }
  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private static String toIndentedString(java.lang.Object o)
  {
    if (o == null)
      return "null";
    return o.toString().replace("\n", "\n    ");
  }
}
