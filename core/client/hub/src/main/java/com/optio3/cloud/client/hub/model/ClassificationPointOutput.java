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

import com.optio3.cloud.client.hub.model.ClassificationPointInputDetails;
import com.optio3.cloud.client.hub.model.ClassificationPointOutputDetails;
import com.optio3.cloud.client.hub.model.NormalizationEquipment;
import com.optio3.cloud.client.hub.model.NormalizationEquipmentLocation;
import com.optio3.cloud.client.hub.model.NormalizationMatchHistory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class ClassificationPointOutput
{

  public String sysId = null;
  public String parentSysId = null;
  public String networkSysId = null;
  public String pointClassOverride = null;
  public List<NormalizationEquipment> equipmentOverrides = new ArrayList<NormalizationEquipment>();
  public ClassificationPointInputDetails details = null;
  public String normalizedName = null;
  public String oldNormalizedName = null;
  public List<NormalizationMatchHistory> normalizationHistory = new ArrayList<NormalizationMatchHistory>();
  public ClassificationPointOutputDetails lastResult = null;
  public ClassificationPointOutputDetails currentResult = null;
  public Map<String, NormalizationEquipment> equipments = new HashMap<String, NormalizationEquipment>();
  public Map<String, List<String>> equipmentRelationships = new HashMap<String, List<String>>();
  public List<NormalizationEquipmentLocation> locations = new ArrayList<NormalizationEquipmentLocation>();
  public List<String> matchingDimensions = new ArrayList<String>();
  public List<String> normalizationTags = new ArrayList<String>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClassificationPointOutput {\n");

    sb.append("    sysId: ").append(toIndentedString(sysId)).append("\n");
    sb.append("    parentSysId: ").append(toIndentedString(parentSysId)).append("\n");
    sb.append("    networkSysId: ").append(toIndentedString(networkSysId)).append("\n");
    sb.append("    pointClassOverride: ").append(toIndentedString(pointClassOverride)).append("\n");
    sb.append("    equipmentOverrides: ").append(toIndentedString(equipmentOverrides)).append("\n");
    sb.append("    details: ").append(toIndentedString(details)).append("\n");
    sb.append("    normalizedName: ").append(toIndentedString(normalizedName)).append("\n");
    sb.append("    oldNormalizedName: ").append(toIndentedString(oldNormalizedName)).append("\n");
    sb.append("    normalizationHistory: ").append(toIndentedString(normalizationHistory)).append("\n");
    sb.append("    lastResult: ").append(toIndentedString(lastResult)).append("\n");
    sb.append("    currentResult: ").append(toIndentedString(currentResult)).append("\n");
    sb.append("    equipments: ").append(toIndentedString(equipments)).append("\n");
    sb.append("    equipmentRelationships: ").append(toIndentedString(equipmentRelationships)).append("\n");
    sb.append("    locations: ").append(toIndentedString(locations)).append("\n");
    sb.append("    matchingDimensions: ").append(toIndentedString(matchingDimensions)).append("\n");
    sb.append("    normalizationTags: ").append(toIndentedString(normalizationTags)).append("\n");
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