/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.time.ZonedDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3DontMap;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.model.jobs.output.RegistryImage;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryImageRecord;
import com.optio3.cloud.model.BaseModelWithHeartbeat;
import com.optio3.cloud.model.TypedRecordIdentity;

public class DeploymentTask extends BaseModelWithHeartbeat
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<DeploymentHostRecord> deployment;

    //--//

    @Optio3MapAsReadOnly
    public String dockerId;

    @Optio3MapAsReadOnly
    public DeploymentStatus status;

    @Optio3MapAsReadOnly
    public String image;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<RegistryImageRecord> imageReference;

    //--//

    @Optio3MapAsReadOnly
    public String name;

    public DeploymentRole purpose;

    @Optio3MapAsReadOnly
    public Map<String, String> labels = Maps.newHashMap();

    @Optio3MapAsReadOnly
    public Map<String, Mountpoint> mounts;

    @Optio3MapAsReadOnly
    public int restartCount;

    //--//

    @Optio3MapAsReadOnly
    public ZonedDateTime lastOutput;

    @Optio3MapAsReadOnly
    public int lastOffset;

    //--//

    @Optio3DontMap
    @JsonIgnore
    public RegistryImage rawImage;

    //--//

    public DeploymentRole ensurePurpose()
    {
        if (purpose == null && rawImage != null)
        {
            purpose = rawImage.targetService;
        }

        return purpose;
    }
}
