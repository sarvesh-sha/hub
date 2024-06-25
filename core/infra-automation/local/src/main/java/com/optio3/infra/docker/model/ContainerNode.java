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

public class ContainerNode
{

    public String  ID     = null;
    public String  IP     = null;
    public String  addr   = null;
    public String  name   = null;
    public Integer cpus   = null;
    public Integer memory = null;
    public String  labels = null;

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("class ContainerNode {\n");

        sb.append("    ID: ")
          .append(toIndentedString(ID))
          .append("\n");
        sb.append("    IP: ")
          .append(toIndentedString(IP))
          .append("\n");
        sb.append("    addr: ")
          .append(toIndentedString(addr))
          .append("\n");
        sb.append("    name: ")
          .append(toIndentedString(name))
          .append("\n");
        sb.append("    cpus: ")
          .append(toIndentedString(cpus))
          .append("\n");
        sb.append("    memory: ")
          .append(toIndentedString(memory))
          .append("\n");
        sb.append("    labels: ")
          .append(toIndentedString(labels))
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