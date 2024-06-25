/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Docker Engine API
 * The Engine API is an HTTP API served by Docker Engine. It is the API the Docker client uses to communicate with the Engine, so everything the Docker client can do can be done with the API.
 *
 * OpenAPI spec version: 1.28
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.infra.docker.model;

import java.util.ArrayList;
import java.util.List;

public class DiskUsage
{

    public Long                   layersSize = null;
    public List<ImageSummary>     images     = new ArrayList<ImageSummary>();
    public List<ContainerSummary> containers = new ArrayList<ContainerSummary>();
    public List<Volume>           volumes    = new ArrayList<Volume>();

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("class DiskUsage {\n");

        sb.append("    layersSize: ")
          .append(toIndentedString(layersSize))
          .append("\n");
        sb.append("    images: ")
          .append(toIndentedString(images))
          .append("\n");
        sb.append("    containers: ")
          .append(toIndentedString(containers))
          .append("\n");
        sb.append("    volumes: ")
          .append(toIndentedString(volumes))
          .append("\n");
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
        {
            return "null";
        }
        return o.toString()
                .replace("\n", "\n    ");
    }
}
