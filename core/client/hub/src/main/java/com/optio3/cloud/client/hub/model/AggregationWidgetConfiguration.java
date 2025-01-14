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
import com.optio3.cloud.client.hub.model.FilterableTimeRange;
import com.optio3.cloud.client.hub.model.WidgetConfiguration;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeName("AggregationWidgetConfiguration")
public class AggregationWidgetConfiguration extends WidgetConfiguration
{

  public ControlPointsGroup controlPointGroup = null;
  public FilterableTimeRange filterableRange = null;
  public Boolean hideRange = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class AggregationWidgetConfiguration {\n");
    sb.append("    ").append(super.toString().replace("\n", "\n    ")).append("\n");
    sb.append("    controlPointGroup: ").append(toIndentedString(controlPointGroup)).append("\n");
    sb.append("    filterableRange: ").append(toIndentedString(filterableRange)).append("\n");
    sb.append("    hideRange: ").append(toIndentedString(hideRange)).append("\n");
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
