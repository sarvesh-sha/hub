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
import com.optio3.cloud.client.hub.model.GeoFence;
import com.optio3.cloud.client.hub.model.LongitudeLatitude;
import com.optio3.cloud.client.hub.model.RecordIdentity;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeName("Location")
public class Location extends Asset
{
  public static final String RECORD_IDENTITY = "Location";



  public LocationType type = null;
  public String phone = null;
  public String address = null;
  public String timeZone = null;
  public LongitudeLatitude geo = null;
  public List<GeoFence> fences = new ArrayList<GeoFence>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Location {\n");
    sb.append("    ").append(super.toString().replace("\n", "\n    ")).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
    sb.append("    address: ").append(toIndentedString(address)).append("\n");
    sb.append("    timeZone: ").append(toIndentedString(timeZone)).append("\n");
    sb.append("    geo: ").append(toIndentedString(geo)).append("\n");
    sb.append("    fences: ").append(toIndentedString(fences)).append("\n");
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
