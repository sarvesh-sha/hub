/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks.recurring;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.annotation.Optio3RecurringProcessor;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.model.customization.digineous.InstanceConfigurationForDigineous;
import com.optio3.cloud.logic.BackgroundActivityScheduler;
import com.optio3.cloud.logic.RecurringActivityHandler;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Logger;
import com.optio3.serialization.Reflection;
import com.optio3.util.TimeUtils;

@Optio3RecurringProcessor
public class RecurringInfiniteImpulsePull extends RecurringActivityHandler
{
    public static final Logger LoggerInstance = BackgroundActivityScheduler.LoggerInstance.createSubLogger(RecurringInfiniteImpulsePull.class);

    @Override
    public Duration startupDelay()
    {
        return null;
    }

    @Override
    public CompletableFuture<ZonedDateTime> process(SessionProvider sessionProvider) throws
                                                                                     Exception
    {
        var cfg = Reflection.as(sessionProvider.getServiceNonNull(InstanceConfiguration.class), InstanceConfigurationForDigineous.class);
        if (cfg != null)
        {
            ZonedDateTime delay = cfg.pullVibrationData();
            if (delay == null)
            {
                delay = TimeUtils.future(5, TimeUnit.MINUTES);
            }

            return wrapAsync(delay);
        }

        return AsyncRuntime.asNull();
    }

    @Override
    public void shutdown()
    {
        // Nothing to do.
    }
}
