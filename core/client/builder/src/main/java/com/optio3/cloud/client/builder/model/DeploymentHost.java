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

import com.optio3.cloud.client.builder.model.DeploymentHostDetails;
import com.optio3.cloud.client.builder.model.DeploymentHostOnlineSessions;
import com.optio3.cloud.client.builder.model.RecordIdentity;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class DeploymentHost
{
  public static final String RECORD_IDENTITY = "DeploymentHost";








  public String sysId = null;
  public ZonedDateTime createdOn = null;
  public ZonedDateTime updatedOn = null;
  public RecordIdentity customerService = null;
  public List<DeploymentRole> roles = new ArrayList<DeploymentRole>();
  public String hostId = null;
  public String hostName = null;
  public String displayName = null;
  public String remoteName = null;
  public String dnsName = null;
  public Integer warningThreshold = null;
  public DeploymentStatus status = null;
  public DeploymentOperationalResponsiveness operationalResponsiveness = null;
  public DeploymentOperationalStatus operationalStatus = null;
  public DockerImageArchitecture architecture = null;
  public DeploymentInstance instanceType = null;
  public String instanceRegion = null;
  public ZonedDateTime lastHeartbeat = null;
  public DeploymentHostOnlineSessions onlineSessions = null;
  public DeploymentHostDetails details = null;
  public Boolean delayedOperations = null;
  public ZonedDateTime lastOutput = null;
  public Integer lastOffset = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class DeploymentHost {\n");

    sb.append("    sysId: ").append(toIndentedString(sysId)).append("\n");
    sb.append("    createdOn: ").append(toIndentedString(createdOn)).append("\n");
    sb.append("    updatedOn: ").append(toIndentedString(updatedOn)).append("\n");
    sb.append("    customerService: ").append(toIndentedString(customerService)).append("\n");
    sb.append("    roles: ").append(toIndentedString(roles)).append("\n");
    sb.append("    hostId: ").append(toIndentedString(hostId)).append("\n");
    sb.append("    hostName: ").append(toIndentedString(hostName)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    remoteName: ").append(toIndentedString(remoteName)).append("\n");
    sb.append("    dnsName: ").append(toIndentedString(dnsName)).append("\n");
    sb.append("    warningThreshold: ").append(toIndentedString(warningThreshold)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    operationalResponsiveness: ").append(toIndentedString(operationalResponsiveness)).append("\n");
    sb.append("    operationalStatus: ").append(toIndentedString(operationalStatus)).append("\n");
    sb.append("    architecture: ").append(toIndentedString(architecture)).append("\n");
    sb.append("    instanceType: ").append(toIndentedString(instanceType)).append("\n");
    sb.append("    instanceRegion: ").append(toIndentedString(instanceRegion)).append("\n");
    sb.append("    lastHeartbeat: ").append(toIndentedString(lastHeartbeat)).append("\n");
    sb.append("    onlineSessions: ").append(toIndentedString(onlineSessions)).append("\n");
    sb.append("    details: ").append(toIndentedString(details)).append("\n");
    sb.append("    delayedOperations: ").append(toIndentedString(delayedOperations)).append("\n");
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