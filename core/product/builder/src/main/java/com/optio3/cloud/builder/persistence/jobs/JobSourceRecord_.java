/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.jobs;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.builder.persistence.jobs.input.RepositoryRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(JobSourceRecord.class)
public abstract class JobSourceRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<JobSourceRecord, JobRecord>        owningJob;
    public static volatile SingularAttribute<JobSourceRecord, RepositoryRecord> repo;
    public static volatile SingularAttribute<JobSourceRecord, String>           commit;
    public static volatile SingularAttribute<JobSourceRecord, String>           branch;

    public static final String OWNING_JOB = "owningJob";
    public static final String REPO       = "repo";
    public static final String COMMIT     = "commit";
    public static final String BRANCH     = "branch";
}

