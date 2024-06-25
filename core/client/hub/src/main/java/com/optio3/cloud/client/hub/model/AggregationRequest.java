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

import com.optio3.cloud.client.hub.model.ControlPointsSelection;
import com.optio3.cloud.client.hub.model.EngineeringUnitsFactors;
import com.optio3.cloud.client.hub.model.FilterableTimeRange;
import com.optio3.cloud.client.hub.model.TagsJoinQuery;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class AggregationRequest
{


  public EngineeringUnitsFactors unitsFactors = null;
  public AggregationTypeId aggregationType = null;
  public ControlPointsSelection selections = null;
  public TagsJoinQuery query = null;
  public List<FilterableTimeRange> filterableRanges = new ArrayList<FilterableTimeRange>();
  public Integer localTimeZoneOffset = null;
  public String prop = null;
  public Double maxInterpolationGap = null;

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class AggregationRequest {\n");

    sb.append("    unitsFactors: ").append(toIndentedString(unitsFactors)).append("\n");
    sb.append("    aggregationType: ").append(toIndentedString(aggregationType)).append("\n");
    sb.append("    selections: ").append(toIndentedString(selections)).append("\n");
    sb.append("    query: ").append(toIndentedString(query)).append("\n");
    sb.append("    filterableRanges: ").append(toIndentedString(filterableRanges)).append("\n");
    sb.append("    localTimeZoneOffset: ").append(toIndentedString(localTimeZoneOffset)).append("\n");
    sb.append("    prop: ").append(toIndentedString(prop)).append("\n");
    sb.append("    maxInterpolationGap: ").append(toIndentedString(maxInterpolationGap)).append("\n");
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