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

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes(
{
    @JsonSubTypes.Type(value = AggregationTableWidgetConfiguration.class),
    @JsonSubTypes.Type(value = AggregationTrendWidgetConfiguration.class),
    @JsonSubTypes.Type(value = AggregationWidgetConfiguration.class),
    @JsonSubTypes.Type(value = AlertFeedWidgetConfiguration.class),
    @JsonSubTypes.Type(value = AlertMapWidgetConfiguration.class),
    @JsonSubTypes.Type(value = AlertSummaryWidgetConfiguration.class),
    @JsonSubTypes.Type(value = AlertTableWidgetConfiguration.class),
    @JsonSubTypes.Type(value = AlertTrendWidgetConfiguration.class),
    @JsonSubTypes.Type(value = AssetGraphSelectorWidgetConfiguration.class),
    @JsonSubTypes.Type(value = ControlPointWidgetConfiguration.class),
    @JsonSubTypes.Type(value = DeviceSummaryWidgetConfiguration.class),
    @JsonSubTypes.Type(value = GroupingWidgetConfiguration.class),
    @JsonSubTypes.Type(value = ImageWidgetConfiguration.class),
    @JsonSubTypes.Type(value = TextWidgetConfiguration.class),
    @JsonSubTypes.Type(value = TimeSeriesWidgetConfiguration.class)
})
public class WidgetConfiguration
{


  public String id = null;
  public Integer size = null;
  public String name = null;
  public String description = null;
  public List<String> locations = new ArrayList<String>();
  public Integer refreshRateInSeconds = null;
  public Boolean manualFontScaling = null;
  public Double fontMultiplier = null;
  public WidgetToolbarBehavior toolbarBehavior = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class WidgetConfiguration {\n");

    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    locations: ").append(toIndentedString(locations)).append("\n");
    sb.append("    refreshRateInSeconds: ").append(toIndentedString(refreshRateInSeconds)).append("\n");
    sb.append("    manualFontScaling: ").append(toIndentedString(manualFontScaling)).append("\n");
    sb.append("    fontMultiplier: ").append(toIndentedString(fontMultiplier)).append("\n");
    sb.append("    toolbarBehavior: ").append(toIndentedString(toolbarBehavior)).append("\n");
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