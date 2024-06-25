/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.event;

import static java.util.Objects.requireNonNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Tuple;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.SingularAttribute;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.model.event.Event;
import com.optio3.cloud.hub.model.event.EventFilterRequest;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord_;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.PaginatedRecordIdentityList;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.SortCriteria;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.RecordWithSequenceNumber;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.search.annotations.Field;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "EVENT", indexes = { @Index(name = "EVENT__CREATEDON", columnList = "sys_created_on"), @Index(name = "EVENT__SEQUENCE", columnList = "sequence_number") })
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "Event", model = Event.class, metamodel = EventRecord_.class)
public abstract class EventRecord extends RecordWithSequenceNumber<EventRecord> implements ModelMapperTarget<Event, EventRecord_>
{
    @Optio3ControlNotifications(reason = "We only notify from event to asset", direct = Notify.ON_ASSOCIATION_CHANGES, reverse = Notify.NEVER, getter = "getAsset")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getAsset")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "asset", foreignKey = @ForeignKey(name = "EVENT__ASSET__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private AssetRecord asset;

    @Field
    public String getAssetNameForSearch()
    {
        return asset != null ? asset.getName() : null;
    }

    @Optio3ControlNotifications(reason = "Ignore location changes", direct = Notify.NEVER, reverse = Notify.NEVER)
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getLocation")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "location", foreignKey = @ForeignKey(name = "EVENT__LOCATION__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private LocationRecord location;

    @Column(name = "sequence_number")
    private int sequenceNumber;

    @Column(name = "description")
    private String description;

    @Lob
    @Column(name = "extended_description", length = 8192)
    private String extendedDescription;

    //--//

    public static <T extends EventRecord> T newInstance(RecordHelper<T> helper,
                                                        Integer sequenceNumber)
    {
        T res = Reflection.newInstance(helper.getEntityClass());

        EventRecord res2 = res; // Silly Java scope checking.
        res2.sequenceNumber = res.assignUniqueNumber(helper, sequenceNumber, null);

        return res;
    }

    public static <T extends EventRecord> T newInstance(RecordHelper<T> helper,
                                                        Integer sequenceNumber,
                                                        AssetRecord rec_asset)
    {
        T res = newInstance(helper, sequenceNumber);

        EventRecord res2 = res; // Silly Java scope checking.
        res2.asset = rec_asset;

        return res;
    }

    public static <T extends EventRecord> T newInstance(RecordHelper<T> helper,
                                                        Integer sequenceNumber,
                                                        LocationRecord rec_location)
    {
        requireNonNull(rec_location);

        T res = newInstance(helper, sequenceNumber);

        EventRecord res2 = res; // Silly Java scope checking.
        res2.location = rec_location;

        return res;
    }

    @Override
    protected SingularAttribute<EventRecord, Integer> fetchSequenceNumberField()
    {
        return EventRecord_.sequenceNumber;
    }

    @Override
    protected int fetchSequenceNumberValue()
    {
        return getSequenceNumber();
    }

    //--//

    public AssetRecord getAsset()
    {
        return asset;
    }

    public DeviceRecord getDevice()
    {
        return Reflection.as(asset, DeviceRecord.class);
    }

    @Field
    public String getAssetModelNameForSearch()
    {
        DeviceRecord deviceInfo = getDevice();
        return deviceInfo != null ? deviceInfo.getModelName() : null;
    }

    @Field
    public String getAssetProductNameForSearch()
    {
        DeviceRecord deviceInfo = getDevice();
        return deviceInfo != null ? deviceInfo.getProductName() : null;
    }

    @Field
    public String getAssetManufacturerNameForSearch()
    {
        DeviceRecord deviceInfo = getDevice();
        return deviceInfo != null ? deviceInfo.getManufacturerName() : null;
    }

    public LocationRecord getLocation()
    {
        return location;
    }

    public int getSequenceNumber()
    {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber)
    {
        this.sequenceNumber = sequenceNumber;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean setDescription(String description)
    {
        if (StringUtils.equals(this.description, description))
        {
            return false; // Nothing changed.
        }

        this.description = description;
        return true;
    }

    public String getExtendedDescription()
    {
        return extendedDescription;
    }

    public boolean setExtendedDescription(String extendedDescription)
    {
        if (StringUtils.equals(this.extendedDescription, extendedDescription))
        {
            return false; // Nothing changed.
        }

        this.extendedDescription = extendedDescription;
        return true;
    }

    //--//

    protected static class JoinHelper<T, R extends EventRecord> extends QueryHelperWithCommonFields<T, R>
    {
        Join<R, AssetRecord>               rootAsset;
        Join<R, DeviceRecord>              rootDevice;
        Join<DeviceRecord, LocationRecord> rootLocation;

        SortCriteria sortByLocation;

        protected JoinHelper(RecordHelper<R> helper,
                             Class<T> clz)
        {
            super(helper, clz);
        }

        //--//

        public Join<R, AssetRecord> joinWithAsset()
        {
            if (rootAsset == null)
            {
                rootAsset = root.join(EventRecord_.asset);
            }

            return rootAsset;
        }

        public Join<R, DeviceRecord> joinWithDevice()
        {
            if (rootDevice == null)
            {
                rootDevice = cb.treat(joinWithAsset(), DeviceRecord.class);
            }

            return rootDevice;
        }

        public Join<DeviceRecord, LocationRecord> joinWithLocation()
        {
            if (rootLocation == null)
            {
                rootLocation = joinWithDevice().join(DeviceRecord_.location);
            }

            return rootLocation;
        }

        //--//

        protected void applyFilters(EventFilterRequest filters)
        {
            if (filters.hasAssets())
            {
                filterByAssets(filters.assetIDs);
            }

            if (filters.hasLocations())
            {
                filterByLocations(filters.locationIDs, filters.locationInclusive);
            }

            SingularAttribute<RecordWithCommonFields, ZonedDateTime> timeAttribute = filters.evaluateUpdatedOn ? RecordWithCommonFields_.updatedOn : RecordWithCommonFields_.createdOn;
            filterTimestampsCoveredByTargetRange(root, timeAttribute, filters.rangeStart, filters.rangeEnd);

            //--//

            filterByManufacturerName(filters.likeDeviceManufacturerName);
            filterByProductName(filters.likeDeviceProductName);
            filterByModelName(filters.likeDeviceModelName);

            //--//

            if (filters.hasSorting())
            {
                for (SortCriteria sort : filters.sortBy)
                {
                    handleSortCriteria(sort);
                }
            }
        }

        protected void handleSortCriteria(SortCriteria sort)
        {
            switch (sort.column)
            {
                case "description":
                {
                    addOrderBy(root, EventRecord_.description, sort.ascending);
                    break;
                }

                case "device.location":
                {
                    //
                    // Because locations are hierarchical, trying to sort by location at the SQL level is too expensive.
                    // So we don't sort, but collect the location information.
                    // After we run the query, we sort the results in memory.
                    //
                    sortByLocation = sort;
                    break;
                }

                case "createdOn":
                {
                    addOrderBy(root, RecordWithCommonFields_.createdOn, sort.ascending);
                    break;
                }

                case "updatedOn":
                {
                    addOrderBy(root, RecordWithCommonFields_.updatedOn, sort.ascending);
                    break;
                }
            }
        }

        private void filterByAssets(List<String> assetIDs)
        {
            if (!assetIDs.isEmpty())
            {
                addWhereReferencingSysIds(root, EventRecord_.asset, assetIDs);
            }
        }

        private void filterByLocations(List<String> locationIDs,
                                       boolean recursive)
        {
            Set<String> recursiveLocationIDs = Sets.newHashSet();

            LocationsEngine          locationsEngine   = helper.getServiceNonNull(LocationsEngine.class);
            LocationsEngine.Snapshot locationsSnapshot = locationsEngine.acquireSnapshot(false);

            for (String locationId : locationIDs)
            {
                locationsSnapshot.recursivelyCollectStructure(recursiveLocationIDs, recursive, locationId);
            }

            if (!recursiveLocationIDs.isEmpty())
            {
                addWhereReferencingSysIds(joinWithDevice(), DeviceRecord_.location, recursiveLocationIDs);
            }
        }

        //--//

        void filterByManufacturerName(String likeName)
        {
            List<ParsedLike> likeFilters = ParsedLike.decode(likeName);
            if (likeFilters != null)
            {
                joinWithDevice();

                addWhereClause(predicateForLike(rootDevice, DeviceRecord_.manufacturerName, likeFilters));
            }
        }

        void filterByProductName(String likeName)
        {
            List<ParsedLike> likeFilters = ParsedLike.decode(likeName);
            if (likeFilters != null)
            {
                joinWithDevice();

                addWhereClause(predicateForLike(rootDevice, DeviceRecord_.productName, likeFilters));
            }
        }

        void filterByModelName(String likeName)
        {
            List<ParsedLike> likeFilters = ParsedLike.decode(likeName);
            if (likeFilters != null)
            {
                joinWithDevice();

                addWhereClause(predicateForLike(rootDevice, DeviceRecord_.modelName, likeFilters));
            }
        }

        //--//

        public static <R extends EventRecord> PaginatedRecordIdentityList returnFilterTuples(RecordHelper<R> helper,
                                                                                             JoinHelper<Tuple, R> jh)
        {
            PaginatedRecordIdentityList res = new PaginatedRecordIdentityList();

            if (jh.sortByLocation != null)
            {
                boolean ascending = jh.sortByLocation.ascending;

                Path<DeviceRecord> rootDevice = jh.joinWithDevice();
                jh.cq.multiselect(jh.root.get(RecordWithCommonFields_.sysId), jh.root.get(RecordWithCommonFields_.updatedOn), rootDevice.get(DeviceRecord_.location));

                Multimap<LocationRecord, RecordIdentity> map = ArrayListMultimap.create();

                for (Tuple t : jh.list())
                {
                    TypedRecordIdentity<R> ri           = RecordIdentity.newInstance(helper, t, 0, 1);
                    LocationRecord         rec_location = (LocationRecord) t.get(2);
                    if (rec_location != null)
                    {
                        map.put(rec_location, ri);
                    }
                }

                List<LocationRecord> locations = Lists.newArrayList(map.keySet());
                locations.sort((l, r) ->
                               {
                                   int diff = l.getName()
                                               .compareToIgnoreCase(r.getName());

                                   return ascending ? diff : -diff;
                               });

                for (LocationRecord rec_location : locations)
                {
                    res.results.addAll(map.get(rec_location));
                }
            }
            else
            {
                jh.cq.multiselect(jh.root.get(RecordWithCommonFields_.sysId), jh.root.get(RecordWithCommonFields_.updatedOn));

                for (Tuple t : jh.list())
                {
                    RecordIdentity ri = RecordIdentity.newInstance(helper, t, 0, 1);
                    res.results.add(ri);
                }
            }

            return res;
        }
    }

    //--//

    public static <T extends EventRecord> List<T> getBatch(RecordHelper<T> helper,
                                                           List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public static PaginatedRecordIdentityList filter(RecordHelper<EventRecord> helper,
                                                     EventFilterRequest filters)
    {
        JoinHelper<Tuple, EventRecord> jh = new JoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        //--//

        return JoinHelper.returnFilterTuples(helper, jh);
    }

    public static long count(RecordHelper<EventRecord> helper,
                             EventFilterRequest filters)
    {
        JoinHelper<Tuple, EventRecord> jh = new JoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return jh.count();
    }

    public static Map<String, Number> countEventsByLocation(RecordHelper<EventRecord> helper,
                                                            EventFilterRequest filters)
    {
        JoinHelper<Tuple, EventRecord> jh = new JoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            filters.sortBy = null; // Just in case, sanitize input.

            jh.applyFilters(filters);
        }

        return jh.countByField(jh.joinWithLocation(), RecordWithCommonFields_.sysId);
    }
}
