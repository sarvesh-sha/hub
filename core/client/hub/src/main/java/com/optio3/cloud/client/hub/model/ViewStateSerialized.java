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

import com.optio3.cloud.client.hub.model.ViewStateItem;
import com.optio3.cloud.client.hub.model.ViewStateSerialized;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class ViewStateSerialized
{

  public Map<String, ViewStateItem> state = new HashMap<String, ViewStateItem>();
  public Map<String, ViewStateSerialized> subStates = new HashMap<String, ViewStateSerialized>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ViewStateSerialized {\n");

    sb.append("    state: ").append(toIndentedString(state)).append("\n");
    sb.append("    subStates: ").append(toIndentedString(subStates)).append("\n");
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
