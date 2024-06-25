/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.recurring;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.annotation.Optio3RecurringProcessor;
import com.optio3.cloud.builder.model.deployment.DeploymentHostImagePullFilterRequest;
import com.optio3.cloud.builder.model.jobs.JobStatus;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostImagePullRecord;
import com.optio3.cloud.logic.BackgroundActivityScheduler;
import com.optio3.cloud.logic.RecurringActivityHandler;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Logger;
import com.optio3.util.TimeUtils;

@Optio3RecurringProcessor
public class RecurringImagePullPruning extends RecurringActivityHandler
{
    public static final Logger LoggerInstance = BackgroundActivityScheduler.LoggerInstance.createSubLogger(RecurringImagePullPruning.class);

    //--//

    @Override
    public Duration startupDelay()
    {
        return Duration.of(20, ChronoUnit.MINUTES);
    }

    @Override
    public CompletableFuture<ZonedDateTime> process(SessionProvider sessionProvider) throws
                                                                                     Exception
    {
        sessionProvider.callWithSessionWithAutoCommit((sessionHolder) ->
                                                      {
                                                          RecordHelper<DeploymentHostImagePullRecord> helper = sessionHolder.createHelper(DeploymentHostImagePullRecord.class);

                                                          DeploymentHostImagePullFilterRequest filters = new DeploymentHostImagePullFilterRequest();
                                                          filters.olderThan = TimeUtils.past(3, TimeUnit.DAYS);

                                                          for (RecordIdentity ri : DeploymentHostImagePullRecord.filterPulls(helper, filters))
                                                          {
                                                              DeploymentHostImagePullRecord rec = helper.getOrNull(ri.sysId);
                                                              if (rec.getStatus() != JobStatus.EXECUTING)
                                                              {
                                                                  helper.delete(rec);
                                                              }
                                                          }
                                                      });

        return wrapAsync(TimeUtils.future(12, TimeUnit.HOURS));
    }

    @Override
    public void shutdown()
    {
        // Nothing to do.
    }
}
