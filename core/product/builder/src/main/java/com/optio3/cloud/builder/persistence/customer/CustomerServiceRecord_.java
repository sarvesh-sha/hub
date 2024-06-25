/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.customer;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.builder.model.customer.CustomerVertical;
import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentInstance;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.persistence.EncryptedPayload;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(CustomerServiceRecord.class)
public abstract class CustomerServiceRecord_ extends com.optio3.cloud.persistence.RecordWithMetadata_
{

    public static volatile SingularAttribute<CustomerServiceRecord, DeploymentOperationalStatus>   operationalStatus;
    public static volatile SingularAttribute<CustomerServiceRecord, byte[]>                        logRanges;
    public static volatile SingularAttribute<CustomerServiceRecord, EncryptedPayload>              masterKey;
    public static volatile SingularAttribute<CustomerServiceRecord, DeploymentInstance>            instanceType;
    public static volatile SingularAttribute<CustomerServiceRecord, Integer>                       lastOffset;
    public static volatile SingularAttribute<CustomerServiceRecord, CustomerVertical>              vertical;
    public static volatile SingularAttribute<CustomerServiceRecord, String>                        instanceRegion;
    public static volatile SingularAttribute<CustomerServiceRecord, EncryptedPayload>              maintPassword;
    public static volatile ListAttribute<CustomerServiceRecord, CustomerServiceSecretRecord>       secrets;
    public static volatile SingularAttribute<CustomerServiceRecord, String>                        url;
    public static volatile SingularAttribute<CustomerServiceRecord, String>                        instanceAccount;
    public static volatile SingularAttribute<CustomerServiceRecord, ZonedDateTime>                 lastOutput;
    public static volatile SingularAttribute<CustomerServiceRecord, Boolean>                       useDemoData;
    public static volatile SingularAttribute<CustomerServiceRecord, Integer>                       diskSize;
    public static volatile SingularAttribute<CustomerServiceRecord, String>                        extraConfigLinesActive;
    public static volatile SingularAttribute<CustomerServiceRecord, BackgroundActivityRecord>      currentActivity;
    public static volatile SingularAttribute<CustomerServiceRecord, EmbeddedDatabaseConfiguration> dbConfiguration;
    public static volatile SingularAttribute<CustomerServiceRecord, String>                        name;
    public static volatile SingularAttribute<CustomerServiceRecord, CustomerRecord>                customer;
    public static volatile SingularAttribute<CustomerServiceRecord, String>                        extraConfigLines;
    public static volatile ListAttribute<CustomerServiceRecord, CustomerServiceBackupRecord>       backups;

    public static final String OPERATIONAL_STATUS        = "operationalStatus";
    public static final String LOG_RANGES                = "logRanges";
    public static final String MASTER_KEY                = "masterKey";
    public static final String INSTANCE_TYPE             = "instanceType";
    public static final String LAST_OFFSET               = "lastOffset";
    public static final String VERTICAL                  = "vertical";
    public static final String INSTANCE_REGION           = "instanceRegion";
    public static final String MAINT_PASSWORD            = "maintPassword";
    public static final String SECRETS                   = "secrets";
    public static final String URL                       = "url";
    public static final String INSTANCE_ACCOUNT          = "instanceAccount";
    public static final String LAST_OUTPUT               = "lastOutput";
    public static final String USE_DEMO_DATA             = "useDemoData";
    public static final String DISK_SIZE                 = "diskSize";
    public static final String EXTRA_CONFIG_LINES_ACTIVE = "extraConfigLinesActive";
    public static final String CURRENT_ACTIVITY          = "currentActivity";
    public static final String DB_CONFIGURATION          = "dbConfiguration";
    public static final String NAME                      = "name";
    public static final String CUSTOMER                  = "customer";
    public static final String EXTRA_CONFIG_LINES        = "extraConfigLines";
    public static final String BACKUPS                   = "backups";
}

