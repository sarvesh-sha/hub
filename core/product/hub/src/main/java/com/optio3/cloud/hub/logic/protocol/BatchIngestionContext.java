/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.protocol;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.logic.spooler.DiscoveredAssetsSummary;
import com.optio3.cloud.hub.logic.spooler.ResultStagingSpooler;
import com.optio3.cloud.hub.logic.spooler.StagedResultsProcessingMode;
import com.optio3.cloud.hub.logic.spooler.StagedResultsSummary;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementSampleRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.ResultStagingRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;

public class BatchIngestionContext
{
    public final SessionHolder                           sessionHolder;
    public final RecordHelper<ResultStagingRecord>       helper_staging;
    public final RecordHelper<AssetRecord>               helper_asset;
    public final RecordHelper<DeviceRecord>              helper_device;
    public final RecordHelper<DeviceElementRecord>       helper_element;
    public final RecordHelper<DeviceElementSampleRecord> helper_archive;

    public final HubConfiguration cfg;

    private final AtomicBoolean m_keepRunning; // Set to false when the application is shutting down.
    private final AtomicBoolean m_yieldControl; // Set to true when another thread wants to step in.
    private final AtomicInteger m_databaseActivity; // Set to non-zero when new database activity has been detected.
    private final int           m_batchThreshold;

    //--//

    public final DiscoveredAssetsSummary         summaryForDatabase;
    public final StagedResultsSummary            summaryForPending;
    public final StagedResultsProcessingMode     mode;
    public final DiscoveredAssetsSummary.ForRoot rootSummary;
    public final AssetRecord                     rootRecord;

    //--//

    private int     m_processedInserts;
    private int     m_processedUpdates;
    private boolean m_madeProgress;
    private boolean m_shouldReschedule;

    //--//

    public BatchIngestionContext(SessionHolder sessionHolder,
                                 DiscoveredAssetsSummary summaryForDatabase,
                                 StagedResultsSummary summaryForPending,
                                 AtomicBoolean keepRunning,
                                 AtomicInteger databaseActivity,
                                 AtomicBoolean yieldControl,
                                 StagedResultsProcessingMode mode,
                                 int batchThreshold,
                                 DiscoveredAssetsSummary.ForRoot rootSummary,
                                 AssetRecord rootRecord)
    {
        this.sessionHolder = requireNonNull(sessionHolder);
        helper_staging     = sessionHolder.createHelper(ResultStagingRecord.class);
        helper_asset       = sessionHolder.createHelper(AssetRecord.class);
        helper_device      = sessionHolder.createHelper(DeviceRecord.class);
        helper_element     = sessionHolder.createHelper(DeviceElementRecord.class);
        helper_archive     = sessionHolder.createHelper(DeviceElementSampleRecord.class);

        cfg = sessionHolder.getServiceNonNull(HubConfiguration.class);

        this.summaryForDatabase = summaryForDatabase;
        this.summaryForPending  = requireNonNull(summaryForPending);

        m_keepRunning      = requireNonNull(keepRunning);
        m_databaseActivity = requireNonNull(databaseActivity);
        m_yieldControl     = requireNonNull(yieldControl);
        m_batchThreshold   = batchThreshold;

        this.mode        = mode;
        this.rootSummary = requireNonNull(rootSummary);
        this.rootRecord  = requireNonNull(rootRecord);
    }

    public void flushIfNeeded()
    {
        if (m_processedInserts + m_processedUpdates > m_batchThreshold)
        {
            flush();

            sessionHolder.commitAndBeginNewTransaction();
        }
    }

    public void flush()
    {
        ResultStagingSpooler.LoggerInstance.debug("Flushing ingestion batch: START : %d insert(s) and %d update(s)", m_processedInserts, m_processedUpdates);
        summaryForPending.flush(ResultStagingSpooler.LoggerInstance, helper_staging);
        ResultStagingSpooler.LoggerInstance.debug("Flushing ingestion batch: END   : %d insert(s) and %d update(s)", m_processedInserts, m_processedUpdates);
        m_processedInserts = 0;
        m_processedUpdates = 0;
    }

    public void markInsert(int num)
    {
        if (num > 0)
        {
            m_processedInserts += num;
            m_madeProgress = true;
        }
    }

    public void markUpdate(int num)
    {
        if (num > 0)
        {
            m_processedUpdates += num;
            m_madeProgress = true;
        }
    }

    public boolean hasMadeProgress()
    {
        return m_madeProgress;
    }

    public boolean shouldReschedule()
    {
        if (m_shouldReschedule)
        {
            return true;
        }

        if (!m_keepRunning.get())
        {
            // Shutdown...
            return true;
        }

        if (m_yieldControl.compareAndSet(true, false))
        {
            m_shouldReschedule = true;
            return true;
        }

        return m_databaseActivity.get() != 0;
    }
}
