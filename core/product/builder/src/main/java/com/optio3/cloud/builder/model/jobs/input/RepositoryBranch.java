/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs.input;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryCommitRecord;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;

public class RepositoryBranch extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<RepositoryRecord> repository;

    //--//

    public String name;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<RepositoryCommitRecord> head;
}
