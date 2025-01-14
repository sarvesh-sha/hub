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

import com.optio3.cloud.client.hub.model.HostAsset;
import com.optio3.cloud.client.hub.model.RecordIdentity;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class BackgroundActivity
{
  public static final String RECORD_IDENTITY = "BackgroundActivity";




  public String sysId = null;
  public ZonedDateTime createdOn = null;
  public ZonedDateTime updatedOn = null;
  public String title = null;
  public RecordIdentity context = null;
  public BackgroundActivityStatus status = null;
  public Long timeout = null;
  public ZonedDateTime nextActivation = null;
  public ZonedDateTime lastActivation = null;
  public BackgroundActivityStatus lastActivationStatus = null;
  public String lastActivationFailure = null;
  public String lastActivationFailureTrace = null;
  public HostAsset worker = null;
  public List<RecordIdentity> waitingActivities = new ArrayList<RecordIdentity>();
  public List<RecordIdentity> subActivities = new ArrayList<RecordIdentity>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class BackgroundActivity {\n");

    sb.append("    sysId: ").append(toIndentedString(sysId)).append("\n");
    sb.append("    createdOn: ").append(toIndentedString(createdOn)).append("\n");
    sb.append("    updatedOn: ").append(toIndentedString(updatedOn)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    context: ").append(toIndentedString(context)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    timeout: ").append(toIndentedString(timeout)).append("\n");
    sb.append("    nextActivation: ").append(toIndentedString(nextActivation)).append("\n");
    sb.append("    lastActivation: ").append(toIndentedString(lastActivation)).append("\n");
    sb.append("    lastActivationStatus: ").append(toIndentedString(lastActivationStatus)).append("\n");
    sb.append("    lastActivationFailure: ").append(toIndentedString(lastActivationFailure)).append("\n");
    sb.append("    lastActivationFailureTrace: ").append(toIndentedString(lastActivationFailureTrace)).append("\n");
    sb.append("    worker: ").append(toIndentedString(worker)).append("\n");
    sb.append("    waitingActivities: ").append(toIndentedString(waitingActivities)).append("\n");
    sb.append("    subActivities: ").append(toIndentedString(subActivities)).append("\n");
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
