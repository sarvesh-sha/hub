/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs.output;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.cloud.annotation.Optio3DontMap;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceBackupRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryImageRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class RegistryTaggedImage extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<JobRecord> owningJob;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<RegistryImageRecord> image;

    @Optio3MapAsReadOnly
    public String tag;

    @Optio3MapAsReadOnly
    public RegistryImageReleaseStatus releaseStatus;

    //--//

    @Optio3DontMap
    @JsonIgnore
    public RegistryImage rawImage;
}
