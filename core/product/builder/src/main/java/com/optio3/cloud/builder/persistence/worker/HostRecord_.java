/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.builder.persistence.worker;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(HostRecord.class)
public abstract class HostRecord_ extends com.optio3.cloud.builder.persistence.worker.HostBoundResource_
{

    public static volatile SingularAttribute<HostRecord, byte[]>        logRanges;
    public static volatile SingularAttribute<HostRecord, String>        domainName;
    public static volatile SingularAttribute<HostRecord, String>        ipAddress;
    public static volatile ListAttribute<HostRecord, HostBoundResource> resources;
    public static volatile SingularAttribute<HostRecord, Integer>       lastOffset;
    public static volatile SingularAttribute<HostRecord, ZonedDateTime> lastOutput;

    public static final String LOG_RANGES  = "logRanges";
    public static final String DOMAIN_NAME = "domainName";
    public static final String IP_ADDRESS  = "ipAddress";
    public static final String RESOURCES   = "resources";
    public static final String LAST_OFFSET = "lastOffset";
    public static final String LAST_OUTPUT = "lastOutput";
}

