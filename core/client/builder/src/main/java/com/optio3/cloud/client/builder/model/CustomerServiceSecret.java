/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Builder APIs
 * APIs and Definitions for the Optio3 Builder product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.cloud.client.builder.model;

import com.optio3.cloud.client.builder.model.RecordIdentity;
import java.time.ZonedDateTime;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class CustomerServiceSecret
{
  public static final String RECORD_IDENTITY = "CustomerServiceSecret";


  public String sysId = null;
  public ZonedDateTime createdOn = null;
  public ZonedDateTime updatedOn = null;
  public RecordIdentity service = null;
  public String context = null;
  public String key = null;
  public String value = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class CustomerServiceSecret {\n");

    sb.append("    sysId: ").append(toIndentedString(sysId)).append("\n");
    sb.append("    createdOn: ").append(toIndentedString(createdOn)).append("\n");
    sb.append("    updatedOn: ").append(toIndentedString(updatedOn)).append("\n");
    sb.append("    service: ").append(toIndentedString(service)).append("\n");
    sb.append("    context: ").append(toIndentedString(context)).append("\n");
    sb.append("    key: ").append(toIndentedString(key)).append("\n");
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
