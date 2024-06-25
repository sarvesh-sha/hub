/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.worker;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.annotation.Optio3MapToPersistence;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.builder.persistence.worker.MappedDockerVolumeRecord;
import com.optio3.cloud.builder.persistence.worker.RepositoryCheckoutRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class ManagedDirectory extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<HostRecord> owningHost;

    //--//

    @Optio3MapAsReadOnly
    @Optio3MapToPersistence(value = "pathAsString")
    public String path;

    //--//

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<RepositoryCheckoutRecord> checkoutsForDb = new TypedRecordIdentityList<>();

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<RepositoryCheckoutRecord> checkoutsForWork = new TypedRecordIdentityList<>();

    @Optio3MapAsReadOnly
    public TypedRecordIdentityList<MappedDockerVolumeRecord> mappedIn = new TypedRecordIdentityList<>();
}
