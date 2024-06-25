/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.persistence;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(RecordForRecurringActivity.class)
public abstract class RecordForRecurringActivity_ extends com.optio3.cloud.persistence.RecordWithCommonFields_
{

    public static volatile SingularAttribute<RecordForRecurringActivity, ZonedDateTime> nextActivation;
    public static volatile SingularAttribute<RecordForRecurringActivity, String>        title;

    public static final String NEXT_ACTIVATION = "nextActivation";
    public static final String TITLE           = "title";
}

