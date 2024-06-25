/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.persistence;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(CommonLogRecord.class)
public abstract class CommonLogRecord_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<CommonLogRecord, Integer> sequenceStart;
    public static volatile SingularAttribute<CommonLogRecord, byte[]>  block;
    public static volatile SingularAttribute<CommonLogRecord, Integer> sequenceEnd;

    public static final String SEQUENCE_START = "sequenceStart";
    public static final String BLOCK          = "block";
    public static final String SEQUENCE_END   = "sequenceEnd";
}

