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

public class TaskSpec
{

    public ContainerSpec                 containerSpec = null;
    public ResourceRequirements          resources     = null;
    public TaskRestartPolicy             restartPolicy = null;
    public Placement                     placement     = null;
    /**
     * A counter that triggers an update even if no relevant parameters have been changed.
     */
    public Integer                       forceUpdate   = null;
    public List<NetworkAttachmentConfig> networks      = new ArrayList<NetworkAttachmentConfig>();
    /**
     * Specifies the log driver to use for tasks created from this spec. If not present, the default one for the swarm will be used, finally falling back to the engine default if not specified.
     */
    public Driver                        logDriver     = null;

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("class TaskSpec {\n");

        sb.append("    containerSpec: ")
          .append(toIndentedString(containerSpec))
          .append("\n");
        sb.append("    resources: ")
          .append(toIndentedString(resources))
          .append("\n");
        sb.append("    restartPolicy: ")
          .append(toIndentedString(restartPolicy))
          .append("\n");
        sb.append("    placement: ")
          .append(toIndentedString(placement))
          .append("\n");
        sb.append("    forceUpdate: ")
          .append(toIndentedString(forceUpdate))
          .append("\n");
        sb.append("    networks: ")
          .append(toIndentedString(networks))
          .append("\n");
        sb.append("    logDriver: ")
          .append(toIndentedString(logDriver))
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