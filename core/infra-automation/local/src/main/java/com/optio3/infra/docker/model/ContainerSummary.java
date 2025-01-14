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

public class ContainerSummary
{

    /**
     * The ID of this container
     */
    public String              id              = null;
    /**
     * The names that this container has been given
     */
    public List<String>        names           = new ArrayList<String>();
    /**
     * The name of the image used when creating this container
     */
    public String              image           = null;
    /**
     * The ID of the image that this container was created from
     */
    public String              imageID         = null;
    /**
     * Command to run when starting the container
     */
    public String              command         = null;
    /**
     * When the container was created
     */
    public Long                created         = null;
    /**
     * The ports exposed by this container
     */
    public List<Port>          ports           = new ArrayList<Port>();
    /**
     * The size of files that have been created or changed by this container
     */
    public Long                sizeRw          = null;
    /**
     * The total size of all the files in this container
     */
    public Long                sizeRootFs      = null;
    /**
     * User-defined key/value metadata.
     */
    public Map<String, String> labels          = new HashMap<String, String>();
    /**
     * The state of this container (e.g. `Exited`)
     */
    public String              state           = null;
    /**
     * Additional human-readable status of this container (e.g. `Exit 0`)
     */
    public String              status          = null;
    public HostConfigSummary   hostConfig      = null;
    public NetworkSummary      networkSettings = null;
    public List<Mount>         mounts          = new ArrayList<Mount>();

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("class ContainerSummary {\n");

        sb.append("    id: ")
          .append(toIndentedString(id))
          .append("\n");
        sb.append("    names: ")
          .append(toIndentedString(names))
          .append("\n");
        sb.append("    image: ")
          .append(toIndentedString(image))
          .append("\n");
        sb.append("    imageID: ")
          .append(toIndentedString(imageID))
          .append("\n");
        sb.append("    command: ")
          .append(toIndentedString(command))
          .append("\n");
        sb.append("    created: ")
          .append(toIndentedString(created))
          .append("\n");
        sb.append("    ports: ")
          .append(toIndentedString(ports))
          .append("\n");
        sb.append("    sizeRw: ")
          .append(toIndentedString(sizeRw))
          .append("\n");
        sb.append("    sizeRootFs: ")
          .append(toIndentedString(sizeRootFs))
          .append("\n");
        sb.append("    labels: ")
          .append(toIndentedString(labels))
          .append("\n");
        sb.append("    state: ")
          .append(toIndentedString(state))
          .append("\n");
        sb.append("    status: ")
          .append(toIndentedString(status))
          .append("\n");
        sb.append("    hostConfig: ")
          .append(toIndentedString(hostConfig))
          .append("\n");
        sb.append("    networkSettings: ")
          .append(toIndentedString(networkSettings))
          .append("\n");
        sb.append("    mounts: ")
          .append(toIndentedString(mounts))
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
