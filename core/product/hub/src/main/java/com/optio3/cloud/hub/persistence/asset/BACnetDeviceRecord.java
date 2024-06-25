/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.asset;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.logic.protocol.BACnetDecoder;
import com.optio3.cloud.hub.logic.protocol.IProtocolDecoder;
import com.optio3.cloud.hub.model.asset.BACnetDevice;
import com.optio3.cloud.hub.model.asset.DeviceElementFilterRequest;
import com.optio3.cloud.hub.model.asset.DeviceElementSampling;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesEnumeratedValue;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LazyRecordFlusher;
import com.optio3.cloud.persistence.MetadataField;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.MetadataTagsMap;
import com.optio3.cloud.persistence.RawQueryHelper;
import com.optio3.cloud.persistence.RecordForFixupProcessing;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.logging.Logger;
import com.optio3.metadata.normalization.BACnetImportExportData;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.enums.BACnetEngineeringUnits;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.objects.lift;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.serialization.TypedBitSet;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.search.annotations.Indexed;

@Entity
@Table(name = "ASSET_DEVICE_BACNET")
@DynamicUpdate // Due to HHH-11506
@Indexed()
@Optio3TableInfo(externalId = "BACnetDevice", model = BACnetDevice.class, metamodel = BACnetDeviceRecord_.class, metadata = BACnetDeviceRecord.WellKnownMetadata.class)
public class BACnetDeviceRecord extends DeviceRecord implements AssetRecord.IProviderOfPropertyTypeExtractorClass
{
    public static class WellKnownMetadata implements Optio3TableInfo.IMetadataDigest
    {
        public static final MetadataField<String>             bacnetDeviceObjectHints = new MetadataField<>("bacnetDeviceObjectHints", String.class);
        public static final MetadataField<BACnetReachability> bacnetReachability      = new MetadataField<>("bacnetReachability", BACnetReachability.class, BACnetReachability::new);
    }

    public static class BACnetReachability
    {
        public ZonedDateTime reachable;
        public ZonedDateTime unreachable;

        public ZonedDateTime notifiedReachable;
        public ZonedDateTime notifiedUnreachable;
        public ZonedDateTime warningDebounce;

        public static class Fixup extends RecordForFixupProcessing.Handler
        {
            @Override
            public RecordForFixupProcessing.Handler.Result process(Logger logger,
                                                                   SessionHolder sessionHolder) throws
                                                                                                Exception
            {
                RecordHelper<BACnetDeviceRecord> helper_device = sessionHolder.createHelper(BACnetDeviceRecord.class);

                for (BACnetDeviceRecord rec_device : helper_device.listAll())
                {
                    rec_device.modifyMetadata((map) ->
                                              {
                                                  BACnetReachability state = WellKnownMetadata.bacnetReachability.get(map);

                                                  state.unreachable         = patch(map, "bacnetUnreachable");
                                                  state.notifiedUnreachable = patch(map, "bacnetWarning");
                                                  state.warningDebounce     = patch(map, "bacnetWarningDebounce");

                                                  WellKnownMetadata.bacnetReachability.put(map, state);
                                              });

                    rec_device.dontRefreshUpdatedOn();
                }

                return Result.Done;
            }

            private static ZonedDateTime patch(MetadataMap map,
                                               String key)
            {
                ZonedDateTime val = map.getDateTime(key);

                map.remove(key);

                return val;
            }
        }
    }

    public static class TypeExtractor extends PropertyTypeExtractor
    {
        @Override
        public Map<String, TimeSeriesPropertyType> classifyRecord(DeviceElementRecord rec,
                                                                  boolean handlePresentationType)
        {
            Map<String, TimeSeriesPropertyType> res = Maps.newHashMap();

            try
            {
                EngineeringUnits units = getUnitsWithClassificationOverride(rec);

                final BACnetObjectModel obj = getContentsAsObject(rec, false);
                if (obj != null)
                {
                    res.putAll(classifyModel(obj, handlePresentationType));

                    BACnetObjectType objectType = obj.getObjectType();
                    if (units != null)
                    {
                        switch (objectType)
                        {
                            case analog_input:
                            case analog_output:
                            case analog_value:
                            case large_analog_value:
                                assignUnit(res, units, BACnetPropertyIdentifier.present_value);
                                assignUnit(res, units, BACnetPropertyIdentifier.min_pres_value);
                                assignUnit(res, units, BACnetPropertyIdentifier.max_pres_value);
                                assignUnit(res, units, BACnetPropertyIdentifier.resolution);
                                assignUnit(res, units, BACnetPropertyIdentifier.cov_increment);
                                assignUnit(res, units, BACnetPropertyIdentifier.interface_value);
                                assignUnit(res, units, BACnetPropertyIdentifier.relinquish_default);
                                break;

                            case integer_value:
                            case positive_integer_value:
                                assignUnit(res, units, BACnetPropertyIdentifier.min_pres_value);
                                assignUnit(res, units, BACnetPropertyIdentifier.max_pres_value);
                                assignUnit(res, units, BACnetPropertyIdentifier.resolution);
                                assignUnit(res, units, BACnetPropertyIdentifier.interface_value);
                                break;

                            case life_safety_point:
                                assignUnit(res, units, BACnetPropertyIdentifier.direct_reading);
                                break;

                            case pulse_converter:
                                assignUnit(res, units, BACnetPropertyIdentifier.scale_factor);
                                assignUnit(res, units, BACnetPropertyIdentifier.adjust_value);
                                break;
                        }
                    }

                    switch (objectType)
                    {
                        case load_control:
                            assignUnit(res, EngineeringUnits.minutes, BACnetPropertyIdentifier.shed_duration);
                            assignUnit(res, EngineeringUnits.minutes, BACnetPropertyIdentifier.duty_window);
                            assignUnit(res, EngineeringUnits.kilowatts, BACnetPropertyIdentifier.full_duty_baseline);
                            break;

                        case lighting_output:
                        case binary_lighting_output:
                            assignUnit(res, EngineeringUnits.kilowatts, BACnetPropertyIdentifier.power);
                            assignUnit(res, EngineeringUnits.kilowatts, BACnetPropertyIdentifier.instantaneous_power);
                            assignUnit(res, EngineeringUnits.kilowatts, BACnetPropertyIdentifier.relinquish_default);
                            break;

                        case lift:
                        {
                            lift obj2 = (lift) obj;

                            assignUnit(res, obj2.car_load_units.getNormalizedUnits(), BACnetPropertyIdentifier.car_load);
                            assignUnit(res, EngineeringUnits.kilowatts, BACnetPropertyIdentifier.instantaneous_power);

                            assignUnit(res, EngineeringUnits.kilowatt_hours, BACnetPropertyIdentifier.energy_meter);
                            break;
                        }

                        case escalator:
                            assignUnit(res, EngineeringUnits.kilowatt_hours, BACnetPropertyIdentifier.energy_meter);
                            break;
                    }
                }
            }
            catch (Exception e)
            {
                HubApplication.LoggerInstance.error("Encountered a problem while trying to classify object '%s': %s", e, rec.getIdentifier());
            }

            return res;
        }

        @Override
        protected void classifyInstance(Map<String, TimeSeriesPropertyType> map,
                                        BaseObjectModel obj,
                                        boolean handlePresentationType)
        {
            BACnetObjectModel objTyped = Reflection.as(obj, BACnetObjectModel.class);
            if (objTyped != null)
            {
                BACnetObjectType objectType = objTyped.getObjectType();

                for (BACnetPropertyIdentifier prop : objectType.propertyTypes()
                                                               .keySet())
                {
                    Type                   t  = objTyped.getType(prop);
                    TimeSeriesPropertyType pt = new TimeSeriesPropertyType();
                    pt.name        = prop.name();
                    pt.displayName = prop.getDisplayName();

                    pt.expectedType = t;
                    pt.targetField  = pt.name;

                    if (Reflection.canAssignTo(TypedBitSet.class, t))
                    {
                        Class<?> clzField = Reflection.getRawType(t);
                        Class<?> enumType = Reflection.searchTypeArgument(TypedBitSet.class, clzField, 0);
                        pt.tryToExtractEnumValues(enumType);

                        pt.type = TimeSeries.SampleType.BitSet;
                    }
                    else if (Reflection.canAssignTo(BitSet.class, t))
                    {
                        pt.type = TimeSeries.SampleType.BitSet;
                    }
                    else if (Reflection.canAssignTo(Enum.class, t))
                    {
                        pt.tryToExtractEnumValues(t);

                        pt.type = TimeSeries.SampleType.Enumerated;
                    }
                    else if (Reflection.canAssignTo(String.class, t))
                    {
                        pt.type = TimeSeries.SampleType.Enumerated;
                    }
                    else if (Reflection.canAssignTo(String[].class, t))
                    {
                        pt.type = TimeSeries.SampleType.EnumeratedSet;
                    }
                    else
                    {
                        TypeDescriptor td = Reflection.getDescriptor(t);
                        if (td == null)
                        {
                            continue;
                        }

                        if (td.isFloatingType())
                        {
                            pt.type = TimeSeries.SampleType.Decimal;
                        }
                        else
                        {
                            pt.type = TimeSeries.SampleType.Integer;

                            if (td.clz == Boolean.TYPE)
                            {
                                pt.isBoolean = true;
                            }

                            Object   val    = objTyped.getValue(BACnetPropertyIdentifier.state_text, null);
                            String[] values = Reflection.as(val, String[].class);
                            if (values != null)
                            {
                                pt.type = TimeSeries.SampleType.Enumerated;

                                int pos = 0;

                                TimeSeriesEnumeratedValue newEnumValue = pt.addEnumValue();
                                newEnumValue.name  = "<no value>";
                                newEnumValue.value = pos++;

                                for (String value : values)
                                {
                                    newEnumValue       = pt.addEnumValue();
                                    newEnumValue.name  = value;
                                    newEnumValue.value = pos++;
                                }
                            }
                        }
                    }

                    map.put(pt.name, pt);
                }
            }
        }

        @Override
        public IProtocolDecoder getProtocolDecoder()
        {
            return new BACnetDecoder();
        }

        @Override
        public EngineeringUnitsFactors getUnitsFactors(DeviceElementRecord rec)
        {
            EngineeringUnits units = getUnitsWithClassificationOverride(rec);
            if (units != null)
            {
                return units.getConversionFactors();
            }

            return null;
        }

        private EngineeringUnits getUnitsWithClassificationOverride(DeviceElementRecord rec_object)
        {
            EngineeringUnits units = null;
            try
            {
                MetadataMap metadata = rec_object.getMetadata();
                units = AssetRecord.WellKnownMetadata.assignedUnits.get(metadata);

                if (units == null)
                {
                    final BACnetObjectModel obj = getContentsAsObject(rec_object, false);
                    if (obj != null)
                    {
                        BACnetEngineeringUnits bacnetUnits = obj.getUnits();
                        units = bacnetUnits.getNormalizedUnits();
                    }
                }
            }
            catch (Throwable t)
            {
                // Ignore failures.
            }

            return units;
        }

        @Override
        public String getIndexedValue(DeviceElementRecord rec)
        {
            return null;
        }

        @Override
        public BACnetObjectModel getContentsAsObject(DeviceElementRecord rec,
                                                     boolean desiredState) throws
                                                                           IOException
        {
            ObjectMapper om = BACnetObjectModel.getObjectMapper();

            if (desiredState)
            {
                return rec.getTypedDesiredContents(om, BACnetObjectModel.class);
            }
            else
            {
                return rec.getTypedContents(om, BACnetObjectModel.class);
            }
        }

        //--//

        private static void assignUnit(Map<String, TimeSeriesPropertyType> res,
                                       EngineeringUnits units,
                                       BACnetPropertyIdentifier prop)
        {
            TimeSeriesPropertyType pt = res.get(prop.name());
            if (pt != null)
            {
                if (!EngineeringUnitsFactors.areIdentical(pt.unitsFactors, units.getConversionFactors()))
                {
                    pt = pt.copy();
                    pt.setUnits(units);
                    res.put(prop.name(), pt);
                }
            }
        }
    }

    //--//

    public BACnetDeviceRecord()
    {
    }

    @Override
    public boolean isReachable()
    {
        var state = getMetadata(WellKnownMetadata.bacnetReachability);
        if (state != null)
        {
            return state.reachable != null;
        }

        return super.isReachable();
    }

    //--//

    @Override
    public List<DeviceElementSampling> prepareSamplingConfiguration(SessionHolder sessionHolder,
                                                                    DeviceElementRecord rec_object,
                                                                    boolean checkNonZeroValue) throws
                                                                                               IOException
    {
        List<DeviceElementSampling> config = Lists.newArrayList();

        BACnetObjectModel obj = rec_object.getTypedContents(BACnetObjectModel.getObjectMapper(), BACnetObjectModel.class);
        if (obj != null)
        {
            final int samplingPeriod = getParentAsset(NetworkAssetRecord.class).getSamplingPeriod();

            switch (obj.getObjectType())
            {
                case accumulator:
                case analog_input:
                case analog_output:
                case analog_value:
                case large_analog_value:
                {
                    if (checkNonZeroValue)
                    {
                        double value = obj.getNumber(BACnetPropertyIdentifier.present_value, null, Double.class);
                        if (value == 0.0)
                        {
                            break;
                        }

                        BACnetEngineeringUnits units = obj.getUnits();
                        if (units == BACnetEngineeringUnits.no_units)
                        {
                            if (4090 <= value && value <= 4100)
                            {
                                // Some devices report a value in this range but without units. It's pretty useless, ignoring it.
                                break;
                            }
                        }
                    }

                    DeviceElementSampling.add(config, BACnetPropertyIdentifier.present_value, samplingPeriod);
                    DeviceElementSampling.add(config, BACnetPropertyIdentifier.event_state, samplingPeriod);
                    DeviceElementSampling.add(config, BACnetPropertyIdentifier.out_of_service, samplingPeriod);
                    DeviceElementSampling.add(config, BACnetPropertyIdentifier.status_flags, samplingPeriod);
                }
                break;

                case binary_input:
                case binary_output:
                case binary_value:
                {
                    if (checkNonZeroValue)
                    {
                        if (obj.extractName(false) == null)
                        {
                            // If it's a binary object with a default name, it's pretty useless, ignoring it.
                            break;
                        }
                    }

                    DeviceElementSampling.add(config, BACnetPropertyIdentifier.present_value, samplingPeriod);
                    DeviceElementSampling.add(config, BACnetPropertyIdentifier.event_state, samplingPeriod);
                    DeviceElementSampling.add(config, BACnetPropertyIdentifier.out_of_service, samplingPeriod);
                    DeviceElementSampling.add(config, BACnetPropertyIdentifier.status_flags, samplingPeriod);
                }
                break;

                case integer_value:
                case positive_integer_value:
                {
                    DeviceElementSampling.add(config, BACnetPropertyIdentifier.present_value, samplingPeriod);
                    DeviceElementSampling.add(config, BACnetPropertyIdentifier.event_state, samplingPeriod);
                    DeviceElementSampling.add(config, BACnetPropertyIdentifier.out_of_service, samplingPeriod);
                    DeviceElementSampling.add(config, BACnetPropertyIdentifier.status_flags, samplingPeriod);
                }
                break;

                case multi_state_input:
                case multi_state_output:
                case multi_state_value:
                {
                    DeviceElementSampling.add(config, BACnetPropertyIdentifier.present_value, samplingPeriod);
                    DeviceElementSampling.add(config, BACnetPropertyIdentifier.event_state, samplingPeriod);
                    DeviceElementSampling.add(config, BACnetPropertyIdentifier.out_of_service, samplingPeriod);
                    DeviceElementSampling.add(config, BACnetPropertyIdentifier.status_flags, samplingPeriod);
                }
                break;
            }
        }

        return config;
    }

    public Class<? extends PropertyTypeExtractor> getPropertyTypeExtractorClass()
    {
        return TypeExtractor.class;
    }

    @Override
    protected void removeInner(ValidationResultsHolder validation,
                               RecordHelper<AssetRecord> helper) throws
                                                                 Exception
    {
        reconfigureSampling(helper.currentSessionHolder());

        HubApplication.LoggerInstance.info("Removing %s [%s]...", getName(), getIdentityDescriptor());

        super.removeInner(validation, helper);

        HubApplication.LoggerInstance.info("Removed %s [%s]...", getName(), getIdentityDescriptor());

        validation.sessionHolder.commitAndBeginNewTransaction();
    }

    //--//

    @Override
    public BACnetImportExportData extractImportExportData(LocationsEngine.Snapshot locationsSnapshot,
                                                          DeviceElementRecord rec_object)
    {
        BACnetDeviceDescriptor desc = getIdentityDescriptor(BACnetDeviceDescriptor.class);

        BACnetImportExportData item = new BACnetImportExportData();
        item.sysId      = rec_object.getSysId();
        item.networkId  = desc.address.networkNumber;
        item.instanceId = desc.address.instanceNumber;
        item.objectId   = new BACnetObjectIdentifier(rec_object.getIdentifier());
        item.transport  = desc.transport;

        MetadataMap metadata = rec_object.getMetadata();
        item.dashboardName      = AssetRecord.WellKnownMetadata.nameFromLegacyImport.get(metadata);
        item.dashboardStructure = AssetRecord.WellKnownMetadata.structureFromLegacyImport.get(metadata);

        item.normalizedName = rec_object.getName();
        item.isSampled      = rec_object.isSampled();

        MetadataTagsMap tags = rec_object.accessTags();
        item.pointClassId  = CollectionUtils.firstElement(tags.getValuesForTag(WellKnownTags.pointClassId));
        item.pointClassAdt = rec_object.getAzureDigitalTwinModel();
        item.pointTags     = WellKnownTags.getTags(tags, false, true, true);

        if (locationsSnapshot != null)
        {
            LocationRecord rec_location = rec_object.getLocation();

            // If it's a proxy, using "getSysId()" would not trigger a DB access.
            item.locationSysId = RecordWithCommonFields.getSysIdSafe(rec_location);
            item.locationName  = locationsSnapshot.getNameLazy(rec_location);
        }

        try
        {
            BACnetObjectModel obj = rec_object.getTypedContents(BACnetObjectModel.getObjectMapper(), BACnetObjectModel.class);
            if (obj != null)
            {
                BACnetEngineeringUnits units = obj.getUnits();

                item.deviceName        = (String) obj.getValue(BACnetPropertyIdentifier.object_name, null);
                item.deviceDescription = (String) obj.getValue(BACnetPropertyIdentifier.description, null);
                item.deviceLocation    = (String) obj.getValue(BACnetPropertyIdentifier.location, null);
                item.deviceModel       = (String) obj.getValue(BACnetPropertyIdentifier.model_name, null);
                item.deviceVendor      = (String) obj.getValue(BACnetPropertyIdentifier.vendor_name, null);

                item.units = units != null ? units.getNormalizedUnits() : EngineeringUnits.no_units;

                Object valueRaw = obj.getValue(BACnetPropertyIdentifier.present_value, null);
                if (valueRaw != null)
                {
                    item.value = valueRaw.toString();
                }
            }
        }
        catch (Exception e)
        {
            // Ignore problems.
        }

        // Only report dashboard name if different.
        if (StringUtils.equals(item.deviceName, item.dashboardName))
        {
            item.dashboardName = null;
        }

        if (StringUtils.equals(item.deviceDescription, item.dashboardName))
        {
            item.dashboardName = null;
        }

        return item;
    }

    public DeviceElementRecord getDeviceObjectFromHint(RecordHelper<DeviceElementRecord> helper)
    {
        String sysId = getMetadata(WellKnownMetadata.bacnetDeviceObjectHints);
        if (sysId != null)
        {
            DeviceElementRecord rec_object = helper.getOrNull(sysId);
            if (rec_object != null && sameSysId(rec_object.getParentAsset()))
            {
                return rec_object;
            }
        }
        return null;
    }

    public void setHintForDeviceObject(DeviceElementRecord rec_object)
    {
        if (rec_object != null && sameSysId(rec_object.getParentAsset()))
        {
            putMetadata(WellKnownMetadata.bacnetDeviceObjectHints, rec_object.getSysId());
        }
    }

    public DeviceElementRecord findDeviceObject(RecordHelper<DeviceElementRecord> helper) throws
                                                                                          Exception
    {
        DeviceElementRecord rec_objectHint = getDeviceObjectFromHint(helper);
        if (rec_objectHint != null)
        {
            return rec_objectHint;
        }

        //
        // Because there could be many objects and they can be large, we can't use 'getNestedAssets', or we'll run out of memory.
        // So we query the items that have no contents and immediately evict them from the session.
        //
        AtomicReference<DeviceElementRecord> res = new AtomicReference<>();

        final DeviceElementFilterRequest filters = DeviceElementFilterRequest.createFilterForParent(this);
        DeviceElementRecord.enumerateNoNesting(helper, filters, (rec_object) ->
        {
            BACnetObjectIdentifier objId = new BACnetObjectIdentifier(rec_object.getIdentifier());
            if (objId.object_type.equals(BACnetObjectType.device))
            {
                setHintForDeviceObject(rec_object);

                res.set(rec_object);
                return StreamHelperNextAction.Stop;
            }

            return StreamHelperNextAction.Continue_Evict;
        });

        return res.get();
    }

    public static Map<String, String> findDeviceObjects(SessionHolder sessionHolder)
    {
        class RawAssetModel
        {
            public String sysId;
            public String parentAsset;
            public String identifier;
        }

        Map<String, String> res = Maps.newHashMap();

        RawQueryHelper<DeviceElementRecord, RawAssetModel> qh = new RawQueryHelper<>(sessionHolder, DeviceElementRecord.class);

        qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
        qh.addReferenceRaw(AssetRecord_.parentAsset, (obj, val) -> obj.parentAsset = val);
        qh.addString(DeviceElementRecord_.identifier, (obj, val) -> obj.identifier = val);

        // Reuse the same instance, since we don't store the individual models.
        final var singletonModel = new RawAssetModel();

        qh.stream(() -> singletonModel, (model) ->
        {
            try
            {
                BACnetObjectIdentifier objId = new BACnetObjectIdentifier(model.identifier);
                if (objId.object_type.equals(BACnetObjectType.device))
                {
                    res.put(model.sysId, model.parentAsset);
                }
            }
            catch (Throwable t)
            {
                // In case it's not a BACnet object.
            }
        });

        return res;
    }

    @Override
    public GatewayDiscoveryEntity createRequest(GatewayDiscoveryEntity en_protocol)
    {
        return en_protocol.createAsRequest(GatewayDiscoveryEntitySelector.BACnet_Device, getIdentityDescriptor());
    }

    //--//

    public static LazyRecordFlusher<BACnetDeviceRecord> ensureDeviceFromDescriptor(RecordHelper<BACnetDeviceRecord> helper,
                                                                                   AssetRecord parent,
                                                                                   BACnetDeviceDescriptor descTarget) throws
                                                                                                                      Exception
    {
        BACnetDeviceRecord rec_device = findByDescriptor(helper, parent, descTarget);
        if (rec_device == null)
        {
            rec_device = new BACnetDeviceRecord();
            rec_device.setIdentityDescriptor(descTarget);

            return helper.wrapAsNewRecord(rec_device, (rec_device2) -> rec_device2.linkToParent(helper, parent));
        }
        else
        {
            rec_device.setIdentityDescriptor(descTarget);

            RecordLocked<BACnetDeviceRecord> lock_device = helper.optimisticallyUpgradeToLocked(rec_device, 10, TimeUnit.SECONDS);
            return helper.wrapAsExistingRecord(lock_device.get());
        }
    }

    public static BACnetDeviceRecord findByDescriptor(RecordHelper<BACnetDeviceRecord> helper,
                                                      AssetRecord parent,
                                                      BACnetDeviceDescriptor descTarget) throws
                                                                                         Exception
    {
        AtomicReference<BACnetDeviceRecord> rec_existing = new AtomicReference<>();

        parent.enumerateChildrenNoNesting(helper, -1, null, (rec_device) ->
        {
            BACnetDeviceDescriptor desc = rec_device.getIdentityDescriptor(BACnetDeviceDescriptor.class);
            if (desc.equals(descTarget))
            {
                rec_existing.set(rec_device);
                return StreamHelperNextAction.Stop;
            }

            return StreamHelperNextAction.Continue_Evict;
        });

        return rec_existing.get();
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
        // Nothing to do.
    }
}
