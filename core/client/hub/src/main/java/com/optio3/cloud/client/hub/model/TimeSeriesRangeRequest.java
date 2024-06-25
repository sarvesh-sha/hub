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

import com.optio3.cloud.client.hub.model.TimeSeriesPropertyRequest;
import java.time.ZonedDateTime;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class TimeSeriesRangeRequest
{

  public TimeSeriesPropertyRequest spec = null;
  public ZonedDateTime rangeStart = null;
  public ZonedDateTime rangeEnd = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class TimeSeriesRangeRequest {\n");

    sb.append("    spec: ").append(toIndentedString(spec)).append("\n");
    sb.append("    rangeStart: ").append(toIndentedString(rangeStart)).append("\n");
    sb.append("    rangeEnd: ").append(toIndentedString(rangeEnd)).append("\n");
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
