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
import java.time.ZonedDateTime;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes(
{
    @JsonSubTypes.Type(value = UserMessageAlert.class),
    @JsonSubTypes.Type(value = UserMessageDevice.class),
    @JsonSubTypes.Type(value = UserMessageGeneric.class),
    @JsonSubTypes.Type(value = UserMessageReport.class),
    @JsonSubTypes.Type(value = UserMessageRoleManagement.class),
    @JsonSubTypes.Type(value = UserMessageWorkflow.class)
})
public class UserMessage
{
  public static final String RECORD_IDENTITY = "UserMessage";


  public String sysId = null;
  public ZonedDateTime createdOn = null;
  public ZonedDateTime updatedOn = null;
  public RecordIdentity user = null;
  public String subject = null;
  public String body = null;
  public Boolean flagNew = null;
  public Boolean flagRead = null;
  public Boolean flagActive = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserMessage {\n");

    sb.append("    sysId: ").append(toIndentedString(sysId)).append("\n");
    sb.append("    createdOn: ").append(toIndentedString(createdOn)).append("\n");
    sb.append("    updatedOn: ").append(toIndentedString(updatedOn)).append("\n");
    sb.append("    user: ").append(toIndentedString(user)).append("\n");
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    flagNew: ").append(toIndentedString(flagNew)).append("\n");
    sb.append("    flagRead: ").append(toIndentedString(flagRead)).append("\n");
    sb.append("    flagActive: ").append(toIndentedString(flagActive)).append("\n");
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
