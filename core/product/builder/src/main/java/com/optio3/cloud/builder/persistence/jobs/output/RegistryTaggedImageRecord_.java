/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.jobs.output;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.builder.model.jobs.output.RegistryImageReleaseStatus;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(RegistryTaggedImageRecord.class)
public abstract class RegistryTaggedImageRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<RegistryTaggedImageRecord, JobRecord>                  owningJob;
    public static volatile SingularAttribute<RegistryTaggedImageRecord, RegistryImageRecord>        image;
    public static volatile SingularAttribute<RegistryTaggedImageRecord, RegistryImageReleaseStatus> releaseStatus;
    public static volatile SingularAttribute<RegistryTaggedImageRecord, String>                     tag;

    public static final String OWNING_JOB     = "owningJob";
    public static final String IMAGE          = "image";
    public static final String RELEASE_STATUS = "releaseStatus";
    public static final String TAG            = "tag";
}

