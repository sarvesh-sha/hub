/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.worker;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.persistence.worker.DockerContainerRecord;
import com.optio3.cloud.builder.persistence.worker.DockerVolumeRecord;
import com.optio3.cloud.builder.persistence.worker.ManagedDirectoryRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;

public class MappedDockerVolume extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<DockerContainerRecord> owningContainer;

    @Optio3MapAsReadOnly
    public String path;

    //--//

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<ManagedDirectoryRecord> directory;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<DockerVolumeRecord> volume;
}
