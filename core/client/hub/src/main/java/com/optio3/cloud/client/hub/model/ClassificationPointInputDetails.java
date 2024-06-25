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

import com.optio3.cloud.client.hub.model.TransportAddress;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class ClassificationPointInputDetails
{

  public String objectName = null;
  public String objectBackupName = null;
  public String objectBackupEquipmentName = null;
  public String objectWorkflowOverrideName = null;
  public String objectDescription = null;
  public String objectIdentifier = null;
  public String objectType = null;
  public String objectUnits = null;
  public String objectLocation = null;
  public List<String> objectBackupStructure = new ArrayList<String>();
  public String controllerName = null;
  public String controllerBackupName = null;
  public String controllerDescription = null;
  public String controllerIdentifier = null;
  public String controllerLocation = null;
  public String controllerModelName = null;
  public String controllerVendorName = null;
  public TransportAddress controllerTransportAddress = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClassificationPointInputDetails {\n");

    sb.append("    objectName: ").append(toIndentedString(objectName)).append("\n");
    sb.append("    objectBackupName: ").append(toIndentedString(objectBackupName)).append("\n");
    sb.append("    objectBackupEquipmentName: ").append(toIndentedString(objectBackupEquipmentName)).append("\n");
    sb.append("    objectWorkflowOverrideName: ").append(toIndentedString(objectWorkflowOverrideName)).append("\n");
    sb.append("    objectDescription: ").append(toIndentedString(objectDescription)).append("\n");
    sb.append("    objectIdentifier: ").append(toIndentedString(objectIdentifier)).append("\n");
    sb.append("    objectType: ").append(toIndentedString(objectType)).append("\n");
    sb.append("    objectUnits: ").append(toIndentedString(objectUnits)).append("\n");
    sb.append("    objectLocation: ").append(toIndentedString(objectLocation)).append("\n");
    sb.append("    objectBackupStructure: ").append(toIndentedString(objectBackupStructure)).append("\n");
    sb.append("    controllerName: ").append(toIndentedString(controllerName)).append("\n");
    sb.append("    controllerBackupName: ").append(toIndentedString(controllerBackupName)).append("\n");
    sb.append("    controllerDescription: ").append(toIndentedString(controllerDescription)).append("\n");
    sb.append("    controllerIdentifier: ").append(toIndentedString(controllerIdentifier)).append("\n");
    sb.append("    controllerLocation: ").append(toIndentedString(controllerLocation)).append("\n");
    sb.append("    controllerModelName: ").append(toIndentedString(controllerModelName)).append("\n");
    sb.append("    controllerVendorName: ").append(toIndentedString(controllerVendorName)).append("\n");
    sb.append("    controllerTransportAddress: ").append(toIndentedString(controllerTransportAddress)).append("\n");
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
