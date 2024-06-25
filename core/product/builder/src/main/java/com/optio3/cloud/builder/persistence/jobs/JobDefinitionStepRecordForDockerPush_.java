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

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(JobDefinitionStepRecordForDockerPush.class)
public abstract class JobDefinitionStepRecordForDockerPush_ extends com.optio3.cloud.builder.persistence.jobs.JobDefinitionStepRecord_
{

    public static volatile SingularAttribute<JobDefinitionStepRecordForDockerPush, String> sourceImage;
    public static volatile SingularAttribute<JobDefinitionStepRecordForDockerPush, String> imageTag;

    public static final String SOURCE_IMAGE = "sourceImage";
    public static final String IMAGE_TAG    = "imageTag";
}

