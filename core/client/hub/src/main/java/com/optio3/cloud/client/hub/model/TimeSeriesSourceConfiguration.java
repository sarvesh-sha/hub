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

import com.optio3.cloud.client.hub.model.AssetGraphBinding;
import com.optio3.cloud.client.hub.model.TimeDuration;
import com.optio3.cloud.client.hub.model.ToggleableNumericRange;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class TimeSeriesSourceConfiguration
{


  public String uuid = null;
  public String id = null;
  public String dimension = null;
  public String color = null;
  public Double showMovingAverage = null;
  public Boolean onlyShowMovingAverage = null;
  public Boolean showDecimation = null;
  public Integer axis = null;
  public Integer panel = null;
  public ToggleableNumericRange range = null;
  public TimeDuration timeOffset = null;
  public AssetGraphBinding pointBinding = null;
  public TimeSeriesDecimationDisplay decimationDisplay = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class TimeSeriesSourceConfiguration {\n");

    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    dimension: ").append(toIndentedString(dimension)).append("\n");
    sb.append("    color: ").append(toIndentedString(color)).append("\n");
    sb.append("    showMovingAverage: ").append(toIndentedString(showMovingAverage)).append("\n");
    sb.append("    onlyShowMovingAverage: ").append(toIndentedString(onlyShowMovingAverage)).append("\n");
    sb.append("    showDecimation: ").append(toIndentedString(showDecimation)).append("\n");
    sb.append("    axis: ").append(toIndentedString(axis)).append("\n");
    sb.append("    panel: ").append(toIndentedString(panel)).append("\n");
    sb.append("    range: ").append(toIndentedString(range)).append("\n");
    sb.append("    timeOffset: ").append(toIndentedString(timeOffset)).append("\n");
    sb.append("    pointBinding: ").append(toIndentedString(pointBinding)).append("\n");
    sb.append("    decimationDisplay: ").append(toIndentedString(decimationDisplay)).append("\n");
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
