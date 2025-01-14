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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class WorkflowOverrides
{

  public List<String> workflowIds = new ArrayList<String>();
  public Map<String, String> deviceNames = new HashMap<String, String>();
  public Map<String, String> deviceLocations = new HashMap<String, String>();
  public Map<String, String> pointNames = new HashMap<String, String>();
  public Map<String, String> pointClasses = new HashMap<String, String>();
  public Map<String, String> pointParents = new HashMap<String, String>();
  public Map<String, Integer> pointSamplingPeriods = new HashMap<String, Integer>();
  public Map<String, Boolean> pointSampling = new HashMap<String, Boolean>();
  public Map<String, String> equipmentNames = new HashMap<String, String>();
  public Map<String, String> equipmentClasses = new HashMap<String, String>();
  public Map<String, String> equipmentParents = new HashMap<String, String>();
  public Map<String, String> equipmentLocations = new HashMap<String, String>();
  public Map<String, String> equipmentMerge = new HashMap<String, String>();
  public List<String> removedEquipment = new ArrayList<String>();
  public List<String> createdEquipment = new ArrayList<String>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowOverrides {\n");

    sb.append("    workflowIds: ").append(toIndentedString(workflowIds)).append("\n");
    sb.append("    deviceNames: ").append(toIndentedString(deviceNames)).append("\n");
    sb.append("    deviceLocations: ").append(toIndentedString(deviceLocations)).append("\n");
    sb.append("    pointNames: ").append(toIndentedString(pointNames)).append("\n");
    sb.append("    pointClasses: ").append(toIndentedString(pointClasses)).append("\n");
    sb.append("    pointParents: ").append(toIndentedString(pointParents)).append("\n");
    sb.append("    pointSamplingPeriods: ").append(toIndentedString(pointSamplingPeriods)).append("\n");
    sb.append("    pointSampling: ").append(toIndentedString(pointSampling)).append("\n");
    sb.append("    equipmentNames: ").append(toIndentedString(equipmentNames)).append("\n");
    sb.append("    equipmentClasses: ").append(toIndentedString(equipmentClasses)).append("\n");
    sb.append("    equipmentParents: ").append(toIndentedString(equipmentParents)).append("\n");
    sb.append("    equipmentLocations: ").append(toIndentedString(equipmentLocations)).append("\n");
    sb.append("    equipmentMerge: ").append(toIndentedString(equipmentMerge)).append("\n");
    sb.append("    removedEquipment: ").append(toIndentedString(removedEquipment)).append("\n");
    sb.append("    createdEquipment: ").append(toIndentedString(createdEquipment)).append("\n");
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
