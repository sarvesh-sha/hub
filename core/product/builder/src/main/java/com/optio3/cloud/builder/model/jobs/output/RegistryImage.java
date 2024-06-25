/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs.output;

import java.time.ZonedDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3DontMap;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.model.BaseModelWithMetadata;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class RegistryImage extends BaseModelWithMetadata
{
    @Optio3MapAsReadOnly
    public String imageSha;

    @Optio3MapAsReadOnly
    public DockerImageArchitecture architecture;

    @Optio3MapAsReadOnly
    public ZonedDateTime buildTime;

    @Optio3MapAsReadOnly
    public DeploymentRole targetService;

    @Optio3MapAsReadOnly
    public Map<String, String> labels = Maps.newHashMap();

    //--//

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<RegistryTaggedImageRecord> referencingTags = new TypedRecordIdentityList<>();

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<DeploymentTaskRecord> referencingTasks = new TypedRecordIdentityList<>();

    //--//

    @Optio3DontMap
    @JsonIgnore
    public boolean isRelease;

    @Optio3DontMap
    @JsonIgnore
    public boolean isReleaseCandidate;
}
