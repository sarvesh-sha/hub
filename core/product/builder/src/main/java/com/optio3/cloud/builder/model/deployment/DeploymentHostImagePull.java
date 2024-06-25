/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.time.ZonedDateTime;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.model.jobs.JobStatus;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;

public class DeploymentHostImagePull extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<DeploymentHostRecord> deployment;

    //--//

    @Optio3MapAsReadOnly
    public String image;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<RegistryTaggedImageRecord> imageReference;

    @Optio3MapAsReadOnly
    public JobStatus status;

    //--//

    @Optio3MapAsReadOnly
    public ZonedDateTime lastOutput;

    @Optio3MapAsReadOnly
    public int lastOffset;
}
