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

public class Plugin
{

    public String         id              = null;
    public String         name            = null;
    /**
     * True when the plugin is running. False when the plugin is not running, only installed.
     */
    public Boolean        enabled         = null;
    public PluginSettings settings        = null;
    /**
     * plugin remote reference used to push/pull the plugin
     */
    public String         pluginReference = null;
    public PluginConfig   config          = null;

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("class Plugin {\n");

        sb.append("    id: ")
          .append(toIndentedString(id))
          .append("\n");
        sb.append("    name: ")
          .append(toIndentedString(name))
          .append("\n");
        sb.append("    enabled: ")
          .append(toIndentedString(enabled))
          .append("\n");
        sb.append("    settings: ")
          .append(toIndentedString(settings))
          .append("\n");
        sb.append("    pluginReference: ")
          .append(toIndentedString(pluginReference))
          .append("\n");
        sb.append("    config: ")
          .append(toIndentedString(config))
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
