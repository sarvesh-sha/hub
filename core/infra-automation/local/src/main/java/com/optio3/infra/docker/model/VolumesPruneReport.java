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

public class VolumesPruneReport
{

    /**
     * Volumes that were deleted
     */
    public List<String> volumesDeleted = new ArrayList<String>();
    /**
     * Disk space reclaimed in bytes
     */
    public Long         spaceReclaimed = null;

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("class VolumesPruneReport {\n");

        sb.append("    volumesDeleted: ")
          .append(toIndentedString(volumesDeleted))
          .append("\n");
        sb.append("    spaceReclaimed: ")
          .append(toIndentedString(spaceReclaimed))
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
