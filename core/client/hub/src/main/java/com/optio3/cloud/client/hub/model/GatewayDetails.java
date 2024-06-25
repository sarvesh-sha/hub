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

import com.optio3.cloud.client.hub.model.GatewayQueueStatus;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class GatewayDetails
{

  public ZonedDateTime lastRefresh = null;
  public Integer availableProcessors = null;
  public Long freeMemory = null;
  public Long totalMemory = null;
  public Long maxMemory = null;
  public Integer hardwareVersion = null;
  public Integer firmwareVersion = null;
  public Map<String, String> networkInterfaces = new HashMap<String, String>();
  public GatewayQueueStatus queueStatus = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class GatewayDetails {\n");

    sb.append("    lastRefresh: ").append(toIndentedString(lastRefresh)).append("\n");
    sb.append("    availableProcessors: ").append(toIndentedString(availableProcessors)).append("\n");
    sb.append("    freeMemory: ").append(toIndentedString(freeMemory)).append("\n");
    sb.append("    totalMemory: ").append(toIndentedString(totalMemory)).append("\n");
    sb.append("    maxMemory: ").append(toIndentedString(maxMemory)).append("\n");
    sb.append("    hardwareVersion: ").append(toIndentedString(hardwareVersion)).append("\n");
    sb.append("    firmwareVersion: ").append(toIndentedString(firmwareVersion)).append("\n");
    sb.append("    networkInterfaces: ").append(toIndentedString(networkInterfaces)).append("\n");
    sb.append("    queueStatus: ").append(toIndentedString(queueStatus)).append("\n");
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
