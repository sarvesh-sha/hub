/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.asset;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Tuple;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Suppliers;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.annotation.Optio3UpgradeValue;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.logic.normalizations.EquipmentClass;
import com.optio3.cloud.hub.logic.normalizations.NormalizationRules;
import com.optio3.cloud.hub.logic.normalizations.PointClass;
import com.optio3.cloud.hub.logic.protocol.IProtocolDecoder;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.logic.tags.TagsStreamNextAction;
import com.optio3.cloud.hub.model.alert.AlertEventLevel;
import com.optio3.cloud.hub.model.alert.AlertEventType;
import com.optio3.cloud.hub.model.alert.AlertType;
import com.optio3.cloud.hub.model.asset.Asset;
import com.optio3.cloud.hub.model.asset.AssetFilterRequest;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.hub.model.asset.AssetState;
import com.optio3.cloud.hub.model.asset.LogicalAsset;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.model.metrics.MetricsBinding;
import com.optio3.cloud.hub.model.normalization.DeviceElementClassificationMetadata;
import com.optio3.cloud.hub.model.normalization.DeviceElementClassificationOverrides;
import com.optio3.cloud.hub.model.normalization.EquipmentClassificationMetadata;
import com.optio3.cloud.hub.model.normalization.NormalizationEquipmentLocations;
import com.optio3.cloud.hub.model.tags.TagsConditionIsEquipment;
import com.optio3.cloud.hub.model.tags.TagsConditionTermWithValue;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.hub.model.workflow.WorkflowStatus;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionVersionRecord;
import com.optio3.cloud.hub.persistence.alert.AlertHistoryRecord;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.hub.persistence.event.EventRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.hub.persistence.workflow.WorkflowRecord;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.SortCriteria;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.MetadataField;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.MetadataSortExtension;
import com.optio3.cloud.persistence.MetadataTagsMap;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RawQueryHelper;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.RecordWithMetadata;
import com.optio3.cloud.persistence.RecordWithMetadata_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.cloud.persistence.StreamHelperResult;
import com.optio3.cloud.search.HibernateIndexingContext;
import com.optio3.cloud.search.HibernateSearch;
import com.optio3.cloud.search.Optio3HibernateSearchContext;
import com.optio3.cloud.search.Optio3QueryAnalyzerOverride;
import com.optio3.collection.Memoizer;
import com.optio3.logging.Logger;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.serialization.Reflection;
import com.optio3.service.IServiceProvider;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.function.ConsumerWithException;
import com.optio3.util.function.FunctionWithException;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "ASSET")
@DynamicUpdate // Due to HHH-11506
@Analyzer(definition = "fuzzy")
@Optio3QueryAnalyzerOverride("fuzzy_query")
@Optio3HibernateSearchContext(handler = AssetRecord.AssetIndexingHelper.class)
@Optio3TableInfo(externalId = "Asset", model = Asset.class, metamodel = AssetRecord_.class, metadata = AssetRecord.WellKnownMetadata.class)
public abstract class AssetRecord extends RecordWithMetadata implements ModelMapperTarget<Asset, AssetRecord_>
{
    public static class AssetIndexingHelper extends HibernateIndexingContext
    {
        public final Map<String, String> structuralParentLookup = Maps.newHashMap();
        public final Map<String, String> controlsParentLookup   = Maps.newHashMap();
        public final Map<String, String> locationLookup         = Maps.newHashMap();
        public final Map<String, String> nameLookup             = Maps.newHashMap();
        public final Set<String>         isEquipment            = Sets.newHashSet();

        @Override
        public void initialize(AbstractApplicationWithDatabase<?> app,
                               String databaseId,
                               Memoizer memoizer)
        {
            try (SessionHolder sessionHolder = SessionHolder.createWithNewReadOnlySession(app, databaseId, Optio3DbRateLimiter.Normal))
            {
                var singletonRelationModel = new RelationshipRecord.Raw();

                RelationshipRecord.streamAllRelations(sessionHolder, () -> singletonRelationModel, (model) ->
                {
                    switch (model.relation)
                    {
                        case structural:
                            structuralParentLookup.put(model.child, model.parent);
                            break;

                        case controls:
                            controlsParentLookup.put(model.child, model.parent);
                            break;
                    }
                });

                //--//

                class AssetSummary
                {
                    public String sysId;
                    public String locationSysId;
                    public String name;
                    public byte[] metadataCompressed;
                }

                RawQueryHelper<AssetRecord, AssetSummary> qh = new RawQueryHelper<>(sessionHolder, AssetRecord.class);

                qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
                qh.addReferenceRaw(AssetRecord_.location, (obj, val) -> obj.locationSysId = val);
                qh.addString(AssetRecord_.name, (obj, val) -> obj.name = val);
                qh.addObject(RecordWithMetadata_.metadataCompressed, byte[].class, (obj, val) -> obj.metadataCompressed = val);

                // Reuse the same instance, since we don't store the individual models.
                final var singletonAssetModel = new AssetSummary();

                qh.stream(() -> singletonAssetModel, (model) ->
                {
                    String sysId = memoizer.intern(model.sysId);

                    if (model.locationSysId != null)
                    {
                        locationLookup.put(sysId, memoizer.intern(model.locationSysId));
                    }

                    if (model.name != null)
                    {
                        nameLookup.put(sysId, memoizer.intern(model.name));
                    }

                    MetadataMap     metadata = MetadataMap.decodeMetadata(model.metadataCompressed);
                    MetadataTagsMap tags     = accessTags(metadata);
                    if (tags.hasTag(WellKnownTags.isEquipment))
                    {
                        isEquipment.add(sysId);
                    }
                });
            }
        }
    }

    //--//

    public interface IProviderOfPropertyTypeExtractorClass
    {
        Class<? extends PropertyTypeExtractor> getPropertyTypeExtractorClass();
    }

    public static class WellKnownMetadata implements Optio3TableInfo.IMetadataDigest
    {
        public static final String tags = "tags";

        public static final MetadataField<String>                          nameFromLegacyImport          = new MetadataField<>("dashboardName", String.class);
        public static final MetadataField<String>                          equipmentNameFromLegacyImport = new MetadataField<>("dashboardEquipmentName", String.class);
        public static final MetadataField<List<String>>                    structureFromLegacyImport     = new MetadataField<>("dashboardStructure", MetadataField.TypeRef_listOfStrings);
        public static final MetadataField<NormalizationEquipmentLocations> locationsWithType             = new MetadataField<>("locationsWithType", NormalizationEquipmentLocations.class);

        public static final MetadataField<String> physicalName   = new MetadataField<>("physicalName", String.class);
        public static final MetadataField<String> normalizedName = new MetadataField<>("normalizedName", String.class);
        public static final MetadataField<String> displayName    = new MetadataField<>("displayName", String.class);

        public static final MetadataField<DeviceElementClassificationOverrides> overrideClassification = new MetadataField<>("classificationOverrides", DeviceElementClassificationOverrides.class);

        // TimeSeries
        public static final MetadataField<String>         propertyTypeExtractor = new MetadataField<>("propertyTypeExtractor", String.class);
        public static final MetadataField<MetricsBinding> metricsBindings       = new MetadataField<>("metricsBindings", MetricsBinding.class);

        // Classification
        public static final MetadataField<String>           pointClassID            = new MetadataField<>("pointClassID", String.class);
        public static final MetadataField<Boolean>          pointIgnore             = new MetadataField<>("pointIgnore", Boolean.class);
        public static final MetadataField<Double>           pointClassScore         = new MetadataField<>("pointClassScore", Double.class);
        public static final MetadataField<Double>           negativePointClassScore = new MetadataField<>("negativePointClassScore", Double.class);
        public static final MetadataField<Double>           pointClassThreshold     = new MetadataField<>("pointClassThreshold", Double.class);
        public static final MetadataField<List<String>>     pointClassTags          = new MetadataField<>("pointClassTags", MetadataField.TypeRef_listOfStrings);
        public static final MetadataField<String>           equipment               = new MetadataField<>("equipment", String.class);
        public static final MetadataField<String>           equipmentClassID        = new MetadataField<>("equipmentClassID", String.class);
        public static final MetadataField<List<String>>     equipmentClassTags      = new MetadataField<>("equipmentClassTags", MetadataField.TypeRef_listOfStrings);
        public static final MetadataField<String>           equipmentKey            = new MetadataField<>("equipmentKey", String.class);
        public static final MetadataField<List<String>>     parentEquipmentName     = new MetadataField<>("parentEquipment", MetadataField.TypeRef_listOfStrings);
        public static final MetadataField<String>           azureDigitalTwinModel   = new MetadataField<>("azureDigitalTwinModel", String.class);
        public static final MetadataField<EngineeringUnits> assignedUnits           = new MetadataField<>("assignedUnits", EngineeringUnits.class);
    }

    public static class WellKnownTags
    {
        public static final String manualPrefix = "$manual.";

        public static final String sysPrefix        = "$sys.";
        public static final String sysEntityPrefix  = sysPrefix + "$table.";
        public static final String sysLocation      = sysPrefix + "$location";
        public static final String sysMetrics       = sysPrefix + "$metrics";
        public static final String sysMetricsOutput = sysPrefix + "$metricsOutput";

        public static final String pointClassId     = sysPrefix + "pointClassId";
        public static final String equipmentClassId = sysPrefix + "equipmentClassId";

        public static final String hasOverrides = sysPrefix + "hasOverrides";
        public static final String isEquipment  = sysPrefix + "isEquipment";

        public static boolean isManualTag(String tag)
        {
            return tag.startsWith(manualPrefix);
        }

        public static boolean isSystemTag(String tag)
        {
            return tag.startsWith(sysPrefix);
        }

        public static String decodeEntityTable(String tag)
        {
            if (tag.startsWith(sysEntityPrefix))
            {
                return tag.substring(sysEntityPrefix.length());
            }

            return null;
        }

        public static String encodeEntityTable(String table)
        {
            return sysEntityPrefix + table;
        }

        public static String decodeManualTag(String tag)
        {
            if (isManualTag(tag))
            {
                return tag.substring(manualPrefix.length());
            }

            return null;
        }

        public static String encodeManualTag(String tag)
        {
            return isManualTag(tag) ? tag : manualPrefix + tag;
        }

        public static void clearTags(MetadataTagsMap tags,
                                     boolean forSystem,
                                     boolean forClassification,
                                     boolean forManual)
        {
            for (String tag : filterTags(tags.listTags(), forSystem, forClassification, forManual))
            {
                tags.removeTag(tag);
            }
        }

        public static Set<String> getTags(MetadataTagsMap tags,
                                          boolean forSystem,
                                          boolean forClassification,
                                          boolean forManual)
        {
            return filterTags(tags.listTags(), forSystem, forClassification, forManual);
        }

        public static void assignTags(MetadataTagsMap map,
                                      boolean forSystem,
                                      boolean forClassification,
                                      boolean forManual,
                                      Collection<String> tags)
        {
            //
            // Clear category.
            //
            clearTags(map, forSystem, forClassification, forManual);

            for (String tag : filterTags(tags, forSystem, forClassification, forManual))
            {
                map.addTag(tag, true);
            }
        }

        private static Set<String> filterTags(Collection<String> tags,
                                              boolean forSystem,
                                              boolean forClassification,
                                              boolean forManual)
        {
            Set<String> res = Collections.emptySet();

            if (tags != null)
            {
                for (String tag : tags)
                {
                    if (isSystemTag(tag))
                    {
                        if (!forSystem)
                        {
                            continue;
                        }
                    }
                    else if (isManualTag(tag))
                    {
                        if (!forManual)
                        {
                            continue;
                        }
                    }
                    else
                    {
                        if (!forClassification)
                        {
                            continue;
                        }
                    }

                    if (res.isEmpty())
                    {
                        res = Sets.newHashSet();
                    }

                    res.add(tag);
                }
            }

            return res;
        }
    }

    //--//

    public static class EquipmentLookup
    {
        public final TagsEngine.Snapshot      snapshot;
        public final Map<String, String>      equipments = Maps.newHashMap();
        public final Multimap<String, String> classIds   = HashMultimap.create();

        public EquipmentLookup(TagsEngine.Snapshot snapshot)
        {
            this.snapshot = snapshot;

            //
            // First, find all the equipments.
            //
            TagsEngine.Snapshot.AssetSet equipmentSet = snapshot.evaluateCondition(new TagsConditionIsEquipment());
            for (TypedRecordIdentity<? extends AssetRecord> ri : equipmentSet.resolve())
            {
                equipments.put(ri.sysId, null);
            }

            //
            // Then, find the class IDs of the equipments.
            //
            for (String equipmentClassId : snapshot.resolveTagValues(WellKnownTags.equipmentClassId))
            {
                TagsEngine.Snapshot.AssetSet classIdSet = snapshot.evaluateCondition(TagsConditionTermWithValue.build(WellKnownTags.equipmentClassId, equipmentClassId));

                TagsEngine.Snapshot.AssetSet targetSetForClassId = classIdSet.intersection(equipmentSet);
                for (TypedRecordIdentity<? extends AssetRecord> ri : targetSetForClassId.resolve())
                {
                    equipments.put(ri.sysId, equipmentClassId);
                    classIds.put(equipmentClassId, ri.sysId);
                }
            }
        }

        public TypedRecordIdentity<? extends AssetRecord> findParent(String sysId,
                                                                     AssetRelationship relationship)
        {
            if (relationship == null)
            {
                relationship = AssetRelationship.structural;
            }

            TagsEngine.Snapshot.AssetSet set = snapshot.resolveRelations(sysId, relationship, true);

            AtomicReference<TypedRecordIdentity<? extends AssetRecord>> ref = new AtomicReference<>();

            set.streamResolved((ri) ->
                               {
                                   ref.set(ri);
                                   return TagsStreamNextAction.Stop; // Only interested in the first hit.
                               });

            return ref.get();
        }

        public Set<TypedRecordIdentity<? extends AssetRecord>> getChildren(String sysId,
                                                                           AssetRelationship relationship)
        {
            if (relationship == null)
            {
                relationship = AssetRelationship.structural;
            }

            TagsEngine.Snapshot.AssetSet set = snapshot.resolveRelations(sysId, relationship, false);

            return set.resolve();
        }
    }

    //--//

    public static abstract class PropertyTypeExtractor
    {
        private static class MapForProperties extends HashMap<String, TimeSeriesPropertyType>
        {
            public static final MapForProperties Empty = new MapForProperties();
        }

        private static class MapForClass extends HashMap<Class<? extends BaseObjectModel>, MapForProperties>
        {
            public static final MapForClass Empty = new MapForClass();
        }

        private static final Supplier<MapForClass> s_cachedClassification                     = Suppliers.memoize(() -> executeClassification(false));
        private static final Supplier<MapForClass> s_cachedClassificationWithPresentationType = Suppliers.memoize(() -> executeClassification(true));

        private static MapForClass executeClassification(boolean handlePresentationType)
        {
            List<PropertyTypeExtractor> extractors = Lists.newArrayList();

            Reflections                                 reflections = new Reflections("com.optio3.", new SubTypesScanner(false));
            Set<Class<? extends PropertyTypeExtractor>> targets     = reflections.getSubTypesOf(PropertyTypeExtractor.class);

            for (Class<? extends PropertyTypeExtractor> extractorClass : targets)
            {
                if (!Reflection.isAbstractClass(extractorClass))
                {
                    extractors.add(Reflection.newInstance(extractorClass));
                }
            }

            MapForClass classification = new MapForClass();

            Set<Class<? extends BaseObjectModel>> subTypes = reflections.getSubTypesOf(BaseObjectModel.class);
            for (Class<? extends BaseObjectModel> subType : subTypes)
            {
                if (!Reflection.isAbstractClass(subType))
                {
                    MapForProperties map = new MapForProperties();
                    BaseObjectModel  obj = Reflection.newInstance(subType);

                    for (PropertyTypeExtractor extractor : extractors)
                    {
                        extractor.classifyInstance(map, obj, handlePresentationType);
                    }

                    classification.put(subType, map);
                }
            }

            return classification;
        }

        //--//

        public TimeSeriesPropertyType locateProperty(BaseObjectModel obj,
                                                     boolean handlePresentationType,
                                                     String identifier)
        {
            identifier = obj.overrideIdentifier(identifier);

            Map<String, TimeSeriesPropertyType> map = classifyModel(obj, handlePresentationType);
            TimeSeriesPropertyType              pt  = map.get(identifier);

            if (pt != null)
            {
                pt.targetField = identifier;
            }

            return pt;
        }

        public abstract Map<String, TimeSeriesPropertyType> classifyRecord(DeviceElementRecord rec,
                                                                           boolean handlePresentationType);

        public Map<String, TimeSeriesPropertyType> classifyModel(BaseObjectModel obj,
                                                                 boolean handlePresentationType)
        {
            if (obj.overrideDescriptorsPerObject())
            {
                Map<String, TimeSeriesPropertyType> map = Maps.newHashMap();

                classifyInstance(map, obj, handlePresentationType);

                return map;
            }

            return classifyTemplate(obj.getClass(), handlePresentationType);
        }

        public static Map<String, TimeSeriesPropertyType> classifyTemplate(Class<? extends BaseObjectModel> clz,
                                                                           boolean handlePresentationType)
        {
            MapForClass cache = handlePresentationType ? s_cachedClassificationWithPresentationType.get() : s_cachedClassification.get();
            return cache.getOrDefault(clz, MapForProperties.Empty);
        }

        protected abstract void classifyInstance(Map<String, TimeSeriesPropertyType> map,
                                                 BaseObjectModel obj,
                                                 boolean handlePresentationType);

        public abstract IProtocolDecoder getProtocolDecoder();

        public abstract EngineeringUnitsFactors getUnitsFactors(DeviceElementRecord rec);

        public abstract String getIndexedValue(DeviceElementRecord rec);

        public abstract BaseObjectModel getContentsAsObject(DeviceElementRecord rec,
                                                            boolean desiredState) throws
                                                                                  IOException;

        public static TimeSeriesPropertyType lookupPropertyType(Map<String, TimeSeriesPropertyType> map,
                                                                String prop)
        {
            return map != null ? map.get(prop) : null;
        }

        public static Class<?> inferExpectedType(Map<String, TimeSeriesPropertyType> map,
                                                 String prop)
        {
            TimeSeriesPropertyType pt = lookupPropertyType(map, prop);
            return pt != null ? pt.getExpectedBoxedType() : null;
        }

        //--//

        public static class None extends PropertyTypeExtractor
        {
            @Override
            public Map<String, TimeSeriesPropertyType> classifyRecord(DeviceElementRecord rec,
                                                                      boolean handlePresentationType)
            {
                return Collections.emptyMap();
            }

            @Override
            protected void classifyInstance(Map<String, TimeSeriesPropertyType> map,
                                            BaseObjectModel obj,
                                            boolean handlePresentationType)
            {
                // Nothing to do, since we don't have a real class...
            }

            @Override
            public IProtocolDecoder getProtocolDecoder()
            {
                return null;
            }

            @Override
            public EngineeringUnitsFactors getUnitsFactors(DeviceElementRecord rec)
            {
                return null;
            }

            @Override
            public String getIndexedValue(DeviceElementRecord rec)
            {
                return null;
            }

            @Override
            public BaseObjectModel getContentsAsObject(DeviceElementRecord rec,
                                                       boolean desiredState)
            {
                return null;
            }
        }
    }

    //--//

    @Optio3ControlNotifications(reason = "No need to notify locations when an asset changes", direct = Notify.ON_ASSOCIATION_CHANGES, reverse = Notify.NEVER, getter = "getLocation")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "location", foreignKey = @ForeignKey(name = "LOCATION__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private LocationRecord location;

    @Field
    public String getLocationNameForSearch()
    {
        AssetIndexingHelper helper = HibernateSearch.IndexingContext.get(AssetIndexingHelper.class);
        if (helper != null)
        {
            String locationSysId = helper.locationLookup.get(getSysId());
            if (locationSysId != null)
            {
                return helper.nameLookup.get(locationSysId);
            }

            return null;
        }

        return location != null ? location.getLocationNameForSearch() : null;
    }

    //--//

    @Column(name = "name")
    @Field
    private String name;

    @Column(name = "asset_id")
    private String assetId;

    @Column(name = "serial_number")
    private String serialNumber;

    @Lob
    @Column(name = "customer_notes", length = 64 * 1024)
    private String customerNotes;

    @Column(name = "last_checked_date")
    private ZonedDateTime lastCheckedDate;

    @Column(name = "last_updated_date")
    private ZonedDateTime lastUpdatedDate;

    @Optio3UpgradeValue("operational")
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private AssetState state = AssetState.operational;

    @Transient
    private boolean stateChanged;

    @Column(name = "hidden")
    @Field(analyze = Analyze.NO)
    private boolean hidden;

    //--//

    @Optio3ControlNotifications(reason = "We want notifications to flow only from children to parent", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getParentAsset")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "parent", foreignKey = @ForeignKey(name = "ASSET__PARENT__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private AssetRecord parentAsset;

    @Field
    public String getParentNameForSearch()
    {
        AssetIndexingHelper helper = HibernateSearch.IndexingContext.get(AssetIndexingHelper.class);
        if (helper != null)
        {
            String parentSysId = helper.structuralParentLookup.get(getSysId());
            if (parentSysId != null)
            {
                return helper.nameLookup.get(parentSysId);
            }

            return null;
        }

        return parentAsset != null ? parentAsset.getName() : null;
    }

    @Field
    public String getParentLocationNameForSearch()
    {
        AssetIndexingHelper helper = HibernateSearch.IndexingContext.get(AssetIndexingHelper.class);
        if (helper != null)
        {
            String parentSysId = helper.structuralParentLookup.get(getSysId());
            if (parentSysId != null)
            {
                String locationSysId = helper.locationLookup.get(parentSysId);
                if (locationSysId != null)
                {
                    return helper.nameLookup.get(locationSysId);
                }
            }

            return null;
        }

        return parentAsset != null ? parentAsset.getLocationNameForSearch() : null;
    }

    //--//

    @OneToMany(mappedBy = "asset", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    @OrderBy("sys_created_on DESC")
    private List<EventRecord> events;

    // This field is only used to create a Metamodel attribute to perform Left Outer Joins against Relationships.
    @Optio3ControlNotifications(reason = "This is just a marker to be able create left joins", direct = Notify.IGNORE, reverse = Notify.IGNORE, markerForLeftJoin = true)
    @OneToMany(mappedBy = "parentAsset", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<RelationshipRecord> relationsAsParent;

    // This field is only used to create a Metamodel attribute to perform Left Outer Joins against Relationships.
    @Optio3ControlNotifications(reason = "This is just a marker to be able create left joins", direct = Notify.IGNORE, reverse = Notify.IGNORE, markerForLeftJoin = true)
    @OneToMany(mappedBy = "childAsset", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<RelationshipRecord> relationsAsChild;

    //--//

    public LocationRecord getLocation()
    {
        return location;
    }

    public boolean setLocation(LocationRecord location)
    {
        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        if (SessionHolder.sameEntity(this.location, location))
        {
            return false; // Nothing changed.
        }

        this.location = location;
        return true;
    }

    public void propagateLocationChangeToChildren(Logger logger,
                                                  RecordHelper<? extends AssetRecord> helper,
                                                  LocationRecord rec_oldLocation,
                                                  LocationRecord rec_newLocation) throws
                                                                                  Exception
    {
        enumerateChildren(helper, true, -1, null, (rec_child) ->
        {
            LocationRecord rec_childLocation = rec_child.getLocation();
            if (rec_childLocation == null || SessionHolder.sameEntity(rec_childLocation, rec_oldLocation))
            {
                if (logger != null)
                {
                    logger.info("Assigned location '%s' to '%s' (%s)", rec_newLocation.getName(), rec_child.getName(), rec_child.getSysId());
                }

                rec_child.setLocation(rec_newLocation);

                rec_child.propagateLocationChangeToChildren(logger, helper, rec_oldLocation, rec_newLocation);

                return StreamHelperNextAction.Continue_Flush_Evict;
            }
            else
            {
                return StreamHelperNextAction.Continue_Evict;
            }
        });
    }

    //--//

    public AssetRecord getParentAsset()
    {
        return parentAsset;
    }

    public boolean linkToParent(RecordHelper<? extends AssetRecord> helper,
                                AssetRecord parent)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(helper.currentSessionHolder(), null, true))
        {
            return linkToParent(helper, parent, validation);
        }
    }

    public boolean linkToParent(RecordHelper<? extends AssetRecord> helper,
                                AssetRecord parent,
                                ValidationResultsHolder validation)
    {
        if (SessionHolder.sameEntity(parent, parentAsset))
        {
            return false; // Nothing changed.
        }

        for (AssetRecord rec = parent; rec != null; rec = rec.parentAsset)
        {
            if (SessionHolder.sameEntity(rec, this))
            {
                validation.addFailure("parent", "Can't set '%s' as parent, it would create a loop", parent.getSysId());
                return false;
            }
        }

        if (validation.canProceed())
        {
            LocationRecord ourLocation = location;

            // If the parent location was the same as ours, assume inherited.
            if (parentAsset != null && SessionHolder.sameEntity(parentAsset.getLocation(), ourLocation))
            {
                ourLocation = null;
            }

            if (parentAsset != null)
            {
                RelationshipRecord.removeRelation(helper.currentSessionHolder(), parentAsset, this, AssetRelationship.structural);
            }

            parentAsset = parent;
            RelationshipRecord.addRelation(helper.currentSessionHolder(), parent, this, AssetRelationship.structural);

            if (parent != null && ourLocation == null)
            {
                LocationRecord parentLocation = parent.getLocation();
                if (parentLocation != null)
                {
                    location = parentLocation;

                    try
                    {
                        propagateLocationChangeToChildren(null, helper, null, parentLocation);
                    }
                    catch (Exception e)
                    {
                        validation.addFailure("location", "Failed due to %s", e.getMessage());
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public boolean unlinkFromParent(RecordHelper<? extends AssetRecord> helper)
    {
        if (parentAsset == null)
        {
            return false; // Nothing changed.
        }

        if (location == null)
        {
            // Inherited location, copy it.
            location = parentAsset.location;
        }

        RelationshipRecord.removeRelation(helper.currentSessionHolder(), parentAsset, this, AssetRelationship.structural);

        parentAsset = null;

        return true;
    }

    //--//

    public String getName()
    {
        return name;
    }

    public String getPhysicalName()
    {
        return getMetadata(WellKnownMetadata.physicalName);
    }

    public boolean setPhysicalName(String val)
    {
        return updateName(WellKnownMetadata.physicalName, val);
    }

    public String getLogicalName()
    {
        return getMetadata(WellKnownMetadata.nameFromLegacyImport);
    }

    public boolean setLogicalName(String val)
    {
        return updateName(WellKnownMetadata.nameFromLegacyImport, val);
    }

    public String getNormalizedName()
    {
        return getMetadata(WellKnownMetadata.normalizedName);
    }

    public boolean setNormalizedName(String val)
    {
        return updateName(WellKnownMetadata.normalizedName, val);
    }

    public String getDisplayName()
    {
        return getMetadata(WellKnownMetadata.displayName);
    }

    public boolean setDisplayName(String val)
    {
        return updateName(WellKnownMetadata.displayName, val);
    }
    
    public boolean setAssignedUnits(EngineeringUnits e)
    {
    	 return modifyMetadata(map -> 
    	 {
    		 DeviceElementClassificationMetadata classification = DeviceElementClassificationMetadata.fromMetadata(map);
    		 classification.assignedUnits = e;
    		 classification.saveToMetadata(map);
         });
    }

    private boolean updateName(MetadataField<String> key,
                               String val)
    {
        return modifyMetadata(map ->
                              {
                                  key.put(map, val);

                                  String name = WellKnownMetadata.displayName.get(map);

                                  if (StringUtils.isEmpty(name))
                                  {
                                      name = WellKnownMetadata.normalizedName.get(map);
                                  }

                                  if (StringUtils.isEmpty(name))
                                  {
                                      name = WellKnownMetadata.physicalName.get(map);
                                  }

                                  if (!StringUtils.isEmpty(name))
                                  {
                                      setNameInner(name);
                                  }
                              });
    }

    private void setNameInner(String name)
    {
        if (!StringUtils.equals(this.name, name))
        {
            this.name = name;
        }
    }

    //--//

    public AssetState getState()
    {
        return state;
    }

    public boolean setState(AssetState state)
    {
        if (state == null)
        {
            state = AssetState.operational;
        }

        if (this.state == state)
        {
            return false; // Nothing changed.
        }

        this.state        = state;
        this.stateChanged = true;
        return true;
    }

    @JsonIgnore
    public boolean hasStateChanged()
    {
        return stateChanged;
    }

    public boolean inStateRecursive(AssetState state)
    {
        for (AssetRecord rec = this; rec != null; rec = rec.getParentAsset())
        {
            if (rec.state == state)
            {
                return true;
            }
        }

        return false;
    }

    public String getAssetId()
    {
        return assetId;
    }

    public void setAssetId(String assetId)
    {
        this.assetId = assetId;
    }

    public String getSerialNumber()
    {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber)
    {
        this.serialNumber = serialNumber;
    }

    public String getCustomerNotes()
    {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes)
    {
        this.customerNotes = customerNotes;
    }

    public ZonedDateTime getLastCheckedDate()
    {
        return lastCheckedDate;
    }

    public void setLastCheckedDate(ZonedDateTime lastCheckedDate)
    {
        this.lastCheckedDate = lastCheckedDate;
    }

    public ZonedDateTime getLastUpdatedDate()
    {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(ZonedDateTime lastUpdatedDate)
    {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    @Field(analyze = Analyze.NO)
    public boolean isHidden()
    {
        return hidden;
    }

    public void setHidden(boolean hidden)
    {
        this.hidden = hidden;
    }

    public String getPreferredTimeZone(IServiceProvider serviceProvider)
    {
        LocationRecord rec_loc = getLocation();
        if (rec_loc == null)
        {
            return null;
        }

        LocationsEngine.Snapshot snapshot = serviceProvider.getServiceNonNull(LocationsEngine.class)
                                                           .acquireSnapshot(false);

        return snapshot.getTimeZone(rec_loc.getSysId());
    }

    //--//

    public String getPointClassId()
    {
        MetadataTagsMap tags   = accessTags();
        Set<String>     values = tags.getValuesForTag(WellKnownTags.pointClassId);
        return CollectionUtils.firstElement(values);
    }

    public boolean setPointClassId(String pointClassId)
    {
        return modifyMetadata(map ->
                              {
                                  DeviceElementClassificationMetadata classification = DeviceElementClassificationMetadata.fromMetadata(map);
                                  classification.pointClassId        = pointClassId;
                                  classification.positiveScore       = 0.0;
                                  classification.negativeScore       = 0.0;
                                  classification.pointClassThreshold = 0.0;
                                  
                                  classification.saveToMetadata(map);
                              });
    }
    
    
    

    public WellKnownPointClass getWellKnownPointClass()
    {
        return WellKnownPointClass.parse(getPointClassId());
    }

    public String getEquipmentKey()
    {
        return getMetadata(WellKnownMetadata.equipmentKey);
    }

    public String getEquipmentClassId()
    {
        MetadataTagsMap tags   = accessTags();
        Set<String>     values = tags.getValuesForTag(WellKnownTags.equipmentClassId);
        return CollectionUtils.firstElement(values);
    }

    public boolean setEquipmentClassId(String equipmentClassId)
    {
        return modifyMetadata(map ->
                              {
                                  EquipmentClassificationMetadata classification = EquipmentClassificationMetadata.fromMetadata(map);
                                  classification.equipmentClassId = equipmentClassId;
                                  classification.saveToMetadata(map);
                              });
    }

    public WellKnownEquipmentClass getWellKnownEquipmentClass()
    {
        return WellKnownEquipmentClass.parse(getEquipmentClassId());
    }

    public String getAzureDigitalTwinModel()
    {
        return getMetadata(WellKnownMetadata.azureDigitalTwinModel);
    }

    public boolean setAzureDigitalTwinModel(String model)
    {
        return putMetadata(WellKnownMetadata.azureDigitalTwinModel, model);
    }

    @Field(analyze = Analyze.NO)
    public boolean isClassifiedForSearch()
    {
        MetadataTagsMap tags = accessTags();
        return tags.hasTag(WellKnownTags.equipmentClassId);
    }

    public boolean isEquipment()
    {
        MetadataTagsMap tags = accessTags();
        return tags.hasTag(WellKnownTags.isEquipment);
    }

    //--//

    public List<EventRecord> getEvents()
    {
        return CollectionUtils.asEmptyCollectionIfNull(events);
    }

    public List<AlertRecord> getAlerts()
    {
        return getEventsOfType(AlertRecord.class);
    }

    public List<WorkflowRecord> getActiveWorkflows()
    {
        return CollectionUtils.filter(getEventsOfType(WorkflowRecord.class), (workflow) -> workflow.getStatus() == WorkflowStatus.Active);
    }

    private <T extends EventRecord> List<T> getEventsOfType(Class<T> clz)
    {
        return CollectionUtils.transformToListNoNulls(getEvents(), (event) -> Reflection.as(event, clz));
    }

    public AlertRecord prepareNewAlert(SessionHolder sessionHolder,
                                       AlertDefinitionVersionRecord rec_def,
                                       ZonedDateTime timestamp,
                                       AlertType alertType,
                                       AlertEventLevel alertLevel)
    {
        RecordHelper<AlertRecord> helper_alert = sessionHolder.createHelper(AlertRecord.class);
        AlertRecord               rec_alert    = AlertRecord.newInstance(helper_alert, null, rec_def, this, alertType);

        if (timestamp != null)
        {
            rec_alert.setCreatedOn(timestamp);
            rec_alert.setUpdatedOn(timestamp);
        }

        return rec_alert;
    }

    public AlertHistoryRecord createNewAlert(SessionHolder sessionHolder,
                                             AlertDefinitionVersionRecord rec_def,
                                             ZonedDateTime timestamp,
                                             AlertType alertType,
                                             AlertEventLevel alertLevel,
                                             String fmt,
                                             Object... args)
    {
        AlertRecord rec_alert = prepareNewAlert(sessionHolder, rec_def, timestamp, alertType, alertLevel);

        sessionHolder.persistEntity(rec_alert);

        return rec_alert.addHistoryEntry(sessionHolder, timestamp, alertLevel, AlertEventType.created, fmt, args);
    }

    //--//

    public <T extends BaseAssetDescriptor> T getIdentityDescriptor(Class<T> clz)
    {
        return Reflection.as(getIdentityDescriptor(), clz);
    }

    public BaseAssetDescriptor getIdentityDescriptor()
    {
        return null; // Subclasses override method.
    }

    //--//

    public <T extends AssetRecord> T getParentAsset(Class<T> clz)
    {
        return SessionHolder.asEntityOfClass(getParentAsset(), clz);
    }

    public <T extends AssetRecord> T getParentAssetOrNull(Class<T> clz)
    {
        return SessionHolder.asEntityOfClassOrNull(getParentAsset(), clz);
    }

    public <T extends AssetRecord> T findParentAssetRecursively(Class<T> clz)
    {
        return findParentAssetRecursively(this, clz);
    }

    public static <T extends AssetRecord> T findParentAssetRecursively(AssetRecord rec,
                                                                       Class<T> clz)
    {
        while (rec != null)
        {
            T rec2 = SessionHolder.asEntityOfClassOrNull(rec, clz);
            if (rec2 != null)
            {
                return rec2;
            }

            rec = rec.getParentAsset();
        }

        return null;
    }

    //--//

    public abstract void assetPostCreate(SessionHolder sessionHolder);

    public final void assetPostUpdate(SessionHolder sessionHolder) throws
                                                                   Exception
    {
        if (hasStateChanged())
        {
            reconfigureSampling(sessionHolder);
        }

        assetPostUpdateInner(sessionHolder);
    }

    protected abstract void assetPostUpdateInner(SessionHolder sessionHolder) throws
                                                                              Exception;

    //--//

    protected static class AssetJoinHelper<T, C extends AssetRecord> extends QueryHelperWithCommonFields<T, C>
    {
        From<C, LocationRecord> rootLocation;

        protected AssetJoinHelper(RecordHelper<C> helper,
                                  Class<T> clz)
        {
            super(helper, clz);
        }

        //--//

        From<C, LocationRecord> joinWithLocation()
        {
            if (rootLocation == null)
            {
                rootLocation = root.join(AssetRecord_.location);
            }

            return rootLocation;
        }

        //--//

        protected void applyFilters(AssetFilterRequest filters)
        {
            filterBySysIds(filters);

            filterByParents(filters);

            filterByChildren(filters.childrenIDs, filters.childrenRelations);

            filterByStates(filters.stateIDs);

            filterByLocations(filters.locationIDs, filters.locationInclusive, filters.locationMissing);

            if (filters.hasNoMetadata)
            {
                addWhereClause(isNull(root, AssetRecord_.metadataCompressed));
            }
            else if (filters.hasMetadata)
            {
                addWhereClause(isNotNull(root, AssetRecord_.metadataCompressed));
            }

            if (filters.isNotHidden)
            {
                addWhereClause(equal(root, AssetRecord_.hidden, false));
            }
            else if (filters.isHidden)
            {
                addWhereClause(equal(root, AssetRecord_.hidden, true));
            }

            //--//

            if (filters.sortBy != null)
            {
                for (SortCriteria sort : filters.sortBy)
                {
                    switch (sort.column)
                    {
                        case "location":
                            //
                            // Because locations are hierarchical, trying to sort by location at the SQL level is too expensive.
                            // So we don't sort, but collect the location information.
                            // After we run the query, we sort the results in memory.
                            //
                            addSortExtension(new SortExtension<String>()
                            {
                                LocationsEngine engine = helper.getService(LocationsEngine.class);
                                LocationsEngine.Snapshot snapshot = engine.acquireSnapshot(false);

                                private Multimap<String, RecordIdentity> m_map = ArrayListMultimap.create();

                                @Override
                                public Path<String> getPath()
                                {
                                    return root.get(AssetRecord_.location)
                                               .get(RecordWithCommonFields_.sysId);
                                }

                                @Override
                                public void processValue(RecordIdentity ri,
                                                         String sysId_location)
                                {
                                    m_map.put(sysId_location, ri);
                                }

                                @Override
                                public void processResults(List<RecordIdentity> results)
                                {
                                    boolean ascending = sort.ascending;

                                    results.clear();

                                    List<String> locations = Lists.newArrayList(m_map.keySet());
                                    locations.sort((l, r) ->
                                                   {
                                                       String lName = BoxingUtils.get(snapshot.getHierarchicalName(l), "");
                                                       String rName = BoxingUtils.get(snapshot.getHierarchicalName(r), "");
                                                       int    diff  = lName.compareToIgnoreCase(rName);

                                                       return ascending ? diff : -diff;
                                                   });

                                    for (String sysId_location : locations)
                                    {
                                        results.addAll(m_map.get(sysId_location));
                                    }
                                }
                            });
                            break;

                        case "equipment":
                        {
                            addSortExtension(new SortExtension<String>()
                            {
                                private TagsEngine.Snapshot m_snapshot = helper.getService(TagsEngine.class)
                                                                               .acquireSnapshot(false);

                                private Multimap<String, RecordIdentity> m_map = ArrayListMultimap.create();

                                @Override
                                public Path<String> getPath()
                                {
                                    return root.get(RecordWithCommonFields_.sysId);
                                }

                                @Override
                                public void processValue(RecordIdentity ri,
                                                         String sysId)
                                {
                                    TagsEngine.Snapshot.AssetSet set = m_snapshot.resolveRelations(sysId, AssetRelationship.controls, true);
                                    TagsStreamNextAction action = set.streamResolved((parent) ->
                                                                                     {
                                                                                         m_map.put(parent.sysId, ri);
                                                                                         return TagsStreamNextAction.Stop;
                                                                                     });
                                    if (action != TagsStreamNextAction.Stop)
                                    {
                                        // Account for null equipment
                                        m_map.put(null, ri);
                                    }
                                }

                                @Override
                                public void processResults(List<RecordIdentity> results)
                                {
                                    boolean ascending = sort.ascending;

                                    results.clear();

                                    Map<String, String> nameLookup = Maps.newHashMap();

                                    RawQueryHelper<LogicalAssetRecord, LogicalAsset> qh = new RawQueryHelper<>(helper.currentSessionHolder(), LogicalAssetRecord.class);

                                    qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
                                    qh.addString(AssetRecord_.name, (obj, val) -> obj.name = val);

                                    // Reuse the same instance, since we don't store the individual models.
                                    final var singletonModel = new LogicalAsset();

                                    qh.stream(() -> singletonModel, (model) ->
                                    {
                                        if (m_map.containsKey(model.sysId))
                                        {
                                            nameLookup.put(model.sysId, model.name != null ? model.name : "");
                                        }
                                    });

                                    List<String> equipments = Lists.newArrayList(m_map.keySet());
                                    equipments.sort((l, r) ->
                                                    {
                                                        String lName = l != null ? nameLookup.get(l) : "";
                                                        String rName = r != null ? nameLookup.get(r) : "";
                                                        int    diff  = lName.compareToIgnoreCase(rName);

                                                        return ascending ? diff : -diff;
                                                    });

                                    for (String sysId : equipments)
                                    {
                                        results.addAll(m_map.get(sysId));
                                    }
                                }
                            });
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

                        case "lastCheckedDate":
                        {
                            addOrderBy(root, AssetRecord_.lastCheckedDate, sort.ascending);
                            break;
                        }

                        case "lastUpdatedDate":
                        {
                            addOrderBy(root, AssetRecord_.lastUpdatedDate, sort.ascending);
                            break;
                        }

                        case "name":
                        {
                            addOrderBy(root, AssetRecord_.name, sort.ascending);
                            break;
                        }

                        case "physicalName":
                        {
                            addMetadataStringSortExtension(WellKnownMetadata.physicalName, sort.ascending);
                            break;
                        }

                        case "logicalName":
                        {
                            addMetadataStringSortExtension(WellKnownMetadata.nameFromLegacyImport, sort.ascending);
                            break;
                        }

                        case "displayName":
                        {
                            addMetadataStringSortExtension(WellKnownMetadata.displayName, sort.ascending);
                            break;
                        }

                        case "normalizedName":
                        {
                            addMetadataStringSortExtension(WellKnownMetadata.normalizedName, sort.ascending);
                            break;
                        }

                        case "state":
                        {
                            addOrderBy(root, AssetRecord_.state, sort.ascending);
                            break;
                        }

                        case "equipmentClassId":
                        {
                            try
                            {
                                TagsEngine         tagsEngine = helper.getService(TagsEngine.class);
                                NormalizationRules rules      = tagsEngine.getActiveNormalizationRules(helper.currentSessionHolder());
                                if (rules != null)
                                {
                                    addSortExtension(new MetadataSortExtension<EquipmentClass>(root)
                                    {
                                        @Override
                                        protected EquipmentClass extractValue(MetadataMap metadata)
                                        {
                                            String equipClassId = WellKnownMetadata.equipmentClassID.get(metadata);
                                            return rules.findEquipmentClass(equipClassId);
                                        }

                                        @Override
                                        protected void sort(List<EquipmentClass> values)
                                        {
                                            values.sort((a, b) -> EquipmentClass.compare(a, b, sort.ascending));
                                        }
                                    });
                                }
                            }
                            catch (Exception ex)
                            {
                                // Ignore failures.
                            }

                            break;
                        }

                        case "parentEquipmentClassId":
                        {
                            try
                            {
                                TagsEngine         tagsEngine = helper.getService(TagsEngine.class);
                                NormalizationRules rules      = tagsEngine.getActiveNormalizationRules(helper.currentSessionHolder());
                                if (rules != null)
                                {
                                    addSortExtension(new SortExtension<String>()
                                    {
                                        private TagsEngine.Snapshot m_snapshot = helper.getService(TagsEngine.class)
                                                                                       .acquireSnapshot(false);
                                        private EquipmentLookup m_lookup = new EquipmentLookup(m_snapshot);

                                        private Multimap<String, RecordIdentity> m_map = ArrayListMultimap.create();

                                        @Override
                                        public Path<String> getPath()
                                        {
                                            return root.get(RecordWithCommonFields_.sysId);
                                        }

                                        @Override
                                        public void processValue(RecordIdentity ri,
                                                                 String sysId)
                                        {
                                            TagsEngine.Snapshot.AssetSet set = m_snapshot.resolveRelations(sysId, AssetRelationship.controls, true);
                                            TagsStreamNextAction action = set.streamResolved((parent) ->
                                                                                             {
                                                                                                 m_map.put(m_lookup.equipments.get(parent.sysId), ri);
                                                                                                 return TagsStreamNextAction.Stop;
                                                                                             });
                                            if (action != TagsStreamNextAction.Stop)
                                            {
                                                // Account for null equipment
                                                m_map.put(null, ri);
                                            }
                                        }

                                        @Override
                                        public void processResults(List<RecordIdentity> results)
                                        {
                                            boolean ascending = sort.ascending;

                                            results.clear();

                                            Map<String, String> equipClassLookup = Maps.newHashMap();
                                            for (EquipmentClass equipClass : rules.equipmentClasses)
                                            {
                                                equipClassLookup.put(equipClass.idAsString(), equipClass.equipClassName);
                                            }

                                            List<String> equipments = Lists.newArrayList(m_map.keySet());
                                            equipments.sort((l, r) ->
                                                            {
                                                                String lName = l != null ? equipClassLookup.get(l) : "";
                                                                String rName = r != null ? equipClassLookup.get(r) : "";
                                                                int    diff  = lName.compareToIgnoreCase(rName);

                                                                return ascending ? diff : -diff;
                                                            });

                                            for (String sysId : equipments)
                                            {
                                                results.addAll(m_map.get(sysId));
                                            }
                                        }
                                    });
                                }
                            }
                            catch (Exception ex)
                            {
                                // Ignore failures.
                            }

                            break;
                        }

                        case "pointClassId":
                        {
                            try
                            {
                                TagsEngine         tagsEngine = helper.getService(TagsEngine.class);
                                NormalizationRules rules      = tagsEngine.getActiveNormalizationRules(helper.currentSessionHolder());
                                if (rules != null)
                                {
                                    addSortExtension(new MetadataSortExtension<PointClass>(root)
                                    {
                                        @Override
                                        protected PointClass extractValue(MetadataMap metadata)
                                        {
                                            MetadataTagsMap tags         = accessTags(metadata);
                                            Set<String>     values       = tags.getValuesForTag(WellKnownTags.pointClassId);
                                            String          pointClassId = CollectionUtils.firstElement(values);
                                            return CollectionUtils.findFirst(rules.pointClasses, (pc) -> StringUtils.equals(pc.idAsString(), pointClassId));
                                        }

                                        @Override
                                        protected void sort(List<PointClass> values)
                                        {
                                            values.sort((a, b) -> PointClass.compare(a, b, sort.ascending));
                                        }
                                    });
                                }
                            }
                            catch (Throwable t)
                            {
                                // Ignore failures.
                            }

                            break;
                        }
                    }
                }
            }

            //--//

            filterTimestampsCoveredByTargetRange(root, AssetRecord_.lastCheckedDate, filters.discoveryRangeStart, filters.discoveryRangeEnd);

            if (filters.tagsQuery != null)
            {
                TagsEngine                   tagsEngine      = helper.getService(TagsEngine.class);
                TagsEngine.Snapshot          tagsSnapshot    = tagsEngine.acquireSnapshot(false);
                TagsEngine.Snapshot.AssetSet tagsQueryResult = tagsSnapshot.evaluateCondition(filters.tagsQuery);

                addWhereClauseIn(root, RecordWithCommonFields_.sysId, CollectionUtils.transformToListNoNulls(tagsQueryResult.resolve(), (ri) -> ri.sysId));
            }
        }

        private void addMetadataStringSortExtension(MetadataField<String> field,
                                                    boolean ascending)
        {
            addSortExtension(new MetadataSortExtension<String>(root)
            {
                @Override
                protected String extractValue(MetadataMap metadata)
                {
                    return field.get(metadata);
                }

                @Override
                protected void sort(List<String> values)
                {
                    values.sort((a, b) ->
                                {
                                    int diff = StringUtils.compareIgnoreCase(a, b);
                                    return ascending ? diff : -diff;
                                });
                }
            });
        }

        protected void filterBySysIds(AssetFilterRequest filters)
        {
            if (CollectionUtils.isNotEmpty(filters.sysIds))
            {
                addWhereClauseIn(root, RecordWithCommonFields_.sysId, filters.sysIds);
            }
        }

        protected void filterByParents(AssetFilterRequest filters)
        {
            List<String>            parentIDs       = CollectionUtils.asEmptyCollectionIfNull(filters.parentIDs);
            List<AssetRelationship> parentRelations = CollectionUtils.asEmptyCollectionIfNull(filters.parentRelations);
            List<ParsedLike>        likeFilters     = ParsedLike.decode(filters.likeFilter);
            boolean                 addLike         = false;

            if (filters.parentTagsQuery != null)
            {
                TagsEngine                   tagsEngine      = helper.getService(TagsEngine.class);
                TagsEngine.Snapshot          tagsSnapshot    = tagsEngine.acquireSnapshot(false);
                TagsEngine.Snapshot.AssetSet tagsQueryResult = tagsSnapshot.evaluateCondition(filters.parentTagsQuery);
                parentIDs = CollectionUtils.transformToListNoNulls(tagsQueryResult.resolve(), (ri) -> ri.sysId);
            }

            if (parentIDs.isEmpty())
            {
                subfilterByNoParents(filters);
            }
            else if (parentIDs.size() == 1 && parentRelations.size() == 1 && parentRelations.contains(AssetRelationship.structural))
            {
                Path<AssetRecord> referencedEntity = root.get(AssetRecord_.parentAsset);

                addWhereClauseWithEqual(referencedEntity, RecordWithCommonFields_.sysId, parentIDs.get(0));

                addLike = true;
            }
            else
            {
                Subquery<String>         subQuery = cq.subquery(String.class);
                Root<RelationshipRecord> group    = subQuery.from(RelationshipRecord.class);

                subQuery.select(group.get(RelationshipRecord_.childAsset)
                                     .get(AssetRecord_.sysId));

                Predicate p1 = group.get(RelationshipRecord_.parentAsset)
                                    .get(AssetRecord_.sysId)
                                    .in(parentIDs);

                if (parentRelations.isEmpty())
                {
                    subQuery.where(p1);
                }
                else
                {
                    final Predicate[] orPredicates = new Predicate[parentRelations.size()];
                    for (int i = 0; i < orPredicates.length; i++)
                    {
                        orPredicates[i] = cb.equal(group.get(RelationshipRecord_.relation), parentRelations.get(i));
                    }

                    subQuery.where(p1, or(orPredicates));
                }

                subQuery.distinct(true);

                addWhereClause(root.in(subQuery));

                addLike = true;
            }

            if (filters.forceLike())
            {
                addLike = true;
            }

            if (filters.tagsQuery != null)
            {
                addLike = true;
            }

            if (CollectionUtils.isNotEmpty(filters.sysIds))
            {
                addLike = true;
            }

            if (addLike && likeFilters != null)
            {
                addWhereClause(predicateForLike(likeFilters));
            }
        }

        protected void subfilterByNoParents(AssetFilterRequest filters)
        {
            // Nothing to do.
        }

        protected void filterByChildren(List<String> childrenIDs,
                                        List<AssetRelationship> childrenRelations)
        {
            if (childrenIDs != null && !childrenIDs.isEmpty())
            {
                Subquery<String>         subQuery = cq.subquery(String.class);
                Root<RelationshipRecord> group    = subQuery.from(RelationshipRecord.class);

                subQuery.select(group.get(RelationshipRecord_.parentAsset)
                                     .get(AssetRecord_.sysId));

                if (childrenRelations == null || childrenRelations.size() == 0)
                {
                    childrenRelations = Lists.newArrayList(AssetRelationship.structural, AssetRelationship.controls);
                }

                final Predicate[] orPredicates = new Predicate[childrenRelations.size()];
                for (int i = 0; i < orPredicates.length; i++)
                {
                    orPredicates[i] = cb.equal(group.get(RelationshipRecord_.relation), childrenRelations.get(i));
                }

                subQuery.where(group.get(RelationshipRecord_.childAsset)
                                    .get(AssetRecord_.sysId)
                                    .in(childrenIDs), or(orPredicates));

                subQuery.distinct(true);

                addWhereClause(root.in(subQuery));
            }
        }

        protected Predicate predicateForLike(List<ParsedLike> likeFilters)
        {
            return predicateForLike(root, AssetRecord_.name, likeFilters);
        }

        protected void filterByStates(List<AssetState> stateIDs)
        {
            if (stateIDs != null && !stateIDs.isEmpty())
            {
                addWhereClauseIn(root, AssetRecord_.state, stateIDs);
            }
        }

        protected void filterByLocations(List<String> locationIDs,
                                         boolean recursive,
                                         boolean locationMissing)
        {
            if (locationIDs != null && !locationIDs.isEmpty())
            {
                Set<String> recursiveLocationIDs = Sets.newHashSet();

                LocationsEngine          locationsEngine   = helper.getServiceNonNull(LocationsEngine.class);
                LocationsEngine.Snapshot locationsSnapshot = locationsEngine.acquireSnapshot(false);

                for (String locationId : locationIDs)
                {
                    locationsSnapshot.recursivelyCollectStructure(recursiveLocationIDs, recursive, locationId);
                }

                if (recursiveLocationIDs.size() > 0)
                {
                    addWhereReferencingSysIds(root, DeviceRecord_.location, recursiveLocationIDs);
                }
            }
            else if (locationMissing)
            {
                addWhereClause(root.get(AssetRecord_.location)
                                   .isNull());
            }
        }
    }

    //--//

    public static List<AssetRecord> getAssetsBatch(RecordHelper<AssetRecord> helper,
                                                   List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public static Map<String, Number> countAssetsByLocation(RecordHelper<AssetRecord> helper,
                                                            AssetFilterRequest filters)
    {
        AssetJoinHelper<Tuple, AssetRecord> jh = new AssetJoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            filters.sortBy = null; // Just in case, sanitize input.

            jh.applyFilters(filters);
        }

        return jh.countByField(jh.joinWithLocation(), RecordWithCommonFields_.sysId);
    }

    public static List<RecordIdentity> filterAssets(RecordHelper<AssetRecord> helper,
                                                    AssetFilterRequest filters)
    {
        AssetJoinHelper<Tuple, AssetRecord> jh = new AssetJoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return AssetJoinHelper.returnFilterTuples(helper, jh);
    }

    public static long countAssets(RecordHelper<AssetRecord> helper,
                                   AssetFilterRequest filters)
    {
        AssetJoinHelper<Tuple, AssetRecord> jh = new AssetJoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return jh.count();
    }

    //--//

    public static <T extends AssetRecord> StreamHelperResult enumerateNoNesting(RecordHelper<T> helper,
                                                                                int maxResults,
                                                                                AssetFilterRequest filters,
                                                                                FunctionWithException<T, StreamHelperNextAction> callback) throws
                                                                                                                                           Exception
    {
        AssetJoinHelper<T, T> jh = new AssetJoinHelper<>(helper, helper.getEntityClass());

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return QueryHelperWithCommonFields.streamNoNesting(maxResults, jh, callback);
    }

    public static <T extends AssetRecord> StreamHelperResult enumerate(RecordHelper<T> helper,
                                                                       boolean batchStream,
                                                                       int maxResults,
                                                                       AssetFilterRequest filters,
                                                                       FunctionWithException<T, StreamHelperNextAction> callback) throws
                                                                                                                                  Exception
    {
        AssetJoinHelper<Tuple, T> jh = new AssetJoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return QueryHelperWithCommonFields.stream(batchStream, maxResults, jh, callback);
    }

    //--//

    public <T extends AssetRecord> StreamHelperResult enumerateChildrenNoNesting(RecordHelper<T> helper,
                                                                                 int maxResults,
                                                                                 ConsumerWithException<AssetFilterRequest> filterCallback,
                                                                                 FunctionWithException<T, StreamHelperNextAction> recordCallback) throws
                                                                                                                                                  Exception
    {
        AssetFilterRequest filters = AssetFilterRequest.createFilterForParent(getSysId());

        if (filterCallback != null)
        {
            filterCallback.accept(filters);
        }

        return enumerateNoNesting(helper, maxResults, filters, recordCallback);
    }

    public <T extends AssetRecord> StreamHelperResult enumerateChildren(RecordHelper<T> helper,
                                                                        boolean batchStream,
                                                                        int maxResults,
                                                                        ConsumerWithException<AssetFilterRequest> filterCallback,
                                                                        FunctionWithException<T, StreamHelperNextAction> recordCallback) throws
                                                                                                                                         Exception
    {
        AssetFilterRequest filters = AssetFilterRequest.createFilterForParent(getSysId());

        if (filterCallback != null)
        {
            filterCallback.accept(filters);
        }

        return enumerate(helper, batchStream, maxResults, filters, recordCallback);
    }

    //--//

    public void reconfigureSampling(SessionHolder sessionHolder) throws
                                                                 Exception
    {
        NetworkAssetRecord rec_network = findParentAssetRecursively(NetworkAssetRecord.class);
        if (rec_network != null)
        {
            InstanceConfiguration cfg = sessionHolder.getService(InstanceConfiguration.class);
            if (cfg != null)
            {
                cfg.handleSamplingReconfiguration(sessionHolder, rec_network);
            }
        }
    }

    //--//

    public static MetadataTagsMap accessTags(MetadataMap metadata)
    {
        return metadata.getTags(WellKnownMetadata.tags);
    }

    public MetadataTagsMap accessTags()
    {
        return accessTags(getMetadata());
    }

    public <T> T modifyTags(Function<MetadataTagsMap, T> callback)
    {
        return modifyMetadata((metadata) ->
                              {
                                  return metadata.modifyTags(WellKnownMetadata.tags, callback);
                              });
    }

    public boolean modifyTags(Consumer<MetadataTagsMap> callback)
    {
        return modifyMetadata((metadata) ->
                              {
                                  metadata.modifyTags(WellKnownMetadata.tags, callback);
                              });
    }

    public void assignTags(Collection<String> tags,
                           boolean forSystem,
                           boolean forClassification,
                           boolean forManual)
    {
        modifyTags((tagsMap) ->
                   {
                       WellKnownTags.assignTags(tagsMap, forSystem, forClassification, forManual, tags);
                   });
    }

    public Set<String> getClassificationTags()
    {
        return WellKnownTags.getTags(accessTags(), false, true, false);
    }

    public Set<String> getManualTags()
    {
        Set<String> res = Sets.newHashSet();

        for (var tag : WellKnownTags.getTags(accessTags(), false, false, true))
        {
            res.add(WellKnownTags.decodeManualTag(tag));
        }

        return res;
    }

    public void setManualTags(Set<String> tags)
    {
        Set<String> tagsWithPrefix = Sets.newHashSet();

        if (tags != null)
        {
            for (var tag : tags)
            {
                tagsWithPrefix.add(WellKnownTags.encodeManualTag(tag));
            }
        }

        assignTags(tagsWithPrefix, false, false, true);
    }

    //--//

    public void checkRemoveConditions(ValidationResultsHolder validation,
                                      RecordHelper<AssetRecord> helper) throws
                                                                        Exception
    {
        if (!validation.isForced())
        {
            if (!inStateRecursive(AssetState.retired))
            {
                if (!canRemoveChildren())
                {
                    String name = getName();
                    if (StringUtils.isEmpty(name))
                    {
                        name = getSysId();
                    }

                    validation.addFailure("nestedAssets", "Asset '%s' has children, not in Retired state", name);
                }
            }
        }
        else
        {
            for (AssetRecord rec_child : getChildren(helper))
            {
                rec_child.checkRemoveConditions(validation, helper);
            }
        }
    }

    protected abstract boolean canRemoveChildren();

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<AssetRecord> helper) throws
                                                         Exception
    {
        checkRemoveConditions(validation, helper);

        if (validation.canProceed())
        {
            removeInner(validation, helper);
        }
    }

    protected void removeInner(ValidationResultsHolder validation,
                               RecordHelper<AssetRecord> helper) throws
                                                                 Exception
    {
        for (AssetRecord rec_child : getChildren(helper))
        {
            rec_child.remove(validation, helper);
        }

        helper.delete(this);
    }

    public <T extends AssetRecord> List<T> getChildren(RecordHelper<T> helper)
    {
        List<T> children = Lists.newArrayList();

        try
        {
            enumerateChildren(helper, true, -1, null, (rec_child) ->
            {
                children.add(rec_child);

                return StreamHelperNextAction.Continue;
            });
        }
        catch (Exception e)
        {
            // We need the try/catch just because the callback parameter declares an exception.
            throw Exceptions.wrapAsRuntimeException(e);
        }

        return children;
    }

    public boolean hasAnyChildren(RecordHelper<AssetRecord> helper) throws
                                                                    Exception
    {
        AtomicBoolean hasChildren = new AtomicBoolean();

        enumerateChildrenNoNesting(helper, 1, null, (rec_child) ->
        {
            hasChildren.set(true);
            return StreamHelperNextAction.Stop_Evict;
        });

        return hasChildren.get();
    }

    public boolean hasAnyChildrenOfType(RecordHelper<AssetRecord> helper,
                                        Class<? extends AssetRecord> clz) throws
                                                                          Exception
    {
        AtomicBoolean hasChildren = new AtomicBoolean();

        enumerateChildrenNoNesting(helper, -1, null, (rec_child) ->
        {
            if (SessionHolder.isEntityOfClass(rec_child, clz))
            {
                hasChildren.set(true);
                return StreamHelperNextAction.Stop_Evict;
            }

            return StreamHelperNextAction.Continue_Evict;
        });

        return hasChildren.get();
    }
}