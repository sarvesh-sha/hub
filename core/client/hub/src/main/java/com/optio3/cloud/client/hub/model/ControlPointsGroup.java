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

import com.optio3.cloud.client.hub.model.AssetGraph;
import com.optio3.cloud.client.hub.model.AssetGraphBinding;
import com.optio3.cloud.client.hub.model.ColorConfiguration;
import com.optio3.cloud.client.hub.model.ControlPointsSelection;
import com.optio3.cloud.client.hub.model.EngineeringUnitsFactors;
import com.optio3.cloud.client.hub.model.ToggleableNumericRange;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class ControlPointsGroup
{





  public String name = null;
  public EngineeringUnitsFactors unitsFactors = null;
  public String unitsDisplay = null;
  public AggregationTypeId aggregationType = null;
  public AggregationTypeId groupAggregationType = null;
  public AggregationGranularity granularity = null;
  public AggregationLimit limitMode = null;
  public Integer limitValue = null;
  public Integer valuePrecision = null;
  public ControlPointsSelection selections = null;
  public ColorConfiguration colorConfig = null;
  public ToggleableNumericRange range = null;
  public AssetGraph graph = null;
  public AssetGraphBinding pointInput = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ControlPointsGroup {\n");

    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    unitsFactors: ").append(toIndentedString(unitsFactors)).append("\n");
    sb.append("    unitsDisplay: ").append(toIndentedString(unitsDisplay)).append("\n");
    sb.append("    aggregationType: ").append(toIndentedString(aggregationType)).append("\n");
    sb.append("    groupAggregationType: ").append(toIndentedString(groupAggregationType)).append("\n");
    sb.append("    granularity: ").append(toIndentedString(granularity)).append("\n");
    sb.append("    limitMode: ").append(toIndentedString(limitMode)).append("\n");
    sb.append("    limitValue: ").append(toIndentedString(limitValue)).append("\n");
    sb.append("    valuePrecision: ").append(toIndentedString(valuePrecision)).append("\n");
    sb.append("    selections: ").append(toIndentedString(selections)).append("\n");
    sb.append("    colorConfig: ").append(toIndentedString(colorConfig)).append("\n");
    sb.append("    range: ").append(toIndentedString(range)).append("\n");
    sb.append("    graph: ").append(toIndentedString(graph)).append("\n");
    sb.append("    pointInput: ").append(toIndentedString(pointInput)).append("\n");
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