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

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes(
{
    @JsonSubTypes.Type(value = SensorConfigForArgoHytos.class),
    @JsonSubTypes.Type(value = SensorConfigForBergstrom.class),
    @JsonSubTypes.Type(value = SensorConfigForBluesky.class),
    @JsonSubTypes.Type(value = SensorConfigForEpSolar.class),
    @JsonSubTypes.Type(value = SensorConfigForGps.class),
    @JsonSubTypes.Type(value = SensorConfigForHendricksonWatchman.class),
    @JsonSubTypes.Type(value = SensorConfigForHolykell.class),
    @JsonSubTypes.Type(value = SensorConfigForI2CHub.class),
    @JsonSubTypes.Type(value = SensorConfigForJ1939.class),
    @JsonSubTypes.Type(value = SensorConfigForMontageBluetoothGateway.class),
    @JsonSubTypes.Type(value = SensorConfigForPalfinger.class),
    @JsonSubTypes.Type(value = SensorConfigForRawCANbus.class),
    @JsonSubTypes.Type(value = SensorConfigForStealthPower.class),
    @JsonSubTypes.Type(value = SensorConfigForTriStar.class),
    @JsonSubTypes.Type(value = SensorConfigForVictron.class)
})
public class SensorConfig
{

  public Integer seconds = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class SensorConfig {\n");

    sb.append("    seconds: ").append(toIndentedString(seconds)).append("\n");
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