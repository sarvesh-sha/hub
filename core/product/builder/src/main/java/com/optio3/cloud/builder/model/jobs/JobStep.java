/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs;

import java.time.ZonedDateTime;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.worker.DockerContainerRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;

public class JobStep extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<JobRecord> owningJob;

    //--//

    @Optio3MapAsReadOnly
    public String name;

    @Optio3MapAsReadOnly
    public JobStatus status;

    @Optio3MapAsReadOnly
    public ZonedDateTime lastOutput;

    @Optio3MapAsReadOnly
    public int lastOffset;

    //--//

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<HostRecord> boundHost;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<DockerContainerRecord> container;
}
