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

import com.optio3.cloud.client.builder.model.MbControl;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeName("MbControlUpgradeToUDP")
public class MbControlUpgradeToUDP extends MbControl
{

  public Integer version = null;
  public Boolean isARM = null;
  public Boolean isIntel = null;
  public Integer registerWidth = null;
  public String hostId = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class MbControlUpgradeToUDP {\n");
    sb.append("    ").append(super.toString().replace("\n", "\n    ")).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    isARM: ").append(toIndentedString(isARM)).append("\n");
    sb.append("    isIntel: ").append(toIndentedString(isIntel)).append("\n");
    sb.append("    registerWidth: ").append(toIndentedString(registerWidth)).append("\n");
    sb.append("    hostId: ").append(toIndentedString(hostId)).append("\n");
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
