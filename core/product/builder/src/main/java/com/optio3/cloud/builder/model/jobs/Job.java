/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.persistence.jobs.JobSourceRecord;
import com.optio3.cloud.builder.persistence.jobs.JobStepRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.builder.persistence.worker.DockerContainerRecord;
import com.optio3.cloud.builder.persistence.worker.DockerTemporaryImageRecord;
import com.optio3.cloud.builder.persistence.worker.DockerVolumeRecord;
import com.optio3.cloud.builder.persistence.worker.ManagedDirectoryRecord;
import com.optio3.cloud.builder.persistence.worker.RepositoryCheckoutRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class Job extends BaseModel
{
    @Optio3MapAsReadOnly
    public String name;

    @Optio3MapAsReadOnly
    public String idPrefix;

    @Optio3MapAsReadOnly
    public JobStatus status;

    @Optio3MapAsReadOnly
    public String branch;

    @Optio3MapAsReadOnly
    public String commit;

    @Optio3MapAsReadOnly
    public String triggeredBy;

    //--//

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<JobSourceRecord> sources = new TypedRecordIdentityList<>();

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<JobStepRecord> steps = new TypedRecordIdentityList<>();

    //--//

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<RegistryTaggedImageRecord> generatedImages = new TypedRecordIdentityList<>();

    //--//

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<DockerContainerRecord> acquiredContainers = new TypedRecordIdentityList<>();

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<DockerVolumeRecord> acquiredVolumes = new TypedRecordIdentityList<>();

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<DockerTemporaryImageRecord> acquiredTemporaryImages = new TypedRecordIdentityList<>();

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<ManagedDirectoryRecord> acquiredDirectories = new TypedRecordIdentityList<>();

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<RepositoryCheckoutRecord> acquiredCheckouts = new TypedRecordIdentityList<>();
}
