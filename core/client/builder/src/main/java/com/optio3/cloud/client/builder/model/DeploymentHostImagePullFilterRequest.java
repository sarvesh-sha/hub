/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Builder APIs
 * APIs and Definitions for the Optio3 Builder product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.cloud.client.builder.model;

import com.optio3.cloud.client.builder.model.SortCriteria;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class DeploymentHostImagePullFilterRequest
{


  public String hostSysId = null;
  public ZonedDateTime olderThan = null;
  public ZonedDateTime newerThan = null;
  public JobStatus statusFilter = null;
  public List<SortCriteria> sortBy = new ArrayList<SortCriteria>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class DeploymentHostImagePullFilterRequest {\n");

    sb.append("    hostSysId: ").append(toIndentedString(hostSysId)).append("\n");
    sb.append("    olderThan: ").append(toIndentedString(olderThan)).append("\n");
    sb.append("    newerThan: ").append(toIndentedString(newerThan)).append("\n");
    sb.append("    statusFilter: ").append(toIndentedString(statusFilter)).append("\n");
    sb.append("    sortBy: ").append(toIndentedString(sortBy)).append("\n");
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
