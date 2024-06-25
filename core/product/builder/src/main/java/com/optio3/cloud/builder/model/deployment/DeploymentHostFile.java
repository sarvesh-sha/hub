/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.time.ZonedDateTime;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;

public class DeploymentHostFile extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<DeploymentHostRecord> deployment;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<DeploymentTaskRecord> task;

    @Optio3MapAsReadOnly
    public String taskName;

    //--//

    @Optio3MapAsReadOnly
    public String path;

    @Optio3MapAsReadOnly
    public long length;

    @Optio3MapAsReadOnly
    public ZonedDateTime downloadedOn;

    @Optio3MapAsReadOnly
    public ZonedDateTime uploadedOn;
}
