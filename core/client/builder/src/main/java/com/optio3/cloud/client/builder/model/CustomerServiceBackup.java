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
import com.optio3.cloud.client.builder.model.RoleAndArchitectureWithImage;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class CustomerServiceBackup
{
  public static final String RECORD_IDENTITY = "CustomerServiceBackup";



  public String sysId = null;
  public ZonedDateTime createdOn = null;
  public ZonedDateTime updatedOn = null;
  public RecordIdentity customerService = null;
  public String fileId = null;
  public Long fileSize = null;
  public String fileIdOnAgent = null;
  public Boolean pendingTransfer = null;
  public BackupKind trigger = null;
  public String extraConfigLines = null;
  public List<RoleAndArchitectureWithImage> roleImages = new ArrayList<RoleAndArchitectureWithImage>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class CustomerServiceBackup {\n");

    sb.append("    sysId: ").append(toIndentedString(sysId)).append("\n");
    sb.append("    createdOn: ").append(toIndentedString(createdOn)).append("\n");
    sb.append("    updatedOn: ").append(toIndentedString(updatedOn)).append("\n");
    sb.append("    customerService: ").append(toIndentedString(customerService)).append("\n");
    sb.append("    fileId: ").append(toIndentedString(fileId)).append("\n");
    sb.append("    fileSize: ").append(toIndentedString(fileSize)).append("\n");
    sb.append("    fileIdOnAgent: ").append(toIndentedString(fileIdOnAgent)).append("\n");
    sb.append("    pendingTransfer: ").append(toIndentedString(pendingTransfer)).append("\n");
    sb.append("    trigger: ").append(toIndentedString(trigger)).append("\n");
    sb.append("    extraConfigLines: ").append(toIndentedString(extraConfigLines)).append("\n");
    sb.append("    roleImages: ").append(toIndentedString(roleImages)).append("\n");
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
