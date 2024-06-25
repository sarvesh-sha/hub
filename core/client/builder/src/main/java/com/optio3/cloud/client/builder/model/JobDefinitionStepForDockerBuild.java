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

import com.optio3.cloud.client.builder.model.JobDefinitionStep;
import com.optio3.cloud.client.builder.model.RecordIdentity;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeName("JobDefinitionStepForDockerBuild")
public class JobDefinitionStepForDockerBuild extends JobDefinitionStep
{
  public static final String RECORD_IDENTITY = "JobDefinitionStepForDockerBuild";



  public String sourcePath = null;
  public String dockerFile = null;
  public DeploymentRole targetService = null;
  public Map<String, String> buildArgs = new HashMap<String, String>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class JobDefinitionStepForDockerBuild {\n");
    sb.append("    ").append(super.toString().replace("\n", "\n    ")).append("\n");
    sb.append("    sourcePath: ").append(toIndentedString(sourcePath)).append("\n");
    sb.append("    dockerFile: ").append(toIndentedString(dockerFile)).append("\n");
    sb.append("    targetService: ").append(toIndentedString(targetService)).append("\n");
    sb.append("    buildArgs: ").append(toIndentedString(buildArgs)).append("\n");
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