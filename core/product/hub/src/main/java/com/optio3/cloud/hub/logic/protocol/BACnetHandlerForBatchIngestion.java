/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.protocol;

import java.time.ZonedDateTime;
import java.util.BitSet;

import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.logic.spooler.DiscoveredAssetsSummary;
import com.optio3.cloud.hub.logic.spooler.StagedResultsSummary;
import com.optio3.cloud.hub.persistence.asset.BACnetDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.cloud.persistence.LazyRecordFlusher;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.logging.ILogger;
import com.optio3.logging.Logger;
import com.optio3.protocol.model.DeviceReachability;
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.objects.device;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.serialization.TypedBitSet;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class BACnetHandlerForBatchIngestion extends CommonProtocolHandlerForIngestion<BACnetDeviceRecord, BACnetObjectModel>
{
    public static class SamplesContextImpl extends SamplesContext<BACnetDeviceRecord, BACnetObjectModel>
    {
        @Override
        public boolean processSampleUpdate(ILogger logger,
                                           BatchIngestionContext context,
                                           StagedResultsSummary.ForAsset forAsset,
                                           StagedResultsSummary.ForObject forObject,
                                           double timestampEpochSeconds,
                                           String contents)
        {
            boolean hasValues = false;

            try
            {
                if (contents != null)
                {
                    BACnetObjectModel obj = BACnetObjectModel.deserializeFromJson(BACnetObjectModel.class, contents);

                    for (BACnetPropertyIdentifierOrUnknown propId : obj.getAccessedProperties())
                    {
                        Object value = obj.getValue(propId, null);
                        if (value == null)
                        {
                            continue; // Let's treat this as a missing value.
                        }

                        TypedBitSet<?> bs = Reflection.as(value, TypedBitSet.class);
                        if (bs != null)
                        {
                            value = bs.toBitSet();
                        }

                        String prop = propId.toJsonValue();

                        if (value instanceof BitSet)
                        {
                            addValue(prop, TimeSeries.SampleType.BitSet, TimeSeries.SampleResolution.Max1Hz, timestampEpochSeconds, 0, value);
                            hasValues = true;
                            continue;
                        }

                        if (value instanceof Enum)
                        {
                            addValue(prop, TimeSeries.SampleType.Enumerated, TimeSeries.SampleResolution.Max1Hz, timestampEpochSeconds, 0, value);
                            hasValues = true;
                            continue;
                        }

                        TypeDescriptor td = Reflection.getDescriptor(value.getClass());
                        if (td != null)
                        {
                            if (td.isFloatingType())
                            {
                                addValue(prop, TimeSeries.SampleType.Decimal, TimeSeries.SampleResolution.Max1Hz, timestampEpochSeconds, 0, value);
                                hasValues = true;
                                continue;
                            }

                            addValue(prop, TimeSeries.SampleType.Integer, TimeSeries.SampleResolution.Max1Hz, timestampEpochSeconds, 0, td.asLongValue(value));
                            hasValues = true;
                            continue;
                        }

                        logger.error("Can't decode sample for property '%s/%s/%s', due to type '%s'", forAsset.identifier, forObject.identifier, prop, value.getClass());
                    }
                }
            }
            catch (Exception e)
            {
                logger.error("Failed to process sample '%s / %s': %s", TimeUtils.fromTimestampToUtcTime(timestampEpochSeconds), contents, e);
            }

            return hasValues;
        }

        @Override
        public void postProcessSampleUpdate(DeviceElementRecord.ArchiveHolder archiveHolder)
        {
            // TODO: purge samples older than retention window.
        }
    }

    //--//

    public BACnetHandlerForBatchIngestion(HubConfiguration cfg,
                                          Logger logger,
                                          StagedResultsSummary.ForAsset forAsset)
    {
        super(cfg, logger, BACnetDeviceRecord.class, forAsset);
    }

    @Override
    protected DiscoveredAssetsSummary.ForAsset fetchAssetSummary(BatchIngestionContext context)
    {
        return context.rootSummary.registerAsset(context.summaryForDatabase, BACnetDeviceRecord.class, m_forAsset.identifier, true);
    }

    @Override
    protected LazyRecordFlusher<BACnetDeviceRecord> fetchAssetRecord(BatchIngestionContext context,
                                                                     DiscoveredAssetsSummary.ForAsset assetSummary) throws
                                                                                                                    Exception
    {
        RecordHelper<BACnetDeviceRecord>      helper = getHelper(context);
        LazyRecordFlusher<BACnetDeviceRecord> deviceRecord;

        String sysId = assetSummary.getSysId();
        if (sysId != null)
        {
            try
            {
                BACnetDeviceRecord rec = helper.get(sysId);
                return helper.wrapAsExistingRecord(rec);
            }
            catch (Throwable t)
            {
                // If the device got deleted, recreate it.
            }
        }

        BACnetDeviceDescriptor desc = (BACnetDeviceDescriptor) m_forAsset.identifier;

        deviceRecord = BACnetDeviceRecord.ensureDeviceFromDescriptor(helper, context.rootRecord, desc);
        context.markInsert(1);
        return deviceRecord;
    }

    //--//

    @Override
    protected void processAssetUpdate(BatchIngestionContext context,
                                      double timestampEpochSeconds,
                                      String contents) throws
                                                       Exception
    {
        if (TimeUtils.isValid(timestampEpochSeconds))
        {
            BACnetDeviceRecord rec_device = ensureAssetRecord(context, false);
            rec_device.setLastUpdatedDate(TimeUtils.fromTimestampToUtcTime(timestampEpochSeconds));
        }

        if (contents != null)
        {
            BACnetDeviceRecord rec_device = ensureAssetRecord(context, false);
            rec_device.setIdentityDescriptor(m_forAsset.identifier);

            //
            // If the BACnet_Device entity has contents, it was the result of a discovery.
            //
            rec_device.setLastCheckedDate(TimeUtils.now());
        }
    }

    @Override
    protected void processAssetReachability(BatchIngestionContext context,
                                            double timestampEpochSeconds,
                                            String contents) throws
                                                             Exception
    {
        BACnetDeviceRecord rec_device = ensureAssetRecord(context, false);

        MetadataMap metadata = rec_device.getMetadata();

        var state = BACnetDeviceRecord.WellKnownMetadata.bacnetReachability.get(metadata);

        DeviceReachability report = ObjectMappers.SkipNulls.readValue(contents, DeviceReachability.class);
        if (report.reachable)
        {
            if (state.reachable == null)
            {
                state.reachable   = TimeUtils.now();
                state.unreachable = null;
                HubApplication.LoggerInstance.info("Device %s reachable!", rec_device.getIdentityDescriptor());
            }
        }
        else
        {
            if (state.unreachable == null)
            {
                ZonedDateTime lastReachable = report.lastReachable;
                if (lastReachable == null)
                {
                    lastReachable = TimeUtils.now();
                }

                state.unreachable     = lastReachable;
                state.reachable       = null;
                state.warningDebounce = null;
                HubApplication.LoggerInstance.info("Device %s unreachable since %s!", rec_device.getIdentityDescriptor(), lastReachable);
            }
        }

        BACnetDeviceRecord.WellKnownMetadata.bacnetReachability.put(metadata, state);

        rec_device.setMetadata(metadata);
    }

    @Override
    protected void processObjectUpdate(BatchIngestionContext context,
                                       double timestampEpochSeconds,
                                       String contents) throws
                                                        Exception
    {
        if (TimeUtils.isValid(timestampEpochSeconds))
        {
            DeviceElementRecord rec_object = ensureObjectRecord(context, false);

            rec_object.setLastUpdatedDate(TimeUtils.fromTimestampToUtcTime(timestampEpochSeconds));
        }

        if (contents != null)
        {
            BACnetDeviceRecord  rec_device = ensureAssetRecord(context, true);
            DeviceElementRecord rec_object = ensureObjectRecord(context, false);

            try
            {
                BACnetObjectModel obj = BACnetObjectModel.deserializeFromJson(BACnetObjectModel.class, contents);
                rec_object.setContents(BACnetObjectModel.getObjectMapper(), obj);

                if (m_cfg.developerSettings.autoConfigureSampling && !rec_object.hasSamplingSettings())
                {
                    rec_object.setSamplingSettings(rec_device.prepareSamplingConfiguration(context.sessionHolder, rec_object, true));
                }

                if (StringUtils.isEmpty(rec_object.getPhysicalName()))
                {
                    String name = obj.extractName(true);
                    if (StringUtils.isNotBlank(name))
                    {
                        rec_object.setPhysicalName(name);
                    }
                }

                device model = Reflection.as(obj, device.class);
                if (model != null)
                {
                    if (StringUtils.isEmpty(rec_device.getPhysicalName()))
                    {
                        String name = obj.extractName(true);
                        if (StringUtils.isNotBlank(name))
                        {
                            rec_device.setPhysicalName(name);
                        }
                    }

                    if (StringUtils.isNotBlank(model.vendor_name))
                    {
                        rec_device.setManufacturerName(model.vendor_name);
                    }

                    if (StringUtils.isNotBlank(model.model_name))
                    {
                        rec_device.setProductName(model.model_name);
                    }

                    if (StringUtils.isNotBlank(model.firmware_revision))
                    {
                        rec_device.setFirmwareVersion(model.firmware_revision);
                    }

                    rec_device.setHintForDeviceObject(rec_object);
                }
            }
            catch (Exception e)
            {
                m_logger.warn("Failed to analyze object '%s': %s", rec_object.getIdentifier(), e);
            }
        }
    }

    @Override
    protected SamplesContext<BACnetDeviceRecord, BACnetObjectModel> prepareSampleUpdateContext()
    {
        return new SamplesContextImpl();
    }
}
