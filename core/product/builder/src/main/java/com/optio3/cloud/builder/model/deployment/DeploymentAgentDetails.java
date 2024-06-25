/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.client.deployer.model.DeployerShutdownConfiguration;
import com.optio3.cloud.client.deployer.model.DeploymentAgentFeature;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;

public class DeploymentAgentDetails
{
    public Set<String> supportedFeatures;

    public DockerImageArchitecture architecture;

    public int availableProcessors;

    public long freeMemory;
    public long totalMemory;
    public long maxMemory;

    public long diskTotal;
    public long diskFree;

    public double cpuUsageUser   = Float.NaN;
    public double cpuUsageSystem = Float.NaN;

    public float                         batteryVoltage = Float.NaN;
    public float                         cpuTemperature = Float.NaN;
    public DeployerShutdownConfiguration shutdownConfiguration;

    public Map<String, String> networkInterfaces = Maps.newHashMap();

    @JsonIgnore
    public List<DeploymentAgentFeature> getSupportedKnownFeatures()
    {
        List<DeploymentAgentFeature> lst = Lists.newArrayList();

        if (supportedFeatures != null)
        {
            for (String supportedFeature : supportedFeatures)
            {
                try
                {
                    lst.add(DeploymentAgentFeature.valueOf(supportedFeature));
                }
                catch (IllegalArgumentException e)
                {
                    // Old feature, ignore.
                }
            }
        }

        return lst;
    }

    //--//

    public boolean canSupport(DeploymentAgentFeature... features)
    {
        for (DeploymentAgentFeature feature : features)
        {
            if (supportedFeatures == null || !supportedFeatures.contains(feature.name()))
            {
                return false;
            }
        }

        return true;
    }
}
