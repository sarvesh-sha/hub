/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.worker;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;

public class DockerTemporaryImage extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<HostRecord> owningHost;

    //--//

    @Optio3MapAsReadOnly
    public String imageTag;
}
