/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.jobs;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.builder.model.jobs.JobStatus;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.builder.persistence.worker.TrackedRecordWithResources;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(JobRecord.class)
public abstract class JobRecord_ extends com.optio3.cloud.builder.persistence.worker.RecordWithResources_
{

    public static volatile ListAttribute<JobRecord, JobSourceRecord>            sources;
    public static volatile SingularAttribute<JobRecord, String>                 name;
    public static volatile SingularAttribute<JobRecord, String>                 commit;
    public static volatile ListAttribute<JobRecord, TrackedRecordWithResources> acquiredResources;
    public static volatile SingularAttribute<JobRecord, String>                 idPrefix;
    public static volatile ListAttribute<JobRecord, RegistryTaggedImageRecord>  generatedImages;
    public static volatile SingularAttribute<JobRecord, JobDefinitionRecord>    definition;
    public static volatile SingularAttribute<JobRecord, String>                 branch;
    public static volatile ListAttribute<JobRecord, JobStepRecord>              steps;
    public static volatile SingularAttribute<JobRecord, JobStatus>              status;
    public static volatile SingularAttribute<JobRecord, String>                 triggeredBy;

    public static final String SOURCES            = "sources";
    public static final String NAME               = "name";
    public static final String COMMIT             = "commit";
    public static final String ACQUIRED_RESOURCES = "acquiredResources";
    public static final String ID_PREFIX          = "idPrefix";
    public static final String GENERATED_IMAGES   = "generatedImages";
    public static final String DEFINITION         = "definition";
    public static final String BRANCH             = "branch";
    public static final String STEPS              = "steps";
    public static final String STATUS             = "status";
    public static final String TRIGGERED_BY       = "triggeredBy";
}

