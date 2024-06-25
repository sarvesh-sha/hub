/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.asset;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Tuple;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.model.asset.Device;
import com.optio3.cloud.hub.model.asset.DeviceElementFilterRequest;
import com.optio3.cloud.hub.model.asset.DeviceElementSampling;
import com.optio3.cloud.hub.model.asset.DeviceFilterRequest;
import com.optio3.cloud.hub.model.customization.digineous.model.DigineousDeviceConfig;
import com.optio3.cloud.hub.model.customization.digineous.model.DigineousMachineConfig;
import com.optio3.cloud.hub.persistence.FixupProcessingRecord;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.SortCriteria;
import com.optio3.cloud.persistence.MetadataField;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.PersistAsJsonHelper;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RawQueryHelper;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.cloud.search.HibernateIndexingContext;
import com.optio3.cloud.search.Optio3HibernateSearchContext;
import com.optio3.collection.Memoizer;
import com.optio3.logging.Logger;
import com.optio3.metadata.normalization.ImportExportData;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.objects.device;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Entity
@Table(name = "ASSET_DEVICE")
@DynamicUpdate // Due to HHH-11506
@Indexed()
@Optio3HibernateSearchContext(handler = DeviceRecord.DeviceIndexingHelper.class)
@Optio3TableInfo(externalId = "Device", model = Device.class, metamodel = DeviceRecord_.class, metadata = DeviceRecord.WellKnownMetadata.class)
public class DeviceRecord extends AssetRecord
{
    public static class WellKnownMetadata implements Optio3TableInfo.IMetadataDigest
    {
        public static final MetadataField<Integer> minutesBeforeTransitionToUnreachable = new MetadataField<>("minutesBeforeTransitionToUnreachable", Integer.class);
        public static final MetadataField<Integer> minutesBeforeTransitionToReachable   = new MetadataField<>("minutesBeforeTransitionToReachable", Integer.class);

        public static final MetadataField<DigineousMachineConfig> digineous_machineConfig = new MetadataField<>("digineous_machineConfig", DigineousMachineConfig.class);
        public static final MetadataField<DigineousDeviceConfig>  digineous_deviceConfig  = new MetadataField<>("digineous_deviceConfig", DigineousDeviceConfig.class);
    }

    //--//

    public static class FixupForDeviceInfoClassification extends FixupProcessingRecord.Handler
    {
        @Override
        public Result process(Logger logger,
                              SessionHolder sessionHolder) throws
                                                           Exception
        {
            RecordHelper<DeviceRecord> helper_device = sessionHolder.createHelper(DeviceRecord.class);

            Map<String, String> deviceToObject = Maps.newHashMap();

            BACnetDeviceRecord.findDeviceObjects(sessionHolder)
                              .forEach((k, v) -> deviceToObject.put(v, k));

            for (DeviceRecord rec_device : helper_device.listAll())
            {
                rec_device.modifyMetadata((map) ->
                                          {
                                              map.remove("deviceInfoClassification");
                                          });

                BACnetDeviceRecord rec_device2 = Reflection.as(rec_device, BACnetDeviceRecord.class);
                if (rec_device2 != null)
                {
                    DeviceElementRecord rec_element = sessionHolder.getEntityOrNull(DeviceElementRecord.class, deviceToObject.get(rec_device.getSysId()));
                    if (rec_element != null)
                    {
                        rec_device2.setHintForDeviceObject(rec_element);

                        device model = rec_element.getTypedContents(BACnetObjectModel.getObjectMapper(), device.class);
                        if (model != null)
                        {
                            rec_device.setManufacturerName(model.vendor_name);
                            rec_device.setProductName(model.model_name);
                            rec_device.setFirmwareVersion(model.firmware_revision);
                        }
                    }
                }

                rec_device.dontRefreshUpdatedOn();
            }

            return Result.Done;
        }
    }

    //--//

    public static class DeviceIndexingHelper extends HibernateIndexingContext
    {
        public final Map<String, BaseAssetDescriptor> identityDescriptorLookup = Maps.newHashMap();

        @Override
        public void initialize(AbstractApplicationWithDatabase<?> app,
                               String databaseId,
                               Memoizer memoizer)
        {
            class RawAssetModel
            {
                public String              sysId;
                public BaseAssetDescriptor identityDescriptor;
            }

            // Reuse the same instance, since we don't store the individual models.
            final var singletonModel = new RawAssetModel();

            try (SessionHolder sessionHolder = SessionHolder.createWithNewReadOnlySession(app, databaseId, Optio3DbRateLimiter.Normal))
            {
                RawQueryHelper<DeviceRecord, RawAssetModel> qh = new RawQueryHelper<>(sessionHolder, DeviceRecord.class);

                qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
                qh.addStringDeserializer(DeviceRecord_.identityDescriptor, BaseAssetDescriptor.class, (obj, val) -> obj.identityDescriptor = val);

                qh.stream(() -> singletonModel, (model) ->
                {
                    if (model.identityDescriptor != null)
                    {
                        String sysId = memoizer.intern(model.sysId);
                        identityDescriptorLookup.put(sysId, model.identityDescriptor);
                    }
                });
            }
        }
    }

    //--//

    @Field
    @Column(name = "manufacturer_name", length = 4000)
    private String manufacturerName;

    @Field
    @Column(name = "product_name", length = 4000)
    private String productName;

    @Field
    @Column(name = "model_name", length = 4000)
    private String modelName;

    @Column(name = "firmware_version")
    private String firmwareVersion;

    @Lob
    @Column(name = "identity_descriptor")
    private String identityDescriptor;

    @Transient
    private final PersistAsJsonHelper<String, BaseAssetDescriptor> m_identityDescriptorHelper = new PersistAsJsonHelper<>(() -> identityDescriptor,
                                                                                                                          (val) -> identityDescriptor = val,
                                                                                                                          String.class,
                                                                                                                          BaseAssetDescriptor.class,
                                                                                                                          ObjectMappers.SkipNulls,
                                                                                                                          true);

    //--//

    private static final BaseAssetDescriptor c_noId = new BaseAssetDescriptor()
    {
        @Override
        public int compareTo(BaseAssetDescriptor o)
        {
            return (o == null || o == this) ? 0 : StringUtils.compareIgnoreCase(this.toString(), o.toString());
        }

        @Override
        public String toString()
        {
            return "<no identifier>";
        }
    };

    //--//

    public DeviceRecord()
    {
    }

    //--//

    @Override
    public BaseAssetDescriptor getIdentityDescriptor()
    {
        return m_identityDescriptorHelper.get();
    }

    public boolean setIdentityDescriptor(BaseAssetDescriptor value)
    {
        return m_identityDescriptorHelper.set(value);
    }

    public List<DeviceElementSampling> prepareSamplingConfiguration(SessionHolder sessionHolder,
                                                                    DeviceElementRecord rec_object,
                                                                    boolean checkNonZeroValue) throws
                                                                                               IOException
    {
        return Collections.emptyList();
    }

    //--//

    public String getManufacturerName()
    {
        return manufacturerName;
    }

    public void setManufacturerName(String vendorName)
    {
        this.manufacturerName = vendorName;
    }

    public String getProductName()
    {
        return productName;
    }

    public void setProductName(String productName)
    {
        this.productName = productName;
    }

    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public String getFirmwareVersion()
    {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion)
    {
        this.firmwareVersion = firmwareVersion;
    }

    //--//

    // Setter for field only present in model.
    public boolean setMinutesBeforeTransitionToUnreachable(int value)
    {
        return modifyMetadata((metadata) ->
                              {
                                  int defaultValue = getDefaultMinutesBeforeTransitionToUnreachable();

                                  if (value == 0 || value == defaultValue)
                                  {
                                      WellKnownMetadata.minutesBeforeTransitionToUnreachable.remove(metadata);
                                  }
                                  else
                                  {
                                      WellKnownMetadata.minutesBeforeTransitionToUnreachable.put(metadata, value);
                                  }
                              });
    }

    public int getMinutesBeforeTransitionToUnreachable()
    {
        return getMinutesBeforeTransitionToUnreachable(getMetadata());
    }

    public int getMinutesBeforeTransitionToUnreachable(MetadataMap metadata)
    {
        return WellKnownMetadata.minutesBeforeTransitionToUnreachable.getOrDefault(metadata, getDefaultMinutesBeforeTransitionToUnreachable());
    }

    protected int getDefaultMinutesBeforeTransitionToUnreachable()
    {
        return 2 * 60 + 5; // Extra five minutes to avoid hitting sampling window.
    }

    public boolean isReachable()
    {
        return true;
    }

    //--//

    // Setter for field only present in model.
    public boolean setMinutesBeforeTransitionToReachable(int value)
    {
        return modifyMetadata((metadata) ->
                              {
                                  int defaultValue = getDefaultMinutesBeforeTransitionToReachable();

                                  if (value == 0 || value == defaultValue)
                                  {
                                      WellKnownMetadata.minutesBeforeTransitionToReachable.remove(metadata);
                                  }
                                  else
                                  {
                                      WellKnownMetadata.minutesBeforeTransitionToReachable.put(metadata, value);
                                  }
                              });
    }

    public int getMinutesBeforeTransitionToReachable()
    {
        return getMinutesBeforeTransitionToReachable(getMetadata());
    }

    public int getMinutesBeforeTransitionToReachable(MetadataMap metadata)
    {
        return WellKnownMetadata.minutesBeforeTransitionToReachable.getOrDefault(metadata, getDefaultMinutesBeforeTransitionToReachable());
    }

    protected int getDefaultMinutesBeforeTransitionToReachable()
    {
        return 60 + 5; // Extra five minutes to avoid hitting sampling window.
    }

    //--//

    public ImportExportData extractImportExportData(LocationsEngine.Snapshot locationsSnapshot,
                                                    DeviceElementRecord rec_object)
    {
        return null; // Subclasses override method.
    }

    public GatewayDiscoveryEntity createRequest(GatewayDiscoveryEntity en_protocol)
    {
        return null;
    }

    public int prepareSamplingConfiguration(RecordHelper<DeviceElementRecord> helper,
                                            GatewayDiscoveryEntity en_protocol) throws
                                                                                Exception
    {
        AtomicInteger objects = new AtomicInteger();

        GatewayDiscoveryEntity en_device = createRequest(en_protocol);
        if (en_device != null)
        {
            Map<String, Integer> map = Maps.newHashMap();

            //
            // Because there could be many objects and they can be large, we can't use 'getNestedAssets', or we'll run out of memory.
            // So we query the items that have sampling configured and immediately evict them from the session.
            //

            //
            // Prepare the filter to only find records with configured samplings.
            //
            DeviceElementFilterRequest filters = DeviceElementFilterRequest.createFilterForParent(this);
            filters.hasAnySampling = true;

            DeviceElementRecord.enumerateNoNesting(helper, filters, (rec_object) ->
            {
                for (DeviceElementSampling elementSampling : rec_object.getSamplingSettings())
                {
                    map.put(elementSampling.propertyName, elementSampling.samplingPeriod);
                }

                if (!map.isEmpty())
                {
                    objects.addAndGet(map.size());

                    GatewayDiscoveryEntity en_config = rec_object.createRequest(en_device, false);
                    en_config.setContentsForObjectConfig(ObjectMappers.SkipNulls, map);
                    map.clear();
                }

                return StreamHelperNextAction.Continue_Evict;
            });
        }

        return objects.get();
    }

    public int preparePropertyUpdate(RecordHelper<DeviceElementRecord> helper,
                                     GatewayDiscoveryEntity en_protocol) throws
                                                                         Exception
    {
        AtomicInteger objects = new AtomicInteger();

        GatewayDiscoveryEntity en_device = createRequest(en_protocol);
        if (en_device != null)
        {
            //
            // Because there could be many objects and they can be large, we can't use 'getNestedAssets', or we'll run out of memory.
            // So we query the items that have sampling configured and immediately evict them from the session.
            //

            DeviceElementFilterRequest filters = DeviceElementFilterRequest.createFilterForParent(this);

            DeviceElementRecord.enumerateNoNesting(helper, filters, (rec_object) ->
            {
                if (rec_object.hasMetadata(DeviceElementRecord.WellKnownMetadata.elementDesiredStateNeeded))
                {
                    GatewayDiscoveryEntity en_config = rec_object.createRequest(en_device, true);
                    if (en_config != null)
                    {
                        objects.incrementAndGet();

                        var obj = rec_object.getContentsAsObject(true);
                        en_config.setContentsAsObject(ObjectMappers.SkipNulls, obj);
                    }
                }

                return StreamHelperNextAction.Continue_Evict;
            });
        }

        return objects.get();
    }

    public int completePropertyUpdate(RecordHelper<DeviceElementRecord> helper,
                                      ZonedDateTime threshold,
                                      AtomicBoolean mustRerun) throws
                                                               Exception
    {
        AtomicInteger objects = new AtomicInteger();

        //
        // Because there could be many objects and they can be large, we can't use 'getNestedAssets', or we'll run out of memory.
        // So we query the items that have sampling configured and immediately evict them from the session.
        //

        DeviceElementFilterRequest filters = DeviceElementFilterRequest.createFilterForParent(this);

        DeviceElementRecord.enumerateNoNesting(helper, filters, (rec_object) ->
        {
            boolean modified = false;

            var lastUpdate = rec_object.getMetadata(DeviceElementRecord.WellKnownMetadata.elementDesiredStateNeeded);
            if (lastUpdate != null)
            {
                objects.incrementAndGet();

                if (lastUpdate.isAfter(threshold))
                {
                    mustRerun.set(true);
                }
                else
                {
                    modified |= rec_object.putMetadata(DeviceElementRecord.WellKnownMetadata.elementDesiredState, null);
                    modified |= rec_object.putMetadata(DeviceElementRecord.WellKnownMetadata.elementDesiredStateNeeded, null);
                }
            }

            return modified ? StreamHelperNextAction.Continue_Flush_Evict : StreamHelperNextAction.Continue_Evict;
        });

        return objects.get();
    }

    //--//

    private static class DeviceJoinHelper<T, D extends DeviceRecord> extends AssetJoinHelper<T, D>
    {
        DeviceJoinHelper(RecordHelper<D> helper,
                         Class<T> clz)
        {
            super(helper, clz);
        }

        //--//

        void applyFilters(DeviceFilterRequest filters)
        {
            super.applyFilters(filters);

            if (filters.sortBy != null)
            {
                for (SortCriteria sort : filters.sortBy)
                {
                    switch (sort.column)
                    {
                        case "identityDescriptor":
                            addDescriptorSortExtension((l, r) ->
                                                       {
                                                           int diff = l.compareTo(r);
                                                           return sort.ascending ? diff : -diff;
                                                       });
                            break;

                        case "transportDescriptor":
                            addDescriptorSortExtension((l, r) ->
                                                       {
                                                           int diff = 0;
                                                           if (l instanceof BACnetDeviceDescriptor && r instanceof BACnetDeviceDescriptor)
                                                           {
                                                               diff = BACnetDeviceDescriptor.compare((BACnetDeviceDescriptor) l, (BACnetDeviceDescriptor) r, true);
                                                           }

                                                           return sort.ascending ? diff : -diff;
                                                       });
                            break;
                    }
                }
            }

            filterByManufacturerName(filters.likeDeviceManufacturerName);
            filterByProductName(filters.likeDeviceProductName);
            filterByModelName(filters.likeDeviceModelName);
        }

        private void addDescriptorSortExtension(Comparator<BaseAssetDescriptor> comparator)
        {
            addSortExtension(new SortExtension<String>()
            {
                private Multimap<BaseAssetDescriptor, RecordIdentity> m_map = ArrayListMultimap.create();

                @Override
                public Path<String> getPath()
                {
                    return root.get(DeviceRecord_.identityDescriptor);
                }

                @Override
                public void processValue(RecordIdentity ri,
                                         String id)
                {
                    BaseAssetDescriptor desc;

                    try
                    {
                        desc = id != null ? ObjectMappers.SkipNulls.readValue(id, BaseAssetDescriptor.class) : c_noId;
                    }
                    catch (IOException e)
                    {
                        // Ignore failures.
                        desc = c_noId;
                    }

                    m_map.put(desc, ri);
                }

                @Override
                public void processResults(List<RecordIdentity> results)
                {
                    results.clear();

                    List<BaseAssetDescriptor> descriptors = Lists.newArrayList(m_map.keySet());
                    descriptors.sort(comparator);

                    for (BaseAssetDescriptor rec_location : descriptors)
                    {
                        results.addAll(m_map.get(rec_location));
                    }
                }
            });
        }

        @Override
        protected Predicate predicateForLike(List<ParsedLike> likeFilters)
        {
            return or(super.predicateForLike(likeFilters), predicateForLike(root, DeviceRecord_.identityDescriptor, likeFilters));
        }

        //--//

        void filterByManufacturerName(String likeName)
        {
            List<ParsedLike> likeFilters = ParsedLike.decode(likeName);
            if (likeFilters != null)
            {
                addWhereClause(predicateForLike(root, DeviceRecord_.manufacturerName, likeFilters));
            }
        }

        void filterByProductName(String likeName)
        {
            List<ParsedLike> likeFilters = ParsedLike.decode(likeName);
            if (likeFilters != null)
            {
                addWhereClause(predicateForLike(root, DeviceRecord_.productName, likeFilters));
            }
        }

        void filterByModelName(String likeName)
        {
            List<ParsedLike> likeFilters = ParsedLike.decode(likeName);
            if (likeFilters != null)
            {
                addWhereClause(predicateForLike(root, DeviceRecord_.modelName, likeFilters));
            }
        }
    }

    //--//

    public static List<DeviceRecord> getBatch(RecordHelper<DeviceRecord> helper,
                                              List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public static <T extends DeviceRecord> Map<String, Number> countDevicesByLocation(RecordHelper<T> helper,
                                                                                      DeviceFilterRequest filters)
    {
        DeviceJoinHelper<Tuple, T> jh = new DeviceJoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            filters.sortBy = null; // Just in case, sanitize input.

            jh.applyFilters(filters);
        }

        return jh.countByField(jh.joinWithLocation(), RecordWithCommonFields_.sysId);
    }

    public static <T extends DeviceRecord> Map<String, Number> countDevicesByManufacturer(RecordHelper<T> helper,
                                                                                          DeviceFilterRequest filters)
    {
        DeviceJoinHelper<Tuple, T> jh = new DeviceJoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            filters.sortBy = null; // Just in case, sanitize input.

            jh.applyFilters(filters);
        }

        return jh.countByField(jh.root, DeviceRecord_.manufacturerName);
    }

    public static <T extends DeviceRecord> List<RecordIdentity> filterDevices(RecordHelper<T> helper,
                                                                              DeviceFilterRequest filters)
    {
        DeviceJoinHelper<Tuple, T> jh = new DeviceJoinHelper<>(helper, Tuple.class);

        if (filters != null)
        {
            jh.applyFilters(filters);
        }

        return AssetJoinHelper.returnFilterTuples(helper, jh);
    }

    public static <T extends DeviceRecord> long countDevices(RecordHelper<T> helper,
                                                             DeviceFilterRequest filters)
    {
        DeviceJoinHelper<Tuple, T> jh = new DeviceJoinHelper<>(helper, Tuple.class);

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
        // Nothing to do.
    }

    @Override
    protected void assetPostUpdateInner(SessionHolder sessionHolder)
    {
        // Nothing to do here, subclasses override method.
    }

    @Override
    protected boolean canRemoveChildren()
    {
        // We store other things under a Device record, they have to be individually deleted.
        return false;
    }
}
