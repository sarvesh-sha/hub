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

import com.optio3.cloud.client.builder.model.DelayedOperation;
import com.optio3.cloud.client.builder.model.DeploymentAgent;
import com.optio3.cloud.client.builder.model.DeploymentHostDetails;
import com.optio3.cloud.client.builder.model.DeploymentHostProvisioningInfo;
import com.optio3.cloud.client.builder.model.DeploymentTask;
import com.optio3.cloud.client.builder.model.RecordIdentity;
import com.optio3.cloud.client.builder.model.RegistryImage;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class DeploymentHostStatusDescriptor
{









  public RecordIdentity ri = null;
  public String customerSysId = null;
  public String customerName = null;
  public String serviceSysId = null;
  public String serviceName = null;
  public CustomerVertical serviceVertical = null;
  public String hostId = null;
  public String hostName = null;
  public String remoteName = null;
  public List<DeploymentRole> roles = new ArrayList<DeploymentRole>();
  public String rolesSummary = null;
  public Map<String, DeploymentTask> tasks = new HashMap<String, DeploymentTask>();
  public Map<String, DeploymentAgent> agents = new HashMap<String, DeploymentAgent>();
  public Map<String, RegistryImage> images = new HashMap<String, RegistryImage>();
  public DeploymentHostDetails hostDetails = null;
  public DeploymentHostProvisioningInfo provisioningInfo = null;
  public ZonedDateTime createdOn = null;
  public ZonedDateTime lastHeartbeat = null;
  public ZonedDateTime agentBuildTime = null;
  public Float batteryVoltage = null;
  public Float cpuTemperature = null;
  public Long diskTotal = null;
  public Long diskFree = null;
  public DeploymentInstance instanceType = null;
  public DockerImageArchitecture architecture = null;
  public DeploymentStatus status = null;
  public DeploymentOperationalStatus operationalStatus = null;
  public DeploymentOperationalResponsiveness responsiveness = null;
  public List<DelayedOperation> delayedOps = new ArrayList<DelayedOperation>();
  public List<DeploymentHostStatusDescriptorFlag> flags = new ArrayList<DeploymentHostStatusDescriptorFlag>();
  public String preparedForCustomer = null;
  public String preparedForService = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class DeploymentHostStatusDescriptor {\n");

    sb.append("    ri: ").append(toIndentedString(ri)).append("\n");
    sb.append("    customerSysId: ").append(toIndentedString(customerSysId)).append("\n");
    sb.append("    customerName: ").append(toIndentedString(customerName)).append("\n");
    sb.append("    serviceSysId: ").append(toIndentedString(serviceSysId)).append("\n");
    sb.append("    serviceName: ").append(toIndentedString(serviceName)).append("\n");
    sb.append("    serviceVertical: ").append(toIndentedString(serviceVertical)).append("\n");
    sb.append("    hostId: ").append(toIndentedString(hostId)).append("\n");
    sb.append("    hostName: ").append(toIndentedString(hostName)).append("\n");
    sb.append("    remoteName: ").append(toIndentedString(remoteName)).append("\n");
    sb.append("    roles: ").append(toIndentedString(roles)).append("\n");
    sb.append("    rolesSummary: ").append(toIndentedString(rolesSummary)).append("\n");
    sb.append("    tasks: ").append(toIndentedString(tasks)).append("\n");
    sb.append("    agents: ").append(toIndentedString(agents)).append("\n");
    sb.append("    images: ").append(toIndentedString(images)).append("\n");
    sb.append("    hostDetails: ").append(toIndentedString(hostDetails)).append("\n");
    sb.append("    provisioningInfo: ").append(toIndentedString(provisioningInfo)).append("\n");
    sb.append("    createdOn: ").append(toIndentedString(createdOn)).append("\n");
    sb.append("    lastHeartbeat: ").append(toIndentedString(lastHeartbeat)).append("\n");
    sb.append("    agentBuildTime: ").append(toIndentedString(agentBuildTime)).append("\n");
    sb.append("    batteryVoltage: ").append(toIndentedString(batteryVoltage)).append("\n");
    sb.append("    cpuTemperature: ").append(toIndentedString(cpuTemperature)).append("\n");
    sb.append("    diskTotal: ").append(toIndentedString(diskTotal)).append("\n");
    sb.append("    diskFree: ").append(toIndentedString(diskFree)).append("\n");
    sb.append("    instanceType: ").append(toIndentedString(instanceType)).append("\n");
    sb.append("    architecture: ").append(toIndentedString(architecture)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    operationalStatus: ").append(toIndentedString(operationalStatus)).append("\n");
    sb.append("    responsiveness: ").append(toIndentedString(responsiveness)).append("\n");
    sb.append("    delayedOps: ").append(toIndentedString(delayedOps)).append("\n");
    sb.append("    flags: ").append(toIndentedString(flags)).append("\n");
    sb.append("    preparedForCustomer: ").append(toIndentedString(preparedForCustomer)).append("\n");
    sb.append("    preparedForService: ").append(toIndentedString(preparedForService)).append("\n");
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