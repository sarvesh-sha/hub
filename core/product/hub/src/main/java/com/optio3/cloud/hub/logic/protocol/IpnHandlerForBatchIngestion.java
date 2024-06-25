/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.protocol;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.logic.spooler.DiscoveredAssetsSummary;
import com.optio3.cloud.hub.logic.spooler.StagedResultsSummary;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.IpnDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.ResultStagingRecord;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.cloud.persistence.LazyRecordFlusher;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.logging.ILogger;
import com.optio3.logging.Logger;
import com.optio3.protocol.model.DeviceReachability;
import com.optio3.protocol.model.FieldModel;
import com.optio3.protocol.model.WellKnownPointClassOrCustom;
import com.optio3.protocol.model.ipn.IpnDeviceDescriptor;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;
import com.optio3.util.TimeUtils;

public class IpnHandlerForBatchIngestion extends CommonProtocolHandlerForIngestion<IpnDeviceRecord, IpnObjectModel>
{
    public static class SamplesContextImpl extends SamplesContext<IpnDeviceRecord, IpnObjectModel>
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
                    IpnObjectModel obj = IpnObjectModel.deserializeFromJson(IpnObjectModel.class, contents);

                    IpnDeviceRecord.TypeExtractor extractor = new IpnDeviceRecord.TypeExtractor();
                    TimeSeriesPropertyType        pt        = extractor.locateProperty(obj, false, forObject.identifier);
                    if (pt != null)
                    {
                        Object value = obj.getField(pt.targetField);
                        if (value != null)
                        {
                            if (pt.type == TimeSeries.SampleType.Integer)
                            {
                                value = Reflection.coerceNumber(value, Long.class);
                            }

                            addValue(DeviceElementRecord.DEFAULT_PROP_NAME, pt.type, pt.resolution, timestampEpochSeconds, pt.digitsOfPrecision, value);

                            hasValues = true;
                        }
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

    public IpnHandlerForBatchIngestion(HubConfiguration cfg,
                                       Logger logger,
                                       StagedResultsSummary.ForAsset forAsset)
    {
        super(cfg, logger, IpnDeviceRecord.class, forAsset);
    }

    @Override
    protected DiscoveredAssetsSummary.ForAsset fetchAssetSummary(BatchIngestionContext context)
    {
        return context.rootSummary.registerAsset(context.summaryForDatabase, IpnDeviceRecord.class, m_forAsset.identifier, true);
    }

    @Override
    protected LazyRecordFlusher<IpnDeviceRecord> fetchAssetRecord(BatchIngestionContext context,
                                                                  DiscoveredAssetsSummary.ForAsset assetSummary) throws
                                                                                                                 Exception
    {
        RecordHelper<IpnDeviceRecord>      helper = getHelper(context);
        LazyRecordFlusher<IpnDeviceRecord> deviceRecord;

        String sysId = assetSummary.getSysId();
        if (sysId != null)
        {
            try
            {
                IpnDeviceRecord rec = helper.get(sysId);
                return helper.wrapAsExistingRecord(rec);
            }
            catch (Throwable t)
            {
                // If the device got deleted, recreate it.
            }
        }

        IpnDeviceDescriptor desc = (IpnDeviceDescriptor) m_forAsset.identifier;

        deviceRecord = IpnDeviceRecord.ensureIdentifier(helper, context.rootRecord, desc.name);

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
        IpnDeviceRecord rec_device = ensureAssetRecord(context, false);

        if (TimeUtils.isValid(timestampEpochSeconds))
        {
            ZonedDateTime timestamp = TimeUtils.fromTimestampToUtcTime(timestampEpochSeconds);
            rec_device.setLastUpdatedDate(timestamp);

            if (contents != null)
            {
                //
                // If the Ipn_Device entity has contents, it was the result of a discovery.
                //
                rec_device.setLastCheckedDate(timestamp);
            }
        }

        if (rec_device.getName() == null)
        {
            IpnDeviceDescriptor desc = rec_device.getIdentityDescriptor(IpnDeviceDescriptor.class);
            if (desc != null)
            {
                rec_device.setPhysicalName(desc.name);
            }
        }
    }

    @Override
    protected void processAssetReachability(BatchIngestionContext context,
                                            double timestampEpochSeconds,
                                            String contents) throws
                                                             Exception
    {
        IpnDeviceRecord rec_device = ensureAssetRecord(context, false);

        MetadataMap   metadata    = rec_device.getMetadata();
        ZonedDateTime unreachable = IpnDeviceRecord.WellKnownMetadata.ipnUnresponsive.get(metadata);

        DeviceReachability report = ObjectMappers.SkipNulls.readValue(contents, DeviceReachability.class);
        if (report.reachable)
        {
            if (unreachable != null)
            {
                IpnDeviceRecord.WellKnownMetadata.ipnUnresponsive.remove(metadata);
                IpnDeviceRecord.WellKnownMetadata.ipnUnresponsiveDebounce.remove(metadata);

                // Give extra time to avoid ping-ponging between unresponsive and responsive.
                int delay = rec_device.getMinutesBeforeTransitionToReachable(metadata);
                IpnDeviceRecord.WellKnownMetadata.ipnResponsiveDebounce.put(metadata, TimeUtils.future(delay, TimeUnit.MINUTES));

                HubApplication.LoggerInstance.info("Sensor %s on network (%s / %s) responsive after %s!",
                                                   rec_device.getIdentityDescriptor(),
                                                   context.rootRecord.getSysId(),
                                                   context.rootRecord.getName(),
                                                   unreachable.toLocalDateTime());
            }
        }
        else
        {
            if (unreachable == null)
            {
                ZonedDateTime now           = TimeUtils.now();
                ZonedDateTime lastReachable = BoxingUtils.get(report.lastReachable, now);

                IpnDeviceRecord.WellKnownMetadata.ipnUnresponsive.put(metadata, lastReachable);
                IpnDeviceRecord.WellKnownMetadata.ipnResponsiveDebounce.remove(metadata);

                // Give extra time to avoid ping-ponging between responsive and unresponsive.
                int delay = rec_device.getMinutesBeforeTransitionToUnreachable(metadata);
                IpnDeviceRecord.WellKnownMetadata.ipnUnresponsiveDebounce.put(metadata, TimeUtils.future(delay, TimeUnit.MINUTES));

                HubApplication.LoggerInstance.info("Sensor %s on network (%s / %s) unresponsive since %s!",
                                                   rec_device.getIdentityDescriptor(),
                                                   context.rootRecord.getSysId(),
                                                   context.rootRecord.getName(),
                                                   lastReachable.toLocalDateTime());
            }
        }

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
            IpnDeviceRecord     rec_device = ensureAssetRecord(context, true);
            DeviceElementRecord rec_object = ensureObjectRecord(context, false);

            try
            {
                IpnObjectModel obj = IpnObjectModel.deserializeFromJson(IpnObjectModel.class, contents);

                FieldModel desc = obj.getDescriptor(rec_object.getIdentifier(), true);

                obj = IpnObjectModel.copySingleProperty(obj, desc.name);

                rec_object.setContents(IpnObjectModel.getObjectMapper(), obj);
                rec_object.setPhysicalName(desc.getDescription(obj));

                if (m_cfg.developerSettings.autoConfigureSampling && !rec_object.hasSamplingSettings())
                {
                    if (rec_object.setSamplingSettings(rec_device.prepareSamplingConfiguration(context.sessionHolder, rec_object, true)))
                    {
                        m_logger.debug("Reconfigure Sampling on (%s # %s # %s), settings changes", rec_device.getSysId(), rec_object.getSysId(), rec_object.getIdentifier());

                        m_objectReconfigureSampling = true;
                    }

                    if (WellKnownPointClassOrCustom.isValid(desc.getPointClass(obj)) && rec_object.getPointClassId() == null)
                    {
                        //
                        // Trigger reconfiguration if the object doesn't have a point class while the descriptor has one.
                        //
                        m_logger.debug("Reconfigure Sampling on (%s # %s # %s), no point class on object (should be %s)",
                                       rec_device.getSysId(),
                                       rec_object.getSysId(),
                                       rec_object.getIdentifier(),
                                       desc.getPointClass(obj));

                        m_objectReconfigureSampling = true;
                    }
                }
            }
            catch (Exception e)
            {
                m_logger.warn("Failed to analyze object '%s': %s", rec_object.getIdentifier(), e);
            }
        }
    }

    @Override
    protected SamplesContext<IpnDeviceRecord, IpnObjectModel> prepareSampleUpdateContext()
    {
        return new SamplesContextImpl();
    }

    //--//

    public static void dumpStatistics(SessionHolder holder,
                                      Logger logger) throws
                                                     Exception
    {
        class ObjectStats
        {
            GatewayDiscoveryEntity en_object;
            int                    batches;
            int                    counter;
            double                 firstTimestamp = TimeUtils.maxEpochSeconds();
            double                 lastTimestamp  = TimeUtils.minEpochSeconds();
        }

        class DeviceStats
        {
            GatewayDiscoveryEntity en_device;

            final Map<String, ObjectStats> objects = Maps.newHashMap();
        }

        class NetworkStats
        {
            GatewayDiscoveryEntity en_network;
            GatewayDiscoveryEntity en_protocol;
            int                    batchCount;
            int                    sampleCount;
            long                   size;

            final Map<String, DeviceStats> devices = Maps.newHashMap();
        }

        Map<String, NetworkStats> networks   = Maps.newHashMap();
        Set<Double>               timestamps = Sets.newHashSet();

        final RecordHelper<ResultStagingRecord> helper_staging = holder.createHelper(ResultStagingRecord.class);

        QueryHelperWithCommonFields<ResultStagingRecord, ResultStagingRecord> jh = new QueryHelperWithCommonFields<>(helper_staging, ResultStagingRecord.class);

        QueryHelperWithCommonFields.streamNoNesting(-1, jh, (rec) ->
        {
            for (GatewayDiscoveryEntity en_network : GatewayDiscoveryEntity.filter(rec.getEntities(), GatewayDiscoveryEntitySelector.Network))
            {
                NetworkStats network = networks.computeIfAbsent(en_network.selectorValue, (key) -> new NetworkStats());

                network.batchCount++;
                network.size += rec.getSizeOfEntities();

                if (network.en_network == null)
                {
                    network.en_network  = GatewayDiscoveryEntity.create(GatewayDiscoveryEntitySelector.Network, en_network.selectorValue);
                    network.en_protocol = network.en_network.createAsRequest(GatewayDiscoveryEntitySelector.Protocol, GatewayDiscoveryEntity.Protocol_Ipn);
                }

                for (GatewayDiscoveryEntity en_protocol : en_network.filter(GatewayDiscoveryEntitySelector.Protocol, GatewayDiscoveryEntity.Protocol_Ipn))
                {
                    for (GatewayDiscoveryEntity en_device : en_protocol.filter(GatewayDiscoveryEntitySelector.Ipn_Device))
                    {
                        IpnDeviceDescriptor identifier = en_device.getSelectorValueAsObject(IpnDeviceDescriptor.class);

                        DeviceStats device = network.devices.computeIfAbsent(identifier.name, (key) -> new DeviceStats());

                        if (device.en_device == null)
                        {
                            device.en_device = network.en_protocol.createAsRequest(GatewayDiscoveryEntitySelector.Ipn_Device, identifier);
                        }

                        for (GatewayDiscoveryEntity en_object : en_device.filter(GatewayDiscoveryEntitySelector.Ipn_Object))
                        {
                            ObjectStats stats = device.objects.computeIfAbsent(en_object.selectorValue, (key) -> new ObjectStats());

                            if (stats.en_object == null)
                            {
                                stats.en_object = device.en_device.createAsRequest(GatewayDiscoveryEntitySelector.Ipn_Object, en_object.selectorValue);
                            }

                            int counter = 0;

                            for (GatewayDiscoveryEntity en_sample : en_object.filter(GatewayDiscoveryEntitySelector.Ipn_ObjectSample))
                            {
                                double timestampEpoch = en_sample.getTimestampEpoch();

                                stats.firstTimestamp = Math.min(stats.firstTimestamp, timestampEpoch);
                                stats.lastTimestamp  = Math.max(stats.lastTimestamp, timestampEpoch);
                                counter++;

                                timestamps.add(timestampEpoch);

                                GatewayDiscoveryEntity req = stats.en_object.createAsRequest(GatewayDiscoveryEntitySelector.Ipn_ObjectSample, en_sample.selectorValue);
                                req.contents = en_sample.contents;

                                network.sampleCount++;
                            }

                            if (counter != 0)
                            {
                                stats.counter += counter;
                                stats.batches++;
                            }
                        }
                    }
                }
            }

            return StreamHelperNextAction.Continue_Evict;
        });

        logger.debug("Total timestamps: %d\n", timestamps.size());

        List<String> networksList = Lists.newArrayList(networks.keySet());
        networksList.sort(String::compareTo);

        for (String networkId : networksList)
        {
            NetworkStats network = networks.get(networkId);

            List<String> devices = Lists.newArrayList(network.devices.keySet());
            devices.sort(String::compareTo);

            logger.info("%s # Batches: %,4d # Samples: %,10d # Batches Total Size: %,10d # Bytes/sample: %d # Uncompressed As Single Batch: %,10d # Compressed As Single Batch: %,10d",
                        networkId,
                        network.batchCount,
                        network.sampleCount,
                        network.size,
                        network.size / Math.max(1, network.sampleCount),
                        ObjectMappers.SkipNulls.writeValueAsBytes(network.en_protocol).length,
                        ObjectMappers.serializeToGzip(network.en_protocol).length);

            for (String deviceId : devices)
            {
                DeviceStats device = network.devices.get(deviceId);

                List<String> objectsList = Lists.newArrayList(device.objects.keySet());
                objectsList.sort(String::compareTo);

                for (String objectId : objectsList)
                {
                    ObjectStats stats = device.objects.get(objectId);
                    if (stats != null && stats.counter > 0)
                    {
                        logger.info("%s # %-60s # %-60s # %,8d %,8d # %s # %s",
                                    networkId,
                                    deviceId,
                                    objectId,
                                    stats.batches,
                                    stats.counter,
                                    TimeUtils.DEFAULT_FORMATTER_NO_MILLI.format(TimeUtils.fromTimestampToUtcTime(stats.firstTimestamp)),
                                    TimeUtils.DEFAULT_FORMATTER_NO_MILLI.format(TimeUtils.fromTimestampToUtcTime(stats.lastTimestamp)));
                    }
                }
            }
        }
    }
}
