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

import com.optio3.cloud.client.builder.model.DeploymentCellularCharge;
import com.optio3.cloud.client.builder.model.DeploymentCellularChargePerHost;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class DeploymentCellularChargesSummary
{

  public Integer count = null;
  public DeploymentCellularCharge last24Hours = null;
  public DeploymentCellularCharge last7Days = null;
  public DeploymentCellularCharge last14Days = null;
  public DeploymentCellularCharge last21Days = null;
  public DeploymentCellularCharge last30Days = null;
  public List<DeploymentCellularChargePerHost> last24HoursPerHost = new ArrayList<DeploymentCellularChargePerHost>();
  public List<DeploymentCellularChargePerHost> last7DaysPerHost = new ArrayList<DeploymentCellularChargePerHost>();
  public List<DeploymentCellularChargePerHost> last14DaysPerHost = new ArrayList<DeploymentCellularChargePerHost>();
  public List<DeploymentCellularChargePerHost> last21DaysPerHost = new ArrayList<DeploymentCellularChargePerHost>();
  public List<DeploymentCellularChargePerHost> last30DaysPerHost = new ArrayList<DeploymentCellularChargePerHost>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class DeploymentCellularChargesSummary {\n");

    sb.append("    count: ").append(toIndentedString(count)).append("\n");
    sb.append("    last24Hours: ").append(toIndentedString(last24Hours)).append("\n");
    sb.append("    last7Days: ").append(toIndentedString(last7Days)).append("\n");
    sb.append("    last14Days: ").append(toIndentedString(last14Days)).append("\n");
    sb.append("    last21Days: ").append(toIndentedString(last21Days)).append("\n");
    sb.append("    last30Days: ").append(toIndentedString(last30Days)).append("\n");
    sb.append("    last24HoursPerHost: ").append(toIndentedString(last24HoursPerHost)).append("\n");
    sb.append("    last7DaysPerHost: ").append(toIndentedString(last7DaysPerHost)).append("\n");
    sb.append("    last14DaysPerHost: ").append(toIndentedString(last14DaysPerHost)).append("\n");
    sb.append("    last21DaysPerHost: ").append(toIndentedString(last21DaysPerHost)).append("\n");
    sb.append("    last30DaysPerHost: ").append(toIndentedString(last30DaysPerHost)).append("\n");
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
