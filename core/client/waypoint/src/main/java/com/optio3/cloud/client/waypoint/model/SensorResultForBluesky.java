/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Waypoint APIs
 * APIs and Definitions for the Optio3 Waypoint product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.cloud.client.waypoint.model;

import com.optio3.cloud.client.waypoint.model.SensorResult;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeName("SensorResultForBluesky")
public class SensorResultForBluesky extends SensorResult
{

  public Float inputVoltage = null;
  public Float inputCurrent = null;
  public Float batteryVoltage = null;
  public Float batteryCurrent = null;
  public Float totalChargeAH = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class SensorResultForBluesky {\n");
    sb.append("    ").append(super.toString().replace("\n", "\n    ")).append("\n");
    sb.append("    inputVoltage: ").append(toIndentedString(inputVoltage)).append("\n");
    sb.append("    inputCurrent: ").append(toIndentedString(inputCurrent)).append("\n");
    sb.append("    batteryVoltage: ").append(toIndentedString(batteryVoltage)).append("\n");
    sb.append("    batteryCurrent: ").append(toIndentedString(batteryCurrent)).append("\n");
    sb.append("    totalChargeAH: ").append(toIndentedString(totalChargeAH)).append("\n");
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
