/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.jobs.input;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(RepositoryCommitRecord.class)
public abstract class RepositoryCommitRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<RepositoryCommitRecord, String>           authorName;
    public static volatile SingularAttribute<RepositoryCommitRecord, RepositoryRecord> repository;
    public static volatile SingularAttribute<RepositoryCommitRecord, String>           message;
    public static volatile SingularAttribute<RepositoryCommitRecord, String>           authorEmailAddress;
    public static volatile SingularAttribute<RepositoryCommitRecord, String>           parents;

    public static final String AUTHOR_NAME          = "authorName";
    public static final String REPOSITORY           = "repository";
    public static final String MESSAGE              = "message";
    public static final String AUTHOR_EMAIL_ADDRESS = "authorEmailAddress";
    public static final String PARENTS              = "parents";
}

