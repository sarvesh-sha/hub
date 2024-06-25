/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.cloud.annotation.Optio3DontMap;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.model.jobs.output.RegistryImage;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.model.BaseModelWithHeartbeat;
import com.optio3.cloud.model.TypedRecordIdentity;
import org.apache.commons.lang3.StringUtils;

public class DeploymentAgent extends BaseModelWithHeartbeat
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<DeploymentHostRecord> deployment;

    //--//

    @Optio3MapAsReadOnly
    public String instanceId;

    @Optio3MapAsReadOnly
    public DeploymentStatus status;

    @Optio3MapAsReadOnly
    public boolean active;

    @Optio3MapAsReadOnly
    public String dockerId;

    // Only used on the backend.
    @JsonIgnore
    public String rpcId;

    //--//

    @Optio3MapAsReadOnly
    public DeploymentAgentDetails details;

    //--//

    @Optio3DontMap
    @JsonIgnore
    public DeploymentTask rawTask;

    @JsonIgnore
    public boolean isDefaultAgent()
    {
        return StringUtils.equals(instanceId, "v1");
    }

    public RegistryImage findImage()
    {
        return rawTask != null ? rawTask.rawImage : null;
    }
}
