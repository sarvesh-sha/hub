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

public class DeploymentHostImagePull
{
  public static final String RECORD_IDENTITY = "DeploymentHostImagePull";



  public String sysId = null;
  public ZonedDateTime createdOn = null;
  public ZonedDateTime updatedOn = null;
  public RecordIdentity deployment = null;
  public String image = null;
  public RecordIdentity imageReference = null;
  public JobStatus status = null;
  public ZonedDateTime lastOutput = null;
  public Integer lastOffset = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class DeploymentHostImagePull {\n");

    sb.append("    sysId: ").append(toIndentedString(sysId)).append("\n");
    sb.append("    createdOn: ").append(toIndentedString(createdOn)).append("\n");
    sb.append("    updatedOn: ").append(toIndentedString(updatedOn)).append("\n");
    sb.append("    deployment: ").append(toIndentedString(deployment)).append("\n");
    sb.append("    image: ").append(toIndentedString(image)).append("\n");
    sb.append("    imageReference: ").append(toIndentedString(imageReference)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    lastOutput: ").append(toIndentedString(lastOutput)).append("\n");
    sb.append("    lastOffset: ").append(toIndentedString(lastOffset)).append("\n");
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