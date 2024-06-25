/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs.input;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryBranchRecord;
import com.optio3.cloud.builder.persistence.worker.RepositoryCheckoutRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class Repository extends BaseModel
{
    public String name;

    public String gitUrl;

    //--//

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<RepositoryBranchRecord> branches = new TypedRecordIdentityList<>();

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<RepositoryCheckoutRecord> checkouts = new TypedRecordIdentityList<>();
}
