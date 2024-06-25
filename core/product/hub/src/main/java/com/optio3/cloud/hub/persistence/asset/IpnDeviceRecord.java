/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.asset;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.ZonedDateTime;
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
import com.optio3.cloud.hub.logic.protocol.IProtocolDecoder;
import com.optio3.cloud.hub.logic.protocol.IpnDecoder;
import com.optio3.cloud.hub.model.asset.DeviceElementSampling;
import com.optio3.cloud.hub.model.asset.IpnDevice;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesEnumeratedValue;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LazyRecordFlusher;
import com.optio3.cloud.persistence.MetadataField;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.protocol.model.EngineeringUnitsFamily;
import com.optio3.protocol.model.FieldModel;
import com.optio3.protocol.model.ipn.IpnDeviceDescriptor;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.serialization.TypedBitSet;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.search.annotations.Indexed;

@Entity
@Table(name = "ASSET_DEVICE_IPN")
@DynamicUpdate // Due to HHH-11506
@Indexed()
@Optio3TableInfo(externalId = "IpnDevice", model = IpnDevice.class, metamodel = IpnDeviceRecord_.class, metadata = IpnDeviceRecord.WellKnownMetadata.class)
public class IpnDeviceRecord extends DeviceRecord implements AssetRecord.IProviderOfPropertyTypeExtractorClass
{
    public static class WellKnownMetadata implements Optio3TableInfo.IMetadataDigest
    {
        public static final MetadataField<ZonedDateTime> ipnUnresponsive         = new MetadataField<>("ipnUnresponsive", ZonedDateTime.class);
        public static final MetadataField<ZonedDateTime> ipnWarning              = new MetadataField<>("ipnWarning", ZonedDateTime.class);
        public static final MetadataField<ZonedDateTime> ipnUnresponsiveDebounce = new MetadataField<>("ipnUnresponsiveDebounce", ZonedDateTime.class);
        public static final MetadataField<ZonedDateTime> ipnResponsiveDebounce   = new MetadataField<>("ipnResponsiveDebounce", ZonedDateTime.class);

        public static final MetadataField<ZonedDateTime> infiniteImpulseLastPull = new MetadataField<>("infiniteImpulseLastPull", ZonedDateTime.class);
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
                final IpnObjectModel obj = getContentsAsObject(rec, false);

                TimeSeriesPropertyType pt = locateProperty(obj, handlePresentationType, rec.getIdentifier());
                if (pt != null)
                {
                    res.put(DeviceElementRecord.DEFAULT_PROP_NAME, pt);
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
            IpnObjectModel objTyped = Reflection.as(obj, IpnObjectModel.class);
            if (objTyped != null)
            {
                for (FieldModel fieldModel : obj.getDescriptors())
                {
                    final EngineeringUnits units = fieldModel.getUnits(obj);
                    if (units == EngineeringUnits.no_units)
                    {
                        // Skip fields with no units.
                        continue;
                    }

                    final TimeSeriesPropertyType pt = new TimeSeriesPropertyType();
                    pt.name              = DeviceElementRecord.DEFAULT_PROP_NAME;
                    pt.displayName       = fieldModel.getDescription(obj);
                    pt.resolution        = TimeSeries.SampleResolution.convert(fieldModel.temporalResolution);
                    pt.digitsOfPrecision = fieldModel.digitsOfPrecision;
                    pt.debounceSeconds   = fieldModel.debounceSeconds;
                    pt.noValueMarker     = fieldModel.noValueMarker;
                    pt.indexed           = fieldModel.indexed;
                    pt.setUnits(units);

                    Type fieldType = fieldModel.type;
                    pt.expectedType = fieldType;
                    pt.targetField  = pt.name;

                    if (units.getFamily() == EngineeringUnitsFamily.Enumerated)
                    {
                        if (Reflection.canAssignTo(TypedBitSet.class, fieldType))
                        {
                            Class<?> clzField = Reflection.getRawType(fieldType);
                            Class<?> enumType = Reflection.searchTypeArgument(TypedBitSet.class, clzField, 0);
                            pt.tryToExtractEnumValues(enumType);

                            if (enumType != null)
                            {
                                pt.expectedType = enumType;
                            }

                            pt.type = TimeSeries.SampleType.BitSet;
                        }
                        else if (Reflection.canAssignTo(String[].class, fieldType))
                        {
                            pt.type = TimeSeries.SampleType.EnumeratedSet;
                        }
                        else
                        {
                            pt.type = TimeSeries.SampleType.Enumerated;

                            int pos = 0;

                            for (String value : fieldModel.getEnumeratedValues(obj))
                            {
                                TimeSeriesEnumeratedValue newEnumValue = pt.addEnumValue();
                                newEnumValue.name  = value;
                                newEnumValue.value = pos++;
                            }
                        }
                    }
                    else if (handlePresentationType && fieldModel.desiredTypeForSamples != Object.class && pt.tryToExtractEnumValues(fieldModel.desiredTypeForSamples))
                    {
                        pt.expectedType = fieldModel.desiredTypeForSamples;
                        pt.type         = TimeSeries.SampleType.Enumerated;
                    }
                    else
                    {
                        TypeDescriptor td = Reflection.getDescriptor(fieldType);
                        if (td == null)
                        {
                            // If the fields is not numeric or enumerated, skip it.
                            continue;
                        }

                        boolean isBoolean = td.clz == boolean.class;

                        if (td.isFloatingType())
                        {
                            pt.type = TimeSeries.SampleType.Decimal;
                        }
                        else if (handlePresentationType && isBoolean)
                        {
                            pt.type = TimeSeries.SampleType.Enumerated;

                            TimeSeriesEnumeratedValue newEnumValue;

                            newEnumValue       = pt.addEnumValue();
                            newEnumValue.name  = "Off";
                            newEnumValue.value = 0;

                            newEnumValue       = pt.addEnumValue();
                            newEnumValue.name  = "On";
                            newEnumValue.value = 1;
                        }
                        else
                        {
                            pt.type = TimeSeries.SampleType.Integer;

                            pt.isBoolean = isBoolean;
                        }
                    }

                    map.put(fieldModel.name, pt);
                }
            }
        }

        @Override
        public IProtocolDecoder getProtocolDecoder()
        {
            return new IpnDecoder();
        }

        @Override
        public EngineeringUnitsFactors getUnitsFactors(DeviceElementRecord rec)
        {
            try
            {
                final IpnObjectModel obj = getContentsAsObject(rec, false);
                if (obj != null)
                {
                    TimeSeriesPropertyType pt = locateProperty(obj, true, rec.getIdentifier());
                    if (pt != null)
                    {
                        return pt.unitsFactors;
                    }
                }
            }
            catch (Exception e)
            {
                HubApplication.LoggerInstance.error("Encountered a problem while trying to extract units factors of object '%s': %s", e, rec.getIdentifier());
            }

            return null;
        }

        @Override
        public String getIndexedValue(DeviceElementRecord rec)
        {
            try
            {
                final IpnObjectModel obj = getContentsAsObject(rec, false);
                if (obj != null)
                {
                    TimeSeriesPropertyType pt = locateProperty(obj, true, rec.getIdentifier());
                    if (pt != null && pt.indexed)
                    {
                        Object val = obj.getField(pt.targetField);

                        Object optVal = pt.tryToResolveEnumValue(val);
                        if (optVal != null)
                        {
                            val = optVal;
                        }

                        if (val instanceof Enum)
                        {
                            Enum typedValue = (Enum) val;
                            val = typedValue.name();
                        }

                        if (val instanceof String)
                        {
                            return (String) val;
                        }

                        if (val instanceof String[])
                        {
                            String[] typedValue = (String[]) val;
                            return String.join(", ", typedValue);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                HubApplication.LoggerInstance.error("Encountered a problem while trying to extract the indexed value of object '%s': %s", e, rec.getIdentifier());
            }

            return null;
        }

        @Override
        public IpnObjectModel getContentsAsObject(DeviceElementRecord rec,
                                                  boolean desiredState) throws
                                                                        IOException
        {
            ObjectMapper om = IpnObjectModel.getObjectMapper();

            if (desiredState)
            {
                return rec.getTypedDesiredContents(om, IpnObjectModel.class);
            }
            else
            {
                return rec.getTypedContents(om, IpnObjectModel.class);
            }
        }
    }

    //--//

    public IpnDeviceRecord()
    {
    }

    @Override
    public boolean isReachable()
    {
        return getMetadata(WellKnownMetadata.ipnUnresponsive) == null;
    }

    @Override
    protected int getDefaultMinutesBeforeTransitionToReachable()
    {
        return 30;
    }

    //--//

    @Override
    public List<DeviceElementSampling> prepareSamplingConfiguration(SessionHolder sessionHolder,
                                                                    DeviceElementRecord rec_obj,
                                                                    boolean checkNonZeroValue) throws
                                                                                               IOException
    {
        List<DeviceElementSampling> config = Lists.newArrayList();

        InstanceConfiguration cfg = sessionHolder.getServiceNonNull(InstanceConfiguration.class);
        if (!cfg.prepareSamplingConfiguration(sessionHolder, this, rec_obj, checkNonZeroValue, config))
        {
            final int samplingPeriod = getParentAsset(NetworkAssetRecord.class).getSamplingPeriod();

            DeviceElementSampling.add(config, DeviceElementRecord.DEFAULT_PROP_NAME, samplingPeriod);
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

        super.removeInner(validation, helper);
    }

    //--//

    @Override
    public GatewayDiscoveryEntity createRequest(GatewayDiscoveryEntity en_protocol)
    {
        return en_protocol.createAsRequest(GatewayDiscoveryEntitySelector.Ipn_Device, getIdentityDescriptor());
    }

    //--//

    public static LazyRecordFlusher<IpnDeviceRecord> ensureIdentifier(RecordHelper<IpnDeviceRecord> helper,
                                                                      AssetRecord parent,
                                                                      String deviceIdentifier) throws
                                                                                               Exception
    {
        IpnDeviceRecord rec_device = findByIdentifier(helper, parent, deviceIdentifier);
        if (rec_device == null)
        {
            rec_device = new IpnDeviceRecord();

            IpnDeviceDescriptor desc = new IpnDeviceDescriptor();
            desc.name = deviceIdentifier;
            rec_device.setIdentityDescriptor(desc);

            return helper.wrapAsNewRecord(rec_device, (rec_device2) -> rec_device2.linkToParent(helper, parent));
        }
        else
        {
            RecordLocked<IpnDeviceRecord> lock_device = helper.optimisticallyUpgradeToLocked(rec_device, 10, TimeUnit.SECONDS);
            return helper.wrapAsExistingRecord(lock_device.get());
        }
    }

    public static IpnDeviceRecord findByIdentifier(RecordHelper<IpnDeviceRecord> helper,
                                                   AssetRecord parent,
                                                   String deviceIdentifier) throws
                                                                            Exception
    {
        AtomicReference<IpnDeviceRecord> rec_existing = new AtomicReference<>();

        parent.enumerateChildrenNoNesting(helper, -1, null, (rec_device) ->
        {
            IpnDeviceDescriptor desc = rec_device.getIdentityDescriptor(IpnDeviceDescriptor.class);
            if (desc != null && StringUtils.equals(desc.name, deviceIdentifier))
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
