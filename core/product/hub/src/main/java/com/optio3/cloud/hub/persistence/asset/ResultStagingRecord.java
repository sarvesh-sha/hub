/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.asset;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.criteria.Predicate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.persistence.DeleteHelperWithCommonFields;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.cloud.persistence.StreamHelperResult;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.BoxingUtils;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.FunctionWithException;

@Entity
@Table(name = "RESULT_STAGING", indexes = { @Index(columnList = "sys_created_on") })
@Optio3TableInfo(externalId = "ResultStaging", model = BaseModel.class, metamodel = ResultStagingRecord_.class)
public class ResultStagingRecord extends RecordWithCommonFields
{
    private static final TypeReference<List<GatewayDiscoveryEntity>> s_typeReference = new TypeReference<List<GatewayDiscoveryEntity>>()
    {
    };

    //--//

    @Column(name = "range_start", nullable = false)
    private ZonedDateTime rangeStart;

    @Column(name = "range_end", nullable = false)
    private ZonedDateTime rangeEnd;

    @Column(name = "objects_processed", nullable = false)
    private boolean objectsProcessed;

    @Column(name = "samples_processed", nullable = false)
    private boolean samplesProcessed;

    @Column(name = "samples_count", nullable = false)
    private int samplesCount;

    @Lob
    @Column(name = "contents")
    private byte[] contents;

    //--//

    public ResultStagingRecord()
    {
    }

    @Override
    public void onEviction()
    {
        super.onEviction();

        contents = null;
    }

    //--//

    public ZonedDateTime getRangeStart()
    {
        return rangeStart;
    }

    public ZonedDateTime getRangeEnd()
    {
        return rangeEnd;
    }

    public boolean coversTimestamp(ZonedDateTime timestamp)
    {
        if (rangeStart.isAfter(timestamp))
        {
            // Timestamp before the start of the archive.
            return false;
        }

        // True if end is after or equal to timestamp.
        return !rangeEnd.isBefore(timestamp);
    }

    public int getSizeOfEntities()
    {
        return contents != null ? contents.length : 0;
    }

    public List<GatewayDiscoveryEntity> getEntities()
    {
        return getEntities(contents);
    }

    public static List<GatewayDiscoveryEntity> getEntities(byte[] contents)
    {
        return ObjectMappers.deserializeFromGzip(contents, s_typeReference);
    }

    public boolean getObjectsProcessed()
    {
        return objectsProcessed;
    }

    public void setObjectsProcessed(boolean objectsProcessed)
    {
        this.objectsProcessed = objectsProcessed;
    }

    public boolean getSamplesProcessed()
    {
        return samplesProcessed;
    }

    public void setSamplesProcessed(boolean samplesProcessed)
    {
        this.samplesProcessed = samplesProcessed;
    }

    public int getNumberOfSamples()
    {
        return samplesCount;
    }

    public boolean hasSamples()
    {
        return samplesCount > 0;
    }

    //--//

    public static void queue(RecordHelper<ResultStagingRecord> helper,
                             List<GatewayDiscoveryEntity> entities)
    {
        class AggregationHelper
        {
            private final ResultStagingRecord m_rec;
            private       double              m_rangeStart = TimeUtils.maxEpochSeconds();
            private       double              m_rangeEnd   = TimeUtils.minEpochSeconds();

            private AggregationHelper(ResultStagingRecord rec)
            {
                m_rec = rec;

                // We start with the assumption that the values don't need processing.
                rec.objectsProcessed = true;
                rec.samplesProcessed = true;
            }

            private void aggregateTimeStamps(List<GatewayDiscoveryEntity> entities)
            {
                if (entities != null)
                {
                    for (GatewayDiscoveryEntity entity : entities)
                    {
                        final double timestamp    = entity.getTimestampEpoch();
                        boolean      hasTimestamp = (timestamp != 0);

                        if (entity.selectorKey.isSample())
                        {
                            m_rec.samplesCount++;
                            m_rec.samplesProcessed = false;
                        }
                        else if (hasTimestamp || entity.contents != null)
                        {
                            m_rec.objectsProcessed = false;
                        }

                        if (hasTimestamp)
                        {
                            m_rangeStart = Math.min(m_rangeStart, timestamp);
                            m_rangeEnd   = Math.max(m_rangeEnd, timestamp);
                        }

                        aggregateTimeStamps(entity.subEntities);
                    }
                }
            }

            private void finalizeTimeStamps()
            {
                ZonedDateTime rangeStart = TimeUtils.fromTimestampToUtcTime(m_rangeStart);
                ZonedDateTime rangeEnd   = TimeUtils.fromTimestampToUtcTime(m_rangeEnd);
                ZonedDateTime now        = TimeUtils.fromSecondsToUtcTime(TimeUtils.nowEpochSeconds());

                m_rec.rangeStart = BoxingUtils.get(rangeStart, now);
                m_rec.rangeEnd   = BoxingUtils.get(rangeEnd, now);
            }
        }

        ResultStagingRecord rec = new ResultStagingRecord();
        rec.contents = ObjectMappers.serializeToGzip(entities);

        {
            AggregationHelper ah = new AggregationHelper(rec);
            ah.aggregateTimeStamps(entities);
            ah.finalizeTimeStamps();
        }

        helper.persist(rec);
    }

    public static int purgeProcessedRecords(RecordHelper<ResultStagingRecord> helper,
                                            ZonedDateTime purgeThreshold)
    {
        helper.lockTableUntilEndOfTransaction(10, TimeUnit.MINUTES);

        DeleteHelperWithCommonFields<ResultStagingRecord> qh = new DeleteHelperWithCommonFields<>(helper);

        qh.filterTimestampsCoveredByTargetRange(qh.root, ResultStagingRecord_.createdOn, null, purgeThreshold);

        qh.addWhereClauseWithEqual(qh.root, ResultStagingRecord_.objectsProcessed, true);
        qh.addWhereClauseWithEqual(qh.root, ResultStagingRecord_.samplesProcessed, true);

        return qh.execute();
    }

    //--//

    public static void streamUnprocessed(RecordHelper<ResultStagingRecord> helper,
                                         int maxResults,
                                         ZonedDateTime createdAfter,
                                         FunctionWithException<ResultStagingRecord, Boolean> processCallback) throws
                                                                                                              Exception
    {
        streamRecords(helper, maxResults, createdAfter, processCallback, (jh) ->
        {
            Predicate pred1 = jh.equal(jh.root, ResultStagingRecord_.objectsProcessed, false);
            Predicate pred2 = jh.equal(jh.root, ResultStagingRecord_.samplesProcessed, false);

            jh.addWhereClause(jh.or(pred1, pred2));
        });
    }

    private static int streamRecords(RecordHelper<ResultStagingRecord> helper,
                                     int maxResults,
                                     ZonedDateTime createdAfter,
                                     FunctionWithException<ResultStagingRecord, Boolean> processCallback,
                                     Consumer<QueryHelperWithCommonFields<ResultStagingRecord, ResultStagingRecord>> filterCallback) throws
                                                                                                                                     Exception
    {
        AtomicInteger processedRecords = new AtomicInteger();

        filterRecords(helper, maxResults, createdAfter, filterCallback, (rec) ->
        {
            if (!processCallback.apply(rec))
            {
                return StreamHelperNextAction.Stop_Evict;
            }

            processedRecords.incrementAndGet();

            return StreamHelperNextAction.Continue_Evict;
        });

        return processedRecords.get();
    }

    private static StreamHelperResult filterRecords(RecordHelper<ResultStagingRecord> helper,
                                                    int maxResults,
                                                    ZonedDateTime createdAfter,
                                                    Consumer<QueryHelperWithCommonFields<ResultStagingRecord, ResultStagingRecord>> filterCallback,
                                                    FunctionWithException<ResultStagingRecord, StreamHelperNextAction> callback) throws
                                                                                                                                 Exception
    {
        QueryHelperWithCommonFields<ResultStagingRecord, ResultStagingRecord> jh = new QueryHelperWithCommonFields<>(helper, ResultStagingRecord.class);

        if (createdAfter != null)
        {
            jh.filterTimestampsCoveredByTargetRange(jh.root, RecordWithCommonFields_.createdOn, createdAfter, null);
        }

        if (filterCallback != null)
        {
            filterCallback.accept(jh);
        }

        jh.addOrderBy(jh.root, RecordWithCommonFields_.createdOn, true);

        jh.setFetchSize(20);

        return QueryHelperWithCommonFields.streamNoNesting(maxResults, jh, callback);
    }
}
