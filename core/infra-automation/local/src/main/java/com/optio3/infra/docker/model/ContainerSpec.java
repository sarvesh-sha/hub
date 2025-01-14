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

public class ContainerSpec
{

    /**
     * The image name to use for the container.
     */
    public String                image           = null;
    /**
     * User-defined key/value data.
     */
    public Map<String, String>   labels          = new HashMap<String, String>();
    /**
     * The command to be run in the image.
     */
    public List<String>          command         = new ArrayList<String>();
    /**
     * Arguments to the command.
     */
    public List<String>          args            = new ArrayList<String>();
    /**
     * The hostname to use for the container, as a valid RFC 1123 hostname.
     */
    public String                hostname        = null;
    /**
     * A list of environment variables in the form `VAR=value`.
     */
    public List<String>          env             = new ArrayList<String>();
    /**
     * The working directory for commands to run in.
     */
    public String                dir             = null;
    /**
     * The user inside the container.
     */
    public String                user            = null;
    /**
     * A list of additional groups that the container process will run as.
     */
    public List<String>          groups          = new ArrayList<String>();
    /**
     * Whether a pseudo-TTY should be allocated.
     */
    public Boolean               TTY             = null;
    /**
     * Open `stdin`
     */
    public Boolean               openStdin       = null;
    /**
     * Mount the container's root filesystem as read only.
     */
    public Boolean               readOnly        = null;
    /**
     * Specification for mounts to be added to containers created as part of the service.
     */
    public List<Mount>           mounts          = new ArrayList<Mount>();
    /**
     * Signal to stop the container.
     */
    public String                stopSignal      = null;
    /**
     * Amount of time to wait for the container to terminate before forcefully killing it.
     */
    public Long                  stopGracePeriod = null;
    public HealthConfig          healthCheck     = null;
    /**
     * A list of hostnames/IP mappings to add to the container's `/etc/hosts` file. The format of extra hosts on swarmkit is specified in: http://man7.org/linux/man-pages/man5/hosts.5.html   IP_address canonical_hostname [aliases...]
     */
    public List<String>          hosts           = new ArrayList<String>();
    public DNSConfig             dnSConfig       = null;
    /**
     * Secrets contains references to zero or more secrets that will be exposed to the service.
     */
    public List<SecretReference> secrets         = new ArrayList<SecretReference>();

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("class ContainerSpec {\n");

        sb.append("    image: ")
          .append(toIndentedString(image))
          .append("\n");
        sb.append("    labels: ")
          .append(toIndentedString(labels))
          .append("\n");
        sb.append("    command: ")
          .append(toIndentedString(command))
          .append("\n");
        sb.append("    args: ")
          .append(toIndentedString(args))
          .append("\n");
        sb.append("    hostname: ")
          .append(toIndentedString(hostname))
          .append("\n");
        sb.append("    env: ")
          .append(toIndentedString(env))
          .append("\n");
        sb.append("    dir: ")
          .append(toIndentedString(dir))
          .append("\n");
        sb.append("    user: ")
          .append(toIndentedString(user))
          .append("\n");
        sb.append("    groups: ")
          .append(toIndentedString(groups))
          .append("\n");
        sb.append("    TTY: ")
          .append(toIndentedString(TTY))
          .append("\n");
        sb.append("    openStdin: ")
          .append(toIndentedString(openStdin))
          .append("\n");
        sb.append("    readOnly: ")
          .append(toIndentedString(readOnly))
          .append("\n");
        sb.append("    mounts: ")
          .append(toIndentedString(mounts))
          .append("\n");
        sb.append("    stopSignal: ")
          .append(toIndentedString(stopSignal))
          .append("\n");
        sb.append("    stopGracePeriod: ")
          .append(toIndentedString(stopGracePeriod))
          .append("\n");
        sb.append("    healthCheck: ")
          .append(toIndentedString(healthCheck))
          .append("\n");
        sb.append("    hosts: ")
          .append(toIndentedString(hosts))
          .append("\n");
        sb.append("    dnSConfig: ")
          .append(toIndentedString(dnSConfig))
          .append("\n");
        sb.append("    secrets: ")
          .append(toIndentedString(secrets))
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
