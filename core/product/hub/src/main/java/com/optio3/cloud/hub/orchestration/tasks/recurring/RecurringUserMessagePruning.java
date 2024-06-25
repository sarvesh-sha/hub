/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks.recurring;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.annotation.Optio3RecurringProcessor;
import com.optio3.cloud.hub.persistence.asset.ResultStagingRecord_;
import com.optio3.cloud.hub.persistence.message.UserMessageRecord;
import com.optio3.cloud.logic.BackgroundActivityScheduler;
import com.optio3.cloud.logic.RecurringActivityHandler;
import com.optio3.cloud.persistence.DeleteHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Logger;
import com.optio3.util.TimeUtils;

@Optio3RecurringProcessor
public class RecurringUserMessagePruning extends RecurringActivityHandler
{
    public static final Logger LoggerInstance = BackgroundActivityScheduler.LoggerInstance.createSubLogger(RecurringUserMessagePruning.class);

    private static final Duration c_storingLength = Duration.of(90, ChronoUnit.DAYS);

    @Override
    public Duration startupDelay()
    {
        return null;
    }

    @Override
    public CompletableFuture<ZonedDateTime> process(SessionProvider sessionProvider) throws
                                                                                     Exception
    {
        try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<UserMessageRecord> helper = sessionHolder.createHelper(UserMessageRecord.class);

            helper.lockTableUntilEndOfTransaction(10, TimeUnit.MINUTES);

            // How long to keep user messages.
            ZonedDateTime now            = TimeUtils.now();
            ZonedDateTime purgeThreshold = now.minus(c_storingLength);

            DeleteHelperWithCommonFields<UserMessageRecord> qh = new DeleteHelperWithCommonFields<>(helper);

            qh.filterTimestampsCoveredByTargetRange(qh.root, ResultStagingRecord_.createdOn, null, purgeThreshold);

            int deleted = qh.execute();

            sessionHolder.commit();

            if (deleted > 0)
            {
                LoggerInstance.info("Pruned %,d user messages older than %s", deleted, c_storingLength);

                helper.queueDefragmentation();
            }
        }

        return wrapAsync(TimeUtils.future(1, TimeUnit.DAYS));
    }

    @Override
    public void shutdown()
    {
        // Nothing to do.
    }
}
