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

import com.optio3.cloud.client.hub.model.EngineExecutionAssignment;
import com.optio3.cloud.client.hub.model.EquipmentClassAssignment;
import com.optio3.cloud.client.hub.model.NormalizationEngineExecutionStep;
import com.optio3.cloud.client.hub.model.NormalizationEngineValueEquipment;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeName("NormalizationEngineExecutionStepEquipmentClassification")
public class NormalizationEngineExecutionStepEquipmentClassification extends NormalizationEngineExecutionStep
{

  public NormalizationEngineValueEquipment equipment = null;
  public EquipmentClassAssignment classificationAssignment = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class NormalizationEngineExecutionStepEquipmentClassification {\n");
    sb.append("    ").append(super.toString().replace("\n", "\n    ")).append("\n");
    sb.append("    equipment: ").append(toIndentedString(equipment)).append("\n");
    sb.append("    classificationAssignment: ").append(toIndentedString(classificationAssignment)).append("\n");
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
