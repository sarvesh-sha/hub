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

import com.optio3.cloud.client.hub.model.RecordIdentity;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class AssetFilterResponse
{

  public Integer version = null;
  public Integer offset = null;
  public Integer nextOffset = null;
  public List<RecordIdentity> results = new ArrayList<RecordIdentity>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class AssetFilterResponse {\n");

    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    offset: ").append(toIndentedString(offset)).append("\n");
    sb.append("    nextOffset: ").append(toIndentedString(nextOffset)).append("\n");
    sb.append("    results: ").append(toIndentedString(results)).append("\n");
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
