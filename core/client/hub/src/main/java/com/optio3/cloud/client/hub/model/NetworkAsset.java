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

import com.optio3.cloud.client.hub.model.Asset;
import com.optio3.cloud.client.hub.model.BaseAssetDescriptor;
import com.optio3.cloud.client.hub.model.DiscoveryState;
import com.optio3.cloud.client.hub.model.ProtocolConfig;
import com.optio3.cloud.client.hub.model.RecordIdentity;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeName("NetworkAsset")
public class NetworkAsset extends Asset
{
  public static final String RECORD_IDENTITY = "NetworkAsset";


  public String cidr = null;
  public String staticAddress = null;
  public String networkInterface = null;
  public Integer samplingPeriod = null;
  public List<ProtocolConfig> protocolsConfiguration = new ArrayList<ProtocolConfig>();
  public DiscoveryState discoveryState = null;
  public ZonedDateTime lastOutput = null;
  public Integer lastOffset = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class NetworkAsset {\n");
    sb.append("    ").append(super.toString().replace("\n", "\n    ")).append("\n");
    sb.append("    cidr: ").append(toIndentedString(cidr)).append("\n");
    sb.append("    staticAddress: ").append(toIndentedString(staticAddress)).append("\n");
    sb.append("    networkInterface: ").append(toIndentedString(networkInterface)).append("\n");
    sb.append("    samplingPeriod: ").append(toIndentedString(samplingPeriod)).append("\n");
    sb.append("    protocolsConfiguration: ").append(toIndentedString(protocolsConfiguration)).append("\n");
    sb.append("    discoveryState: ").append(toIndentedString(discoveryState)).append("\n");
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
