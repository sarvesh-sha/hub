/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

public class DeploymentAgentStatus
{
    // Use String instead of DeploymentAgentFeature, to make it backward compatible, in case we remove features.
    public Set<String> supportedFeatures;

    /**
     * Local time on the host
     */
    public ZonedDateTime localTime;

    /**
     * The host this Deployer Agent runs on.
     */
    public String hostId;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setHostName(String val)
    {
    }

    /**
     * A unique id assigned to the Agent, to distinguish it from other agents running on the same host.
     */
    public String instanceId;

    /**
     * The container hosting the Agent.
     */
    public String dockerId;

    /**
     * Info about modem.
     */
    public DeployerCellularInfo cellular;

    //--//

    /**
     * The architecture of the Deployer host.
     */
    public DockerImageArchitecture architecture;

    public int  availableProcessors;
    public long freeMemory;
    public long totalMemory;
    public long maxMemory;

    public long diskTotal;
    public long diskFree;

    public double cpuUsageUser   = Double.NaN;
    public double cpuUsageSystem = Double.NaN;

    public float                         batteryVoltage = Float.NaN;
    public float                         cpuTemperature = Float.NaN;
    public DeployerShutdownConfiguration shutdownConfiguration;

    public Map<String, String> networkInterfaces = Maps.newHashMap();

    //--//

    /**
     * All the containers running on the Deployer host, if changed.
     */
    public List<ContainerStatus> tasks;

    /**
     * All the images installed on the Deployer host, if changed.
     */
    public List<ImageStatus> images;
}
