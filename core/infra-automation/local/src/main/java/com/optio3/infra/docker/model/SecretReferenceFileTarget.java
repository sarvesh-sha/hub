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

public class SecretReferenceFileTarget
{

    /**
     * Name represents the final filename in the filesystem.
     */
    public String  name = null;
    /**
     * UID represents the file UID.
     */
    public String  UID  = null;
    /**
     * GID represents the file GID.
     */
    public String  GID  = null;
    /**
     * Mode represents the FileMode of the file.
     */
    public Integer mode = null;

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("class SecretReferenceFileTarget {\n");

        sb.append("    name: ")
          .append(toIndentedString(name))
          .append("\n");
        sb.append("    UID: ")
          .append(toIndentedString(UID))
          .append("\n");
        sb.append("    GID: ")
          .append(toIndentedString(GID))
          .append("\n");
        sb.append("    mode: ")
          .append(toIndentedString(mode))
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