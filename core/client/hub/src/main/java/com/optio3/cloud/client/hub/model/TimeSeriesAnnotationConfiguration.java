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

public class TimeSeriesAnnotationConfiguration
{


  public Boolean hideTooltip = null;
  public String title = null;
  public String description = null;
  public Integer panel = null;
  public String sourceId = null;
  public TimeSeriesAnnotationType type = null;
  public Double minX = null;
  public Double maxX = null;
  public Double minY = null;
  public Double maxY = null;
  public Double tooltipOffsetX = null;
  public Double tooltipOffsetY = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class TimeSeriesAnnotationConfiguration {\n");

    sb.append("    hideTooltip: ").append(toIndentedString(hideTooltip)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    panel: ").append(toIndentedString(panel)).append("\n");
    sb.append("    sourceId: ").append(toIndentedString(sourceId)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    minX: ").append(toIndentedString(minX)).append("\n");
    sb.append("    maxX: ").append(toIndentedString(maxX)).append("\n");
    sb.append("    minY: ").append(toIndentedString(minY)).append("\n");
    sb.append("    maxY: ").append(toIndentedString(maxY)).append("\n");
    sb.append("    tooltipOffsetX: ").append(toIndentedString(tooltipOffsetX)).append("\n");
    sb.append("    tooltipOffsetY: ").append(toIndentedString(tooltipOffsetY)).append("\n");
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