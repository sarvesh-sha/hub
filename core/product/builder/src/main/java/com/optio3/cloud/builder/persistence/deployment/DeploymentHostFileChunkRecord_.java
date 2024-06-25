/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.deployment;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(DeploymentHostFileChunkRecord.class)
public abstract class DeploymentHostFileChunkRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<DeploymentHostFileChunkRecord, DeploymentHostFileRecord> owningFile;
    public static volatile SingularAttribute<DeploymentHostFileChunkRecord, Integer>                  sequenceNumber;
    public static volatile SingularAttribute<DeploymentHostFileChunkRecord, byte[]>                   contents;
    public static volatile SingularAttribute<DeploymentHostFileChunkRecord, Integer>                  length;

    public static final String OWNING_FILE     = "owningFile";
    public static final String SEQUENCE_NUMBER = "sequenceNumber";
    public static final String CONTENTS        = "contents";
    public static final String LENGTH          = "length";
}

