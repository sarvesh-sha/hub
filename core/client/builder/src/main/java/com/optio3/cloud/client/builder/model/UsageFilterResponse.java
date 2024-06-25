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

import com.optio3.cloud.client.builder.model.RecordIdentity;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class UsageFilterResponse
{

  public Integer userPreferenceHits = null;
  public List<RecordIdentity> userPreferenceItems = new ArrayList<RecordIdentity>();
  public Integer systemPreferenceHits = null;
  public List<RecordIdentity> systemPreferenceItems = new ArrayList<RecordIdentity>();
  public Integer dashboardHits = null;
  public List<RecordIdentity> dashboardItems = new ArrayList<RecordIdentity>();
  public Integer alertDefinitionVersionHits = null;
  public List<RecordIdentity> alertDefinitionVersionItems = new ArrayList<RecordIdentity>();
  public Integer metricsDefinitionVersionHits = null;
  public List<RecordIdentity> metricsDefinitionVersionItems = new ArrayList<RecordIdentity>();
  public Integer normalizationVersionHits = null;
  public List<RecordIdentity> normalizationVersionItems = new ArrayList<RecordIdentity>();
  public Integer reportDefinitionVersionHits = null;
  public List<RecordIdentity> reportDefinitionVersionItems = new ArrayList<RecordIdentity>();
  public Integer workflowHits = null;
  public List<RecordIdentity> workflowItems = new ArrayList<RecordIdentity>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class UsageFilterResponse {\n");

    sb.append("    userPreferenceHits: ").append(toIndentedString(userPreferenceHits)).append("\n");
    sb.append("    userPreferenceItems: ").append(toIndentedString(userPreferenceItems)).append("\n");
    sb.append("    systemPreferenceHits: ").append(toIndentedString(systemPreferenceHits)).append("\n");
    sb.append("    systemPreferenceItems: ").append(toIndentedString(systemPreferenceItems)).append("\n");
    sb.append("    dashboardHits: ").append(toIndentedString(dashboardHits)).append("\n");
    sb.append("    dashboardItems: ").append(toIndentedString(dashboardItems)).append("\n");
    sb.append("    alertDefinitionVersionHits: ").append(toIndentedString(alertDefinitionVersionHits)).append("\n");
    sb.append("    alertDefinitionVersionItems: ").append(toIndentedString(alertDefinitionVersionItems)).append("\n");
    sb.append("    metricsDefinitionVersionHits: ").append(toIndentedString(metricsDefinitionVersionHits)).append("\n");
    sb.append("    metricsDefinitionVersionItems: ").append(toIndentedString(metricsDefinitionVersionItems)).append("\n");
    sb.append("    normalizationVersionHits: ").append(toIndentedString(normalizationVersionHits)).append("\n");
    sb.append("    normalizationVersionItems: ").append(toIndentedString(normalizationVersionItems)).append("\n");
    sb.append("    reportDefinitionVersionHits: ").append(toIndentedString(reportDefinitionVersionHits)).append("\n");
    sb.append("    reportDefinitionVersionItems: ").append(toIndentedString(reportDefinitionVersionItems)).append("\n");
    sb.append("    workflowHits: ").append(toIndentedString(workflowHits)).append("\n");
    sb.append("    workflowItems: ").append(toIndentedString(workflowItems)).append("\n");
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
