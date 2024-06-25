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
import com.optio3.cloud.client.hub.model.BrandingConfiguration;
import com.optio3.cloud.client.hub.model.PaneCardConfiguration;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;

public class PaneConfiguration
{

  public String id = null;
  public String name = null;
  public BrandingConfiguration branding = null;
  public AssetGraphBinding titleInput = null;
  public AssetGraph graph = null;
  public List<PaneCardConfiguration> elements = new ArrayList<PaneCardConfiguration>();

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("class PaneConfiguration {\n");

    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    branding: ").append(toIndentedString(branding)).append("\n");
    sb.append("    titleInput: ").append(toIndentedString(titleInput)).append("\n");
    sb.append("    graph: ").append(toIndentedString(graph)).append("\n");
    sb.append("    elements: ").append(toIndentedString(elements)).append("\n");
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
