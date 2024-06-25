/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import java.time.ZonedDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.optio3.cloud.annotation.Optio3AutoTrim;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.annotation.Optio3MapToPersistence;
import com.optio3.cloud.hub.model.HostAsset;
import com.optio3.cloud.hub.model.location.Location;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.BaseModelWithMetadata;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.protocol.model.BaseAssetDescriptor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = Device.class),
                @JsonSubTypes.Type(value = NetworkAsset.class),
                @JsonSubTypes.Type(value = GatewayAsset.class),
                @JsonSubTypes.Type(value = DeviceElement.class),
                @JsonSubTypes.Type(value = HostAsset.class),
                @JsonSubTypes.Type(value = Location.class),
                @JsonSubTypes.Type(value = LogicalAsset.class) })
public abstract class Asset extends BaseModelWithMetadata
{
    @Optio3MapAsReadOnly
    @Optio3AutoTrim()
    public String name;

    @Optio3MapAsReadOnly
    @Optio3AutoTrim()
    public String physicalName;

    @Optio3MapAsReadOnly
    @Optio3AutoTrim()
    public String logicalName;

    @Optio3MapAsReadOnly
    @Optio3AutoTrim()
    public String normalizedName;

    @Optio3AutoTrim()
    public String displayName;

    //--//

    public AssetState state;

    public String assetId;

    public String serialNumber;

    public String customerNotes;

    public ZonedDateTime lastCheckedDate;
    public ZonedDateTime lastUpdatedDate;

    public boolean hidden;

    //--//

    public TypedRecordIdentity<LocationRecord> location;

    //--//

    @Optio3MapAsReadOnly
    public String pointClassId;

    @Optio3MapAsReadOnly
    public String equipmentClassId;

    @Optio3MapAsReadOnly
    public String azureDigitalTwinModel;

    @Optio3MapAsReadOnly
    @Optio3MapToPersistence("equipment")
    public boolean isEquipment;

    //--//

    @Optio3MapAsReadOnly
    public Set<String> classificationTags;

    public Set<String> manualTags;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<AssetRecord> parentAsset;

    @Optio3MapAsReadOnly
    public BaseAssetDescriptor identityDescriptor;

    //--//

    public abstract AssetRecord newRecord();
}
