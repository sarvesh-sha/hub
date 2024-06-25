/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.asset;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.hub.model.asset.AssetRelationship;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(RelationshipRecord.class)
public abstract class RelationshipRecord_
{

    public static volatile SingularAttribute<RelationshipRecord, AssetRecord>       childAsset;
    public static volatile SingularAttribute<RelationshipRecord, AssetRecord>       parentAsset;
    public static volatile SingularAttribute<RelationshipRecord, AssetRelationship> relation;

    public static final String CHILD_ASSET  = "childAsset";
    public static final String PARENT_ASSET = "parentAsset";
    public static final String RELATION     = "relation";
}

