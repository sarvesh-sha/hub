/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;

public class JobSource extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<JobRecord> owningJob;

    //--//

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<RepositoryRecord> repo;

    @Optio3MapAsReadOnly
    public String branch;

    @Optio3MapAsReadOnly
    public String commit;
}
