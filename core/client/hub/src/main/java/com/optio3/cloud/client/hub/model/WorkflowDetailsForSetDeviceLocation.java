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

import com.optio3.cloud.client.hub.model.WorkflowAsset;
import com.optio3.cloud.client.hub.model.WorkflowDetails;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeName("WorkflowDetailsForSetDeviceLocation")
public class WorkflowDetailsForSetDeviceLocation extends WorkflowDetails
{

  public List<WorkflowAsset> devices = new ArrayList<WorkflowAsset>();
  public String locationSysId = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowDetailsForSetDeviceLocation {\n");
    sb.append("    ").append(super.toString().replace("\n", "\n    ")).append("\n");
    sb.append("    devices: ").append(toIndentedString(devices)).append("\n");
    sb.append("    locationSysId: ").append(toIndentedString(locationSysId)).append("\n");
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
