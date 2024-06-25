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

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes(
{
    @JsonSubTypes.Type(value = InstanceConfigurationDoNothing.class),
    @JsonSubTypes.Type(value = InstanceConfigurationForCRE.class),
    @JsonSubTypes.Type(value = InstanceConfigurationForDigineous.class),
    @JsonSubTypes.Type(value = InstanceConfigurationForDigitalMatter.class),
    @JsonSubTypes.Type(value = InstanceConfigurationForEPowerAmazon.class),
    @JsonSubTypes.Type(value = InstanceConfigurationForMerlinSolar.class),
    @JsonSubTypes.Type(value = InstanceConfigurationForMontageWalmart.class),
    @JsonSubTypes.Type(value = InstanceConfigurationForPalfinger.class),
    @JsonSubTypes.Type(value = InstanceConfigurationForStealthPower.class)
})
public class InstanceConfiguration
{


  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class InstanceConfiguration {\n");

    sb.append("}");
    return sb.toString();
  }

}
