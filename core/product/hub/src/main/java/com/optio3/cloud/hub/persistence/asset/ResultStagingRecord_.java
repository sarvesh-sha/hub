/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.asset;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(ResultStagingRecord.class)
public abstract class ResultStagingRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<ResultStagingRecord, Boolean>       objectsProcessed;
    public static volatile SingularAttribute<ResultStagingRecord, Boolean>       samplesProcessed;
    public static volatile SingularAttribute<ResultStagingRecord, ZonedDateTime> rangeStart;
    public static volatile SingularAttribute<ResultStagingRecord, byte[]>        contents;
    public static volatile SingularAttribute<ResultStagingRecord, Integer>       samplesCount;
    public static volatile SingularAttribute<ResultStagingRecord, ZonedDateTime> rangeEnd;

    public static final String OBJECTS_PROCESSED = "objectsProcessed";
    public static final String SAMPLES_PROCESSED = "samplesProcessed";
    public static final String RANGE_START       = "rangeStart";
    public static final String CONTENTS          = "contents";
    public static final String SAMPLES_COUNT     = "samplesCount";
    public static final String RANGE_END         = "rangeEnd";
}

