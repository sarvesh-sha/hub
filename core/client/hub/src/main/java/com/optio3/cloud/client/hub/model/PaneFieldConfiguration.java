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

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes(
{
    @JsonSubTypes.Type(value = PaneFieldConfigurationAggregatedValue.class),
    @JsonSubTypes.Type(value = PaneFieldConfigurationAlertCount.class),
    @JsonSubTypes.Type(value = PaneFieldConfigurationAlertFeed.class),
    @JsonSubTypes.Type(value = PaneFieldConfigurationChart.class),
    @JsonSubTypes.Type(value = PaneFieldConfigurationCurrentValue.class),
    @JsonSubTypes.Type(value = PaneFieldConfigurationPathMap.class)
})
public class PaneFieldConfiguration
{

  public String label = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class PaneFieldConfiguration {\n");

    sb.append("    label: ").append(toIndentedString(label)).append("\n");
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