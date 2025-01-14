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

import com.optio3.cloud.client.hub.model.Asset;
import com.optio3.cloud.client.hub.model.BaseAssetDescriptor;
import com.optio3.cloud.client.hub.model.RecordIdentity;
import java.time.ZonedDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeName("Device")
public class Device extends Asset
{
  public static final String RECORD_IDENTITY = "Device";


  public String manufacturerName = null;
  public String productName = null;
  public String modelName = null;
  public String firmwareVersion = null;
  public Integer minutesBeforeTransitionToUnreachable = null;
  public Integer minutesBeforeTransitionToReachable = null;
  public Boolean reachable = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Device {\n");
    sb.append("    ").append(super.toString().replace("\n", "\n    ")).append("\n");
    sb.append("    manufacturerName: ").append(toIndentedString(manufacturerName)).append("\n");
    sb.append("    productName: ").append(toIndentedString(productName)).append("\n");
    sb.append("    modelName: ").append(toIndentedString(modelName)).append("\n");
    sb.append("    firmwareVersion: ").append(toIndentedString(firmwareVersion)).append("\n");
    sb.append("    minutesBeforeTransitionToUnreachable: ").append(toIndentedString(minutesBeforeTransitionToUnreachable)).append("\n");
    sb.append("    minutesBeforeTransitionToReachable: ").append(toIndentedString(minutesBeforeTransitionToReachable)).append("\n");
    sb.append("    reachable: ").append(toIndentedString(reachable)).append("\n");
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
