/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.customer;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(CustomerServiceBackupRecord.class)
public abstract class CustomerServiceBackupRecord_ extends com.optio3.cloud.persistence.RecordWithMetadata_
{

    public static volatile SingularAttribute<CustomerServiceBackupRecord, CustomerServiceRecord> customerService;
    public static volatile SingularAttribute<CustomerServiceBackupRecord, Long>                  fileSize;
    public static volatile SingularAttribute<CustomerServiceBackupRecord, String>                fileIdOnAgent;
    public static volatile SingularAttribute<CustomerServiceBackupRecord, Boolean>               pendingTransfer;
    public static volatile SingularAttribute<CustomerServiceBackupRecord, BackupKind>            trigger;
    public static volatile SingularAttribute<CustomerServiceBackupRecord, String>                fileId;
    public static volatile SingularAttribute<CustomerServiceBackupRecord, String>                extraConfigLines;

    public static final String CUSTOMER_SERVICE   = "customerService";
    public static final String FILE_SIZE          = "fileSize";
    public static final String FILE_ID_ON_AGENT   = "fileIdOnAgent";
    public static final String PENDING_TRANSFER   = "pendingTransfer";
    public static final String TRIGGER            = "trigger";
    public static final String FILE_ID            = "fileId";
    public static final String EXTRA_CONFIG_LINES = "extraConfigLines";
}

