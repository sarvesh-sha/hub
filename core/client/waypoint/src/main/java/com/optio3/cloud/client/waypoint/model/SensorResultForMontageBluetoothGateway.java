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

@JsonTypeName("SensorResultForMontageBluetoothGateway")
public class SensorResultForMontageBluetoothGateway extends SensorResult
{

  public Boolean detectedHeartbeat = null;
  public Boolean detectedPixelTag = null;
  public Boolean detectedSmartLock = null;
  public Boolean detectedTRH = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class SensorResultForMontageBluetoothGateway {\n");
    sb.append("    ").append(super.toString().replace("\n", "\n    ")).append("\n");
    sb.append("    detectedHeartbeat: ").append(toIndentedString(detectedHeartbeat)).append("\n");
    sb.append("    detectedPixelTag: ").append(toIndentedString(detectedPixelTag)).append("\n");
    sb.append("    detectedSmartLock: ").append(toIndentedString(detectedSmartLock)).append("\n");
    sb.append("    detectedTRH: ").append(toIndentedString(detectedTRH)).append("\n");
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
