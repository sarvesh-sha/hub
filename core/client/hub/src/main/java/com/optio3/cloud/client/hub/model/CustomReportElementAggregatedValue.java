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

import com.optio3.cloud.client.hub.model.ControlPointsGroup;
import com.optio3.cloud.client.hub.model.CustomReportElement;
import com.optio3.cloud.client.hub.model.RecurringWeeklySchedule;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeName("CustomReportElementAggregatedValue")
public class CustomReportElementAggregatedValue extends CustomReportElement
{

  public String label = null;
  public ControlPointsGroup controlPointGroup = null;
  public Boolean isFilterApplied = null;
  public RecurringWeeklySchedule filter = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class CustomReportElementAggregatedValue {\n");
    sb.append("    ").append(super.toString().replace("\n", "\n    ")).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    controlPointGroup: ").append(toIndentedString(controlPointGroup)).append("\n");
    sb.append("    isFilterApplied: ").append(toIndentedString(isFilterApplied)).append("\n");
    sb.append("    filter: ").append(toIndentedString(filter)).append("\n");
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
