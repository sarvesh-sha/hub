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

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class DataImportProgress
{


  public BackgroundActivityStatus status = null;
  public Integer devicesToProcess = null;
  public Integer devicesProcessed = null;
  public Integer elementsProcessed = null;
  public Integer elementsModified = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class DataImportProgress {\n");

    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    devicesToProcess: ").append(toIndentedString(devicesToProcess)).append("\n");
    sb.append("    devicesProcessed: ").append(toIndentedString(devicesProcessed)).append("\n");
    sb.append("    elementsProcessed: ").append(toIndentedString(elementsProcessed)).append("\n");
    sb.append("    elementsModified: ").append(toIndentedString(elementsModified)).append("\n");
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
