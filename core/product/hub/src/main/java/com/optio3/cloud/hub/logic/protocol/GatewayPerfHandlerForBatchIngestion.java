/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.protocol;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.logic.spooler.DiscoveredAssetsSummary;
import com.optio3.cloud.hub.logic.spooler.StagedResultsSummary;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.cloud.persistence.LazyRecordFlusher;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.infra.NetworkHelper;
import com.optio3.infra.WellKnownSites;
import com.optio3.logging.ILogger;
import com.optio3.logging.Logger;
import com.optio3.protocol.model.GatewayPerformanceCounters;
import com.optio3.serialization.Reflection;
import com.optio3.util.TimeUtils;

public class GatewayPerfHandlerForBatchIngestion extends CommonProtocolHandlerForIngestion<AssetRecord, GatewayPerformanceCounters>
{
    public static class SamplesContextImpl extends SamplesContext<AssetRecord, GatewayPerformanceCounters>
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
                    GatewayPerformanceCounters obj = GatewayPerformanceCounters.deserializeFromJson(contents);

                    GatewayAssetRecord.TypeExtractor    extractor = new GatewayAssetRecord.TypeExtractor();
                    Map<String, TimeSeriesPropertyType> map       = extractor.classifyModel(obj, false);
                    for (TimeSeriesPropertyType pt : map.values())
                    {
                        Object value = obj.getField(pt.targetField);
                        if (value != null)
                        {
                            value = Reflection.coerceNumber(value, Double.class);

                            addValue(pt.targetField, pt.type, TimeSeries.SampleResolution.Max1Hz, timestampEpochSeconds, 0, value);

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
            try
            {
                ZonedDateTime now             = TimeUtils.now();
                ZonedDateTime retentionWindow = now.minus(60, ChronoUnit.DAYS);
                archiveHolder.deleteSamplesOlderThan(retentionWindow);
            }
            catch (Exception e)
            {
                // Ignore failures.
            }
        }
    }

    //--//

    public GatewayPerfHandlerForBatchIngestion(HubConfiguration cfg,
                                               Logger logger,
                                               StagedResultsSummary.ForAsset forAsset)
    {
        super(cfg, logger, AssetRecord.class, forAsset);
    }

    @Override
    protected DiscoveredAssetsSummary.ForAsset fetchAssetSummary(BatchIngestionContext context)
    {
        return context.rootSummary.registerAsset(context.summaryForDatabase, GatewayAssetRecord.class, m_forAsset.identifier, true);
    }

    @Override
    protected LazyRecordFlusher<AssetRecord> fetchAssetRecord(BatchIngestionContext context,
                                                              DiscoveredAssetsSummary.ForAsset assetSummary)
    {
        // We place all the objects under the Gateway record.
        RecordHelper<AssetRecord> helper = getHelper(context);
        return helper.wrapAsExistingRecord(context.rootRecord);
    }

    //--//

    @Override
    protected void processAssetUpdate(BatchIngestionContext context,
                                      double timestampEpochSeconds,
                                      String contents)
    {
        // Nothing to do.
    }

    @Override
    protected void processAssetReachability(BatchIngestionContext context,
                                            double timestampEpochSeconds,
                                            String contents)
    {
        // Nothing to do.
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

            final String objectIdentifier = getObjectIdentifier();

            try
            {
                switch (objectIdentifier)
                {
                    case "Global":
                        rec_object.setPhysicalName(objectIdentifier);
                        break;

                    default:
                        InetAddress address = InetAddress.getByName(objectIdentifier);
                        String canonical = lookupAddress(address);

                        rec_object.setPhysicalName(canonical);
                        break;
                }
            }
            catch (Exception e)
            {
                m_logger.debug("Failed to resolve host '%s', due to %s", objectIdentifier, e);

                // Ignore resolution issues.
                rec_object.setPhysicalName(objectIdentifier);
            }
        }
    }

    private String lookupAddress(InetAddress address) throws
                                                      UnknownHostException
    {
        URI fullPath = m_cfg.parseConnectionUrl(null);

        InetAddress localhost = InetAddress.getByName(fullPath.getHost());
        if (localhost.equals(address))
        {
            return "Hub";
        }

        InetAddress builder = InetAddress.getByName(WellKnownSites.builderServer());
        if (builder.equals(address))
        {
            return "Builder";
        }

        String name = address.getCanonicalHostName();

        if (NetworkHelper.isNonRoutableRange(address) != null)
        {
            name += " (non-routable)";
        }

        return name;
    }

    @Override
    protected SamplesContext<AssetRecord, GatewayPerformanceCounters> prepareSampleUpdateContext()
    {
        return new SamplesContextImpl();
    }
}
