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

@JsonTypeName("SensorResultForVictron")
public class SensorResultForVictron extends SensorResult
{

  public Float batteryVoltage = null;
  public Float batteryCurrent = null;
  public Float panelVoltage = null;
  public Float panelPower = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class SensorResultForVictron {\n");
    sb.append("    ").append(super.toString().replace("\n", "\n    ")).append("\n");
    sb.append("    batteryVoltage: ").append(toIndentedString(batteryVoltage)).append("\n");
    sb.append("    batteryCurrent: ").append(toIndentedString(batteryCurrent)).append("\n");
    sb.append("    panelVoltage: ").append(toIndentedString(panelVoltage)).append("\n");
    sb.append("    panelPower: ").append(toIndentedString(panelPower)).append("\n");
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
