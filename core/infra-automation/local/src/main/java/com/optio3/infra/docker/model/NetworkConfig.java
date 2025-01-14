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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public class NetworkConfig
{

    public String                        bridge                 = null;
    public String                        sandboxID              = null;
    public Boolean                       hairpinMode            = null;
    public String                        linkLocalIPv6Address   = null;
    public Integer                       linkLocalIPv6PrefixLen = null;
    public Map<String, JsonNode>         ports                  = new HashMap<String, JsonNode>();
    public String                        sandboxKey             = null;
    public List<JsonNode>                secondaryIPAddresses   = new ArrayList<JsonNode>();
    public List<JsonNode>                secondaryIPv6Addresses = new ArrayList<JsonNode>();
    public String                        endpointID             = null;
    public String                        gateway                = null;
    public String                        globalIPv6Address      = null;
    public Integer                       globalIPv6PrefixLen    = null;
    public String                        ipAddress              = null;
    public Integer                       ipPrefixLen            = null;
    public String                        ipv6Gateway            = null;
    public String                        macAddress             = null;
    public Map<String, EndpointSettings> networks               = new HashMap<String, EndpointSettings>();

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("class NetworkConfig {\n");

        sb.append("    bridge: ")
          .append(toIndentedString(bridge))
          .append("\n");
        sb.append("    sandboxID: ")
          .append(toIndentedString(sandboxID))
          .append("\n");
        sb.append("    hairpinMode: ")
          .append(toIndentedString(hairpinMode))
          .append("\n");
        sb.append("    linkLocalIPv6Address: ")
          .append(toIndentedString(linkLocalIPv6Address))
          .append("\n");
        sb.append("    linkLocalIPv6PrefixLen: ")
          .append(toIndentedString(linkLocalIPv6PrefixLen))
          .append("\n");
        sb.append("    ports: ")
          .append(toIndentedString(ports))
          .append("\n");
        sb.append("    sandboxKey: ")
          .append(toIndentedString(sandboxKey))
          .append("\n");
        sb.append("    secondaryIPAddresses: ")
          .append(toIndentedString(secondaryIPAddresses))
          .append("\n");
        sb.append("    secondaryIPv6Addresses: ")
          .append(toIndentedString(secondaryIPv6Addresses))
          .append("\n");
        sb.append("    endpointID: ")
          .append(toIndentedString(endpointID))
          .append("\n");
        sb.append("    gateway: ")
          .append(toIndentedString(gateway))
          .append("\n");
        sb.append("    globalIPv6Address: ")
          .append(toIndentedString(globalIPv6Address))
          .append("\n");
        sb.append("    globalIPv6PrefixLen: ")
          .append(toIndentedString(globalIPv6PrefixLen))
          .append("\n");
        sb.append("    ipAddress: ")
          .append(toIndentedString(ipAddress))
          .append("\n");
        sb.append("    ipPrefixLen: ")
          .append(toIndentedString(ipPrefixLen))
          .append("\n");
        sb.append("    ipv6Gateway: ")
          .append(toIndentedString(ipv6Gateway))
          .append("\n");
        sb.append("    macAddress: ")
          .append(toIndentedString(macAddress))
          .append("\n");
        sb.append("    networks: ")
          .append(toIndentedString(networks))
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
