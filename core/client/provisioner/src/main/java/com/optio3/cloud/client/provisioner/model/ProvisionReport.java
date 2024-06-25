/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Provisioner APIs
 * APIs and Definitions for the Optio3 Provisioner product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.cloud.client.provisioner.model;

import com.optio3.cloud.client.provisioner.model.ProvisionTest;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class ProvisionReport
{


  public ZonedDateTime timestamp = null;
  public String manufacturingLocation = null;
  public String stationNumber = null;
  public String stationProgram = null;
  public String boardHardwareVersion = null;
  public String boardFirmwareVersion = null;
  public String boardSerialNumber = null;
  public String modemModule = null;
  public String modemRevision = null;
  public String firmwareVersion = null;
  public DockerImageArchitecture architecture = null;
  public String hostId = null;
  public String imsi = null;
  public String imei = null;
  public String iccid = null;
  public List<ProvisionTest> tests = new ArrayList<ProvisionTest>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProvisionReport {\n");

    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
    sb.append("    manufacturingLocation: ").append(toIndentedString(manufacturingLocation)).append("\n");
    sb.append("    stationNumber: ").append(toIndentedString(stationNumber)).append("\n");
    sb.append("    stationProgram: ").append(toIndentedString(stationProgram)).append("\n");
    sb.append("    boardHardwareVersion: ").append(toIndentedString(boardHardwareVersion)).append("\n");
    sb.append("    boardFirmwareVersion: ").append(toIndentedString(boardFirmwareVersion)).append("\n");
    sb.append("    boardSerialNumber: ").append(toIndentedString(boardSerialNumber)).append("\n");
    sb.append("    modemModule: ").append(toIndentedString(modemModule)).append("\n");
    sb.append("    modemRevision: ").append(toIndentedString(modemRevision)).append("\n");
    sb.append("    firmwareVersion: ").append(toIndentedString(firmwareVersion)).append("\n");
    sb.append("    architecture: ").append(toIndentedString(architecture)).append("\n");
    sb.append("    hostId: ").append(toIndentedString(hostId)).append("\n");
    sb.append("    imsi: ").append(toIndentedString(imsi)).append("\n");
    sb.append("    imei: ").append(toIndentedString(imei)).append("\n");
    sb.append("    iccid: ").append(toIndentedString(iccid)).append("\n");
    sb.append("    tests: ").append(toIndentedString(tests)).append("\n");
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