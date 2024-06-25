/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs.input;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;

public class RepositoryCommit extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<RepositoryRecord> repository;

    @Optio3MapAsReadOnly
    public String commitHash;

    //--//

    @Optio3MapAsReadOnly
    public String message;

    @Optio3MapAsReadOnly
    public String authorName;

    @Optio3MapAsReadOnly
    public String authorEmailAddress;

    @Optio3MapAsReadOnly
    public String[] parents;
}
