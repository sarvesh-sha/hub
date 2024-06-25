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

import com.optio3.cloud.client.hub.model.ProberOperation;
import com.optio3.cloud.client.hub.model.ProberOperationBaseResults;
import com.optio3.cloud.client.hub.model.RecordIdentity;
import java.time.ZonedDateTime;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class GatewayProberOperation
{
  public static final String RECORD_IDENTITY = "GatewayProberOperation";


  public String sysId = null;
  public ZonedDateTime createdOn = null;
  public ZonedDateTime updatedOn = null;
  public ZonedDateTime lastHeartbeat = null;
  public RecordIdentity gateway = null;
  public ProberOperation inputDetails = null;
  public ProberOperationBaseResults outputDetails = null;
  public ZonedDateTime lastOutput = null;
  public Integer lastOffset = null;
  public RecordIdentity currentActivity = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class GatewayProberOperation {\n");

    sb.append("    sysId: ").append(toIndentedString(sysId)).append("\n");
    sb.append("    createdOn: ").append(toIndentedString(createdOn)).append("\n");
    sb.append("    updatedOn: ").append(toIndentedString(updatedOn)).append("\n");
    sb.append("    lastHeartbeat: ").append(toIndentedString(lastHeartbeat)).append("\n");
    sb.append("    gateway: ").append(toIndentedString(gateway)).append("\n");
    sb.append("    inputDetails: ").append(toIndentedString(inputDetails)).append("\n");
    sb.append("    outputDetails: ").append(toIndentedString(outputDetails)).append("\n");
    sb.append("    lastOutput: ").append(toIndentedString(lastOutput)).append("\n");
    sb.append("    lastOffset: ").append(toIndentedString(lastOffset)).append("\n");
    sb.append("    currentActivity: ").append(toIndentedString(currentActivity)).append("\n");
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