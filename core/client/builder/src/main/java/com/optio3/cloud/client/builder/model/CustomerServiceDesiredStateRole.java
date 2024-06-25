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
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class CustomerServiceDesiredStateRole
{



  public DeploymentRole role = null;
  public DockerImageArchitecture architecture = null;
  public RecordIdentity image = null;
  public Boolean shutdown = null;
  public Boolean shutdownIfDifferent = null;
  public Boolean launch = null;
  public Boolean launchIfMissing = null;
  public Boolean launchIfMissingAndIdle = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class CustomerServiceDesiredStateRole {\n");

    sb.append("    role: ").append(toIndentedString(role)).append("\n");
    sb.append("    architecture: ").append(toIndentedString(architecture)).append("\n");
    sb.append("    image: ").append(toIndentedString(image)).append("\n");
    sb.append("    shutdown: ").append(toIndentedString(shutdown)).append("\n");
    sb.append("    shutdownIfDifferent: ").append(toIndentedString(shutdownIfDifferent)).append("\n");
    sb.append("    launch: ").append(toIndentedString(launch)).append("\n");
    sb.append("    launchIfMissing: ").append(toIndentedString(launchIfMissing)).append("\n");
    sb.append("    launchIfMissingAndIdle: ").append(toIndentedString(launchIfMissingAndIdle)).append("\n");
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