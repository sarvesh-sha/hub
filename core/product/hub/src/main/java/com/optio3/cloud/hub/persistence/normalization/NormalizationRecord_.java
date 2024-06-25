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
@StaticMetamodel(NormalizationRecord.class)
public abstract class NormalizationRecord_ extends com.optio3.cloud.persistence.RecordWithSequenceNumber_
{

    public static volatile SingularAttribute<NormalizationRecord, Boolean> active;
    public static volatile SingularAttribute<NormalizationRecord, String>  rules;
    public static volatile SingularAttribute<NormalizationRecord, Integer> version;

    public static final String ACTIVE  = "active";
    public static final String RULES   = "rules";
    public static final String VERSION = "version";
}

