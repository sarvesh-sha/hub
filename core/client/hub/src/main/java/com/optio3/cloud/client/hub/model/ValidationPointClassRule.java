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

public class ValidationPointClassRule
{

  public String pointClassId = null;
  public Boolean isBinary = null;
  public Double minValue = null;
  public Double maxValue = null;
  public List<String> allowableObjectTypes = new ArrayList<String>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ValidationPointClassRule {\n");

    sb.append("    pointClassId: ").append(toIndentedString(pointClassId)).append("\n");
    sb.append("    isBinary: ").append(toIndentedString(isBinary)).append("\n");
    sb.append("    minValue: ").append(toIndentedString(minValue)).append("\n");
    sb.append("    maxValue: ").append(toIndentedString(maxValue)).append("\n");
    sb.append("    allowableObjectTypes: ").append(toIndentedString(allowableObjectTypes)).append("\n");
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