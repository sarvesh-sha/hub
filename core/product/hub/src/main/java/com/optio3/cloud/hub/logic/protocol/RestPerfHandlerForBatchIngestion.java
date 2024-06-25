/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.protocol;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.logic.spooler.DiscoveredAssetsSummary;
import com.optio3.cloud.hub.logic.spooler.StagedResultsSummary;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.hub.persistence.HostAssetRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.persistence.LazyRecordFlusher;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.logging.ILogger;
import com.optio3.logging.Logger;
import com.optio3.protocol.model.RestPerformanceCounters;
import com.optio3.serialization.Reflection;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class RestPerfHandlerForBatchIngestion extends CommonProtocolHandlerForIngestion<AssetRecord, RestPerformanceCounters>
{
    public static class SamplesContextImpl extends SamplesContext<AssetRecord, RestPerformanceCounters>
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
                    RestPerformanceCounters obj = RestPerformanceCounters.deserializeFromJson(contents);

                    HostAssetRecord.TypeExtractor       extractor = new HostAssetRecord.TypeExtractor();
                    Map<String, TimeSeriesPropertyType> map       = extractor.classifyModel(obj, false);
                    for (TimeSeriesPropertyType pt : map.values())
                    {
                        Object value = obj.getField(pt.targetField);
                        if (value != null)
                        {
                            value = Reflection.coerceNumber(value, Long.class);

                            addValue(pt.targetField, pt.type, pt.resolution, timestampEpochSeconds, 0, value);

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
                ZonedDateTime retentionWindow = now.minus(30, ChronoUnit.DAYS);
                archiveHolder.deleteSamplesOlderThan(retentionWindow);
            }
            catch (Exception e)
            {
                // Ignore failures.
            }
        }
    }

    //--//

    public RestPerfHandlerForBatchIngestion(HubConfiguration cfg,
                                            Logger logger,
                                            StagedResultsSummary.ForAsset forAsset)
    {
        super(cfg, logger, AssetRecord.class, forAsset);
    }

    @Override
    protected DiscoveredAssetsSummary.ForAsset fetchAssetSummary(BatchIngestionContext context)
    {
        return context.rootSummary.registerAsset(context.summaryForDatabase, HostAssetRecord.class, m_forAsset.identifier, true);
    }

    @Override
    protected LazyRecordFlusher<AssetRecord> fetchAssetRecord(BatchIngestionContext context,
                                                              DiscoveredAssetsSummary.ForAsset assetSummary)
    {
        // We place all the objects under the Host record.
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

            if (StringUtils.isEmpty(rec_object.getName()))
            {
                rec_object.setPhysicalName(getObjectIdentifier());
            }
        }
    }

    @Override
    protected SamplesContext<AssetRecord, RestPerformanceCounters> prepareSampleUpdateContext()
    {
        return new SamplesContextImpl();
    }
}
