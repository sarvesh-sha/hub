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
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes(
{
    @JsonSubTypes.Type(value = AlertEngineExecutionStep.class),
    @JsonSubTypes.Type(value = AlertEngineExecutionStepCommitAction.class),
    @JsonSubTypes.Type(value = AlertEngineExecutionStepCreateAlert.class),
    @JsonSubTypes.Type(value = AlertEngineExecutionStepSetAlertDescription.class),
    @JsonSubTypes.Type(value = AlertEngineExecutionStepSetAlertSeverity.class),
    @JsonSubTypes.Type(value = AlertEngineExecutionStepSetAlertStatus.class),
    @JsonSubTypes.Type(value = AlertEngineExecutionStepSetAlertText.class),
    @JsonSubTypes.Type(value = AlertEngineExecutionStepSetControlPointValue.class),
    @JsonSubTypes.Type(value = MetricsEngineExecutionStep.class),
    @JsonSubTypes.Type(value = NormalizationEngineExecutionStep.class),
    @JsonSubTypes.Type(value = NormalizationEngineExecutionStepEquipmentClassification.class),
    @JsonSubTypes.Type(value = NormalizationEngineExecutionStepPointClassification.class),
    @JsonSubTypes.Type(value = NormalizationEngineExecutionStepPushEquipment.class),
    @JsonSubTypes.Type(value = NormalizationEngineExecutionStepUnits.class)
})
@JsonTypeName("EngineExecutionStep")
public class EngineExecutionStep
{

  public String enteringBlockId = null;
  public String leavingBlockId = null;
  public EngineExecutionAssignment assignment = null;
  public String notImplemented = null;
  public String failure = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class EngineExecutionStep {\n");

    sb.append("    enteringBlockId: ").append(toIndentedString(enteringBlockId)).append("\n");
    sb.append("    leavingBlockId: ").append(toIndentedString(leavingBlockId)).append("\n");
    sb.append("    assignment: ").append(toIndentedString(assignment)).append("\n");
    sb.append("    notImplemented: ").append(toIndentedString(notImplemented)).append("\n");
    sb.append("    failure: ").append(toIndentedString(failure)).append("\n");
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