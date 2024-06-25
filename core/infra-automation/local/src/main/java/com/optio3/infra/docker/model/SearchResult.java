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

public class SearchResult
{

    public String  description  = null;
    public Boolean is_official  = null;
    public Boolean is_automated = null;
    public String  name         = null;
    public Integer star_count   = null;

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("class SearchResult {\n");

        sb.append("    description: ")
          .append(toIndentedString(description))
          .append("\n");
        sb.append("    is_official: ")
          .append(toIndentedString(is_official))
          .append("\n");
        sb.append("    is_automated: ")
          .append(toIndentedString(is_automated))
          .append("\n");
        sb.append("    name: ")
          .append(toIndentedString(name))
          .append("\n");
        sb.append("    star_count: ")
          .append(toIndentedString(star_count))
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
