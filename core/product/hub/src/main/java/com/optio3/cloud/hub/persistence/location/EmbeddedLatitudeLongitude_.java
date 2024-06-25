/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.location;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(EmbeddedLatitudeLongitude.class)
public abstract class EmbeddedLatitudeLongitude_
{

    public static volatile SingularAttribute<EmbeddedLatitudeLongitude, Double> latitude;
    public static volatile SingularAttribute<EmbeddedLatitudeLongitude, Double> longitude;

    public static final String LATITUDE  = "latitude";
    public static final String LONGITUDE = "longitude";
}

