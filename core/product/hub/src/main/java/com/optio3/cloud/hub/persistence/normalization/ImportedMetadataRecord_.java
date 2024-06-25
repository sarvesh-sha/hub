/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.normalization;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(ImportedMetadataRecord.class)
public abstract class ImportedMetadataRecord_ extends com.optio3.cloud.persistence.RecordWithSequenceNumber_
{

    public static volatile SingularAttribute<ImportedMetadataRecord, String>  metadata;
    public static volatile SingularAttribute<ImportedMetadataRecord, Boolean> active;
    public static volatile SingularAttribute<ImportedMetadataRecord, byte[]>  metadataCompressed;
    public static volatile SingularAttribute<ImportedMetadataRecord, Integer> version;

    public static final String METADATA            = "metadata";
    public static final String ACTIVE              = "active";
    public static final String METADATA_COMPRESSED = "metadataCompressed";
    public static final String VERSION             = "version";
}

