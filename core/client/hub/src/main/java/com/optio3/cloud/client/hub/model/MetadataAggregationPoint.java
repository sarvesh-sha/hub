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
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class MetadataAggregationPoint
{

  public String pointId = null;
  public String pointName = null;
  public String pointNameRaw = null;
  public String pointNameBackup = null;
  public String identifier = null;
  public String pointClassId = null;
  public String buildingId = null;
  public String locationSysId = null;
  public String equipmentId = null;
  public List<String> tags = new ArrayList<String>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class MetadataAggregationPoint {\n");

    sb.append("    pointId: ").append(toIndentedString(pointId)).append("\n");
    sb.append("    pointName: ").append(toIndentedString(pointName)).append("\n");
    sb.append("    pointNameRaw: ").append(toIndentedString(pointNameRaw)).append("\n");
    sb.append("    pointNameBackup: ").append(toIndentedString(pointNameBackup)).append("\n");
    sb.append("    identifier: ").append(toIndentedString(identifier)).append("\n");
    sb.append("    pointClassId: ").append(toIndentedString(pointClassId)).append("\n");
    sb.append("    buildingId: ").append(toIndentedString(buildingId)).append("\n");
    sb.append("    locationSysId: ").append(toIndentedString(locationSysId)).append("\n");
    sb.append("    equipmentId: ").append(toIndentedString(equipmentId)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
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