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

import com.optio3.cloud.client.hub.model.EventFilterRequest;
import com.optio3.cloud.client.hub.model.SortCriteria;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeName("WorkflowFilterRequest")
public class WorkflowFilterRequest extends EventFilterRequest
{




  public String likeFilter = null;
  public List<WorkflowStatus> workflowStatusIDs = new ArrayList<WorkflowStatus>();
  public List<WorkflowType> workflowTypeIDs = new ArrayList<WorkflowType>();
  public List<WorkflowPriority> workflowPriorityIDs = new ArrayList<WorkflowPriority>();
  public List<String> createdByIDs = new ArrayList<String>();
  public List<String> assignedToIDs = new ArrayList<String>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowFilterRequest {\n");
    sb.append("    ").append(super.toString().replace("\n", "\n    ")).append("\n");
    sb.append("    likeFilter: ").append(toIndentedString(likeFilter)).append("\n");
    sb.append("    workflowStatusIDs: ").append(toIndentedString(workflowStatusIDs)).append("\n");
    sb.append("    workflowTypeIDs: ").append(toIndentedString(workflowTypeIDs)).append("\n");
    sb.append("    workflowPriorityIDs: ").append(toIndentedString(workflowPriorityIDs)).append("\n");
    sb.append("    createdByIDs: ").append(toIndentedString(createdByIDs)).append("\n");
    sb.append("    assignedToIDs: ").append(toIndentedString(assignedToIDs)).append("\n");
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
