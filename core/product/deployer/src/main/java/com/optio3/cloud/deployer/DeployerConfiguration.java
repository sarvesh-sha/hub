/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.deployer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.optio3.cloud.AbstractConfiguration;
import com.optio3.cloud.client.deployer.model.DeploymentAgentFeature;

public class DeployerConfiguration extends AbstractConfiguration
{
    // TODO: UPGRADE PATCH: Legacy field, from when Hibernate was a dependency.
    public void setDatabase(JsonNode node)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setConnectionUsername(String connectionUsername)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setConnectionPassword(String connectionPassword)
    {
    }

    //--//

    public String connectionUrl;

    //--//

    public String hostId;
    public String instanceId;

    public String watchdogFile;
    public String heartbeatFile;

    public String IMSI;
    public String IMEI;
    public String ICCID;

    public String bootConfig = "/optio3-boot/optio3_config.txt";

    //--//

    public boolean sendTasksInformationWithHeartbeat = true;

    public Set<DeploymentAgentFeature> supportedFeatures;

    //--//

    @JsonIgnore
    public Path getAgentFilesRoot()
    {
        return Paths.get(scratchDirectory, "AgentFiles");
    }
}
