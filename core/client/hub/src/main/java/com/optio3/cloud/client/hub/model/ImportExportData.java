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

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes(
{
    @JsonSubTypes.Type(value = BACnetBulkRenamingData.class),
    @JsonSubTypes.Type(value = BACnetImportExportData.class)
})
public class ImportExportData
{


  public String sysId = null;
  public String deviceName = null;
  public String deviceDescription = null;
  public String deviceLocation = null;
  public List<String> deviceStructure = new ArrayList<String>();
  public String deviceVendor = null;
  public String deviceModel = null;
  public String dashboardName = null;
  public String dashboardEquipmentName = null;
  public List<String> dashboardStructure = new ArrayList<String>();
  public String normalizedName = null;
  public Boolean isSampled = null;
  public String pointClassId = null;
  public String pointClassAdt = null;
  public List<String> pointTags = new ArrayList<String>();
  public String locationName = null;
  public String locationSysId = null;
  public EngineeringUnits units = null;
  public String value = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ImportExportData {\n");

    sb.append("    sysId: ").append(toIndentedString(sysId)).append("\n");
    sb.append("    deviceName: ").append(toIndentedString(deviceName)).append("\n");
    sb.append("    deviceDescription: ").append(toIndentedString(deviceDescription)).append("\n");
    sb.append("    deviceLocation: ").append(toIndentedString(deviceLocation)).append("\n");
    sb.append("    deviceStructure: ").append(toIndentedString(deviceStructure)).append("\n");
    sb.append("    deviceVendor: ").append(toIndentedString(deviceVendor)).append("\n");
    sb.append("    deviceModel: ").append(toIndentedString(deviceModel)).append("\n");
    sb.append("    dashboardName: ").append(toIndentedString(dashboardName)).append("\n");
    sb.append("    dashboardEquipmentName: ").append(toIndentedString(dashboardEquipmentName)).append("\n");
    sb.append("    dashboardStructure: ").append(toIndentedString(dashboardStructure)).append("\n");
    sb.append("    normalizedName: ").append(toIndentedString(normalizedName)).append("\n");
    sb.append("    isSampled: ").append(toIndentedString(isSampled)).append("\n");
    sb.append("    pointClassId: ").append(toIndentedString(pointClassId)).append("\n");
    sb.append("    pointClassAdt: ").append(toIndentedString(pointClassAdt)).append("\n");
    sb.append("    pointTags: ").append(toIndentedString(pointTags)).append("\n");
    sb.append("    locationName: ").append(toIndentedString(locationName)).append("\n");
    sb.append("    locationSysId: ").append(toIndentedString(locationSysId)).append("\n");
    sb.append("    units: ").append(toIndentedString(units)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
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