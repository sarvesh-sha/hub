/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.location;

import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Tuple;

import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.model.DeliveryOptions;
import com.optio3.cloud.hub.model.asset.AssetFilterRequest;
import com.optio3.cloud.hub.model.asset.LocationFilterRequest;
import com.optio3.cloud.hub.model.location.GeoFence;
import com.optio3.cloud.hub.model.location.Location;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.model.location.LongitudeLatitude;
import com.optio3.cloud.hub.persistence.FixupProcessingRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord_;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.persistence.MetadataField;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.logging.Logger;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.IdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Entity
@Table(name = "ASSET_LOCATION")
@DynamicUpdate // Due to HHH-11506
@Indexed
@Optio3TableInfo(externalId = "Location", model = Location.class, metamodel = LocationRecord_.class, metadata = LocationRecord.WellKnownMetadata.class)
public class LocationRecord extends AssetRecord
{
    public static class FixupForAddress extends FixupProcessingRecord.Handler
    {
        public static class FacilityAddress
        {
            public String street;
            public String city;
            public String state;
            public String zip;
            public String country;
        }

        @Override
        public Result process(Logger logger,
                              SessionHolder sessionHolder) throws
                                                           Exception
        {
            RecordHelper<LocationRecord> helper = sessionHolder.createHelper(LocationRecord.class);
            for (LocationRecord rec : helper.listAll())
            {
                MetadataMap map = rec.getMetadata();

                FacilityAddress address = map.getObject("locationAddress", FacilityAddress.class);
                if (address != null)
                {
                    StringBuilder sb = new StringBuilder();

                    append(sb, address.street);
                    append(sb, address.city);
                    append(sb, address.state, address.zip);
                    append(sb, address.country);

                    WellKnownMetadata.locationAddress.put(map, sb.toString());

                    if (rec.setMetadata(map))
                    {
                        rec.dontRefreshUpdatedOn();
                    }
                }
            }

            return Result.Done;
        }

        private static void append(StringBuilder sb,
                                   String... parts)
        {
            boolean first = true;
            for (String part : parts)
            {
                if (StringUtils.isNotBlank(part))
                {
                    if (sb.length() > 0)
                    {
                        sb.append(first ? ", " : " ");
                    }

                    first = false;

                    sb.append(part.equals("US") ? "USA" : part);
                }
            }
        }
    }

    public static class WellKnownMetadata implements Optio3TableInfo.IMetadataDigest
    {
        public static final MetadataField<String>            locationTimeZone   = new MetadataField<>("locationTimeZone", String.class);
        public static final MetadataField<String>            locationPhone      = new MetadataField<>("locationPhone", String.class);
        public static final MetadataField<String>            locationAddress    = new MetadataField<>("locationAddress", String.class);
        public static final MetadataField<LongitudeLatitude> locationCoordinate = new MetadataField<>("locationCoordinate", LongitudeLatitude.class);
        public static final MetadataField<GeoFences>         locationGeoFences  = new MetadataField<>("locationGeoFences", GeoFences.class);

        public static final MetadataField<DeliveryOptions> locationEmailSettings = new MetadataField<>("locationEmailSettings", DeliveryOptions.class);
        public static final MetadataField<DeliveryOptions> locationSmsSettings   = new MetadataField<>("locationSmsSettings", DeliveryOptions.class);
    }

    public static class GeoFences
    {
        public List<GeoFence> lst;
    }

    //--//

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private LocationType type;

    //--//

    @OneToMany(mappedBy = "location", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private Set<AssetRecord> assets;

    //--//

    public LocationRecord()
    {
    }

    public LocationType getType()
    {
        return type;
    }

    public void setType(LocationType type)
    {
        this.type = type;
    }

    @Field
    public String getLocationNameForSearch()
    {
        return getName();
    }

    public String getPhone()
    {
        return getMetadata(WellKnownMetadata.locationPhone);
    }

    public void setPhone(String phone)
    {
        putMetadata(WellKnownMetadata.locationPhone, phone);
    }

    public String getAddress()
    {
        return getMetadata(WellKnownMetadata.locationAddress);
    }

    public void setAddress(String address)
    {
        putMetadata(WellKnownMetadata.locationAddress, address);
    }

    public String getTimeZone()
    {
        return getMetadata(WellKnownMetadata.locationTimeZone);
    }

    public void setTimeZone(String timezone)
    {
        putMetadata(WellKnownMetadata.locationTimeZone, timezone);
    }

    public static LongitudeLatitude getGeo(MetadataMap metadataMap)
    {
        LongitudeLatitude geo = WellKnownMetadata.locationCoordinate.get(metadataMap);

        return geo != null && geo.isValid() ? geo : null;
    }

    public LongitudeLatitude getGeo()
    {
        return getGeo(getMetadata());
    }

    public void setFences(List<GeoFence> fences)
    {
        GeoFences fencesWrapper;

        if (fences != null)
        {
            for (GeoFence fence : fences)
            {
                if (fence.uniqueId == null)
                {
                    fence.uniqueId = IdGenerator.newGuid();
                }

                fence.validate();
            }

            fencesWrapper     = new GeoFences();
            fencesWrapper.lst = fences;
        }
        else
        {
            fencesWrapper = null;
        }

        putMetadata(WellKnownMetadata.locationGeoFences, fencesWrapper);
    }

    public static List<GeoFence> getFences(MetadataMap map)
    {
        GeoFences fencesWrapper = WellKnownMetadata.locationGeoFences.get(map);
        return fencesWrapper != null ? fencesWrapper.lst : null;
    }

    public List<GeoFence> getFences()
    {
        return getFences(getMetadata());
    }

    public void setGeo(LongitudeLatitude geo)
    {
        putMetadata(WellKnownMetadata.locationCoordinate, geo);
    }

    public Set<AssetRecord> getAssets()
    {
        return CollectionUtils.asEmptyCollectionIfNull(assets);
    }

    //--//

    public LocationRecord getTopLevelLocation()
    {
        return getTopLevelLocation(this);
    }

    public static LocationRecord getTopLevelLocation(LocationRecord rec)
    {
        if (rec == null)
        {
            return null;
        }

        while (true)
        {
            LocationRecord rec_parent = Reflection.as(rec.getParentAsset(), LocationRecord.class);
            if (rec_parent == null)
            {
                break;
            }

            rec = rec_parent;
        }

        return rec;
    }

    //--//

    private static class LocationJoinHelper<T> extends AssetJoinHelper<T, LocationRecord>
    {
        LocationJoinHelper(RecordHelper<LocationRecord> helper,
                           Class<T> clz)
        {
            super(helper, clz);
        }

        //--//

        void applyFilters(LocationFilterRequest filters)
        {
            if (filters.hasGeoFences)
            {
                LocationsEngine          engine   = helper.getService(LocationsEngine.class);
                LocationsEngine.Snapshot snapshot = engine.acquireSnapshot(false);

                filters.sysIds = snapshot.withGeoFences();
                if (CollectionUtils.isEmpty(filters.sysIds))
                {
                    // The list has to contain something to properly filter.
                    filters.sysIds.add("<none>");
                }
            }

            super.applyFilters(filters);
        }

        @Override
        protected void subfilterByNoParents(AssetFilterRequest filters)
        {
            addWhereClauseIsNull(root, AssetRecord_.parentAsset);
        }
    }

    public static List<RecordIdentity> filterLocations(RecordHelper<LocationRecord> helper,
                                                       LocationFilterRequest filters)
    {
        LocationJoinHelper<Tuple> jh = new LocationJoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return AssetJoinHelper.returnFilterTuples(helper, jh);
    }

    public static long countLocations(RecordHelper<LocationRecord> helper,
                                      LocationFilterRequest filters)
    {
        LocationJoinHelper<Tuple> jh = new LocationJoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return jh.count();
    }
    //--//

    @Override
    public void assetPostCreate(SessionHolder sessionHolder)
    {
        AssetRecord rec_parent = getParentAsset();
        if (rec_parent != null && !(SessionHolder.isEntityOfClass(rec_parent, LocationRecord.class)))
        {
            throw Exceptions.newRuntimeException("Parent of location is not a Location");
        }
    }

    @Override
    protected void assetPostUpdateInner(SessionHolder sessionHolder)
    {
        // Nothing to do.
    }

    @Override
    protected boolean canRemoveChildren()
    {
        // TODO: do we store other things under a Location record? For now, okay to delete recursively.
        return true;
    }
}
