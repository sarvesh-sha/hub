/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

// NOTE: Generated automatically by script refreshMetamodel.sh!!
package com.optio3.cloud.hub.persistence.asset;

import java.time.ZonedDateTime;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import com.optio3.cloud.hub.model.asset.AssetState;
import com.optio3.cloud.hub.persistence.event.EventRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(AssetRecord.class)
public abstract class AssetRecord_ extends com.optio3.cloud.persistence.RecordWithMetadata_
{

    public static volatile SingularAttribute<AssetRecord, String>         serialNumber;
    public static volatile SingularAttribute<AssetRecord, Boolean>        hidden;
    public static volatile SingularAttribute<AssetRecord, String>         customerNotes;
    public static volatile SingularAttribute<AssetRecord, ZonedDateTime>  lastCheckedDate;
    public static volatile ListAttribute<AssetRecord, RelationshipRecord> relationsAsChild;
    public static volatile SingularAttribute<AssetRecord, ZonedDateTime>  lastUpdatedDate;
    public static volatile ListAttribute<AssetRecord, RelationshipRecord> relationsAsParent;
    public static volatile SingularAttribute<AssetRecord, String>         assetId;
    public static volatile SingularAttribute<AssetRecord, String>         name;
    public static volatile SingularAttribute<AssetRecord, LocationRecord> location;
    public static volatile SingularAttribute<AssetRecord, AssetState>     state;
    public static volatile SingularAttribute<AssetRecord, AssetRecord>    parentAsset;
    public static volatile ListAttribute<AssetRecord, EventRecord>        events;

    public static final String SERIAL_NUMBER       = "serialNumber";
    public static final String HIDDEN              = "hidden";
    public static final String CUSTOMER_NOTES      = "customerNotes";
    public static final String LAST_CHECKED_DATE   = "lastCheckedDate";
    public static final String RELATIONS_AS_CHILD  = "relationsAsChild";
    public static final String LAST_UPDATED_DATE   = "lastUpdatedDate";
    public static final String RELATIONS_AS_PARENT = "relationsAsParent";
    public static final String ASSET_ID            = "assetId";
    public static final String NAME                = "name";
    public static final String LOCATION            = "location";
    public static final String STATE               = "state";
    public static final String PARENT_ASSET        = "parentAsset";
    public static final String EVENTS              = "events";
}

