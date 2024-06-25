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

import com.optio3.cloud.client.hub.model.RecordIdentity;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class ReportDefinition
{
  public static final String RECORD_IDENTITY = "ReportDefinition";


  public String sysId = null;
  public ZonedDateTime createdOn = null;
  public ZonedDateTime updatedOn = null;
  public RecordIdentity user = null;
  public String title = null;
  public String description = null;
  public Boolean active = null;
  public ZonedDateTime autoDelete = null;
  public RecordIdentity headVersion = null;
  public RecordIdentity releaseVersion = null;
  public List<RecordIdentity> reports = new ArrayList<RecordIdentity>();
  public List<RecordIdentity> versions = new ArrayList<RecordIdentity>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReportDefinition {\n");

    sb.append("    sysId: ").append(toIndentedString(sysId)).append("\n");
    sb.append("    createdOn: ").append(toIndentedString(createdOn)).append("\n");
    sb.append("    updatedOn: ").append(toIndentedString(updatedOn)).append("\n");
    sb.append("    user: ").append(toIndentedString(user)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
    sb.append("    autoDelete: ").append(toIndentedString(autoDelete)).append("\n");
    sb.append("    headVersion: ").append(toIndentedString(headVersion)).append("\n");
    sb.append("    releaseVersion: ").append(toIndentedString(releaseVersion)).append("\n");
    sb.append("    reports: ").append(toIndentedString(reports)).append("\n");
    sb.append("    versions: ").append(toIndentedString(versions)).append("\n");
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
