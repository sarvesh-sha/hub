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
import com.optio3.cloud.client.hub.model.DeviceElementSampling;
import com.optio3.cloud.client.hub.model.RecordIdentity;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeName("DeviceElement")
public class DeviceElement extends Asset
{
  public static final String RECORD_IDENTITY = "DeviceElement";


  public String identifier = null;
  public JsonNode contents = null;
  public JsonNode desiredContents = null;
  public Boolean ableToUpdateState = null;
  public List<DeviceElementSampling> samplingSettings = new ArrayList<DeviceElementSampling>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class DeviceElement {\n");
    sb.append("    ").append(super.toString().replace("\n", "\n    ")).append("\n");
    sb.append("    identifier: ").append(toIndentedString(identifier)).append("\n");
    sb.append("    contents: ").append(toIndentedString(contents)).append("\n");
    sb.append("    desiredContents: ").append(toIndentedString(desiredContents)).append("\n");
    sb.append("    ableToUpdateState: ").append(toIndentedString(ableToUpdateState)).append("\n");
    sb.append("    samplingSettings: ").append(toIndentedString(samplingSettings)).append("\n");
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
