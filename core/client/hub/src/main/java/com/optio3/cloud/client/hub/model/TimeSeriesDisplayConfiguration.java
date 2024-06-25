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

public class TimeSeriesDisplayConfiguration
{

  public String title = null;
  public Integer size = null;
  public Boolean fillArea = null;
  public Boolean hideDecimation = null;
  public Boolean automaticAggregation = null;
  public Integer panelSpacing = null;
  public Boolean showAlerts = null;
  public Boolean hideSources = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class TimeSeriesDisplayConfiguration {\n");

    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    fillArea: ").append(toIndentedString(fillArea)).append("\n");
    sb.append("    hideDecimation: ").append(toIndentedString(hideDecimation)).append("\n");
    sb.append("    automaticAggregation: ").append(toIndentedString(automaticAggregation)).append("\n");
    sb.append("    panelSpacing: ").append(toIndentedString(panelSpacing)).append("\n");
    sb.append("    showAlerts: ").append(toIndentedString(showAlerts)).append("\n");
    sb.append("    hideSources: ").append(toIndentedString(hideSources)).append("\n");
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