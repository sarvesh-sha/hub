/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration;

import static java.util.Objects.requireNonNull;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.HostRemoter;
import com.optio3.cloud.builder.persistence.BackgroundActivityChunkRecord;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.ConsumerWithException;

public abstract class AbstractBuilderActivityHandler extends BackgroundActivityHandler<BackgroundActivityRecord, BackgroundActivityChunkRecord, HostRecord>
{
    @JsonIgnore
    protected BuilderApplication app;

    @JsonIgnore
    protected BuilderConfiguration appConfig;

    @JsonIgnore
    protected HostRemoter hostRemoter;

    //--//

    @Override
    public void setSessionProvider(SessionProvider sessionProvider)
    {
        super.setSessionProvider(sessionProvider);

        this.app         = getServiceNonNull(BuilderApplication.class);
        this.appConfig   = getService(BuilderConfiguration.class);
        this.hostRemoter = getService(HostRemoter.class);
    }

    @Override
    protected Class<BackgroundActivityChunkRecord> getChunkClass()
    {
        return BackgroundActivityChunkRecord.class;
    }

    //--//

    public static <T extends AbstractBuilderActivityHandler> BackgroundActivityRecord scheduleActivity(SessionHolder sessionHolder,
                                                                                                       long delay,
                                                                                                       ChronoUnit unit,
                                                                                                       Class<T> handlerClass,
                                                                                                       ConsumerWithException<T> configure) throws
                                                                                                                                           Exception
    {
        requireNonNull(sessionHolder);

        T newHandler = BackgroundActivityHandler.allocate(handlerClass);

        configure.accept(newHandler);

        ZonedDateTime when = TimeUtils.now();

        // Start the actual task with some delay, to avoid overlapping with outer transaction.
        when = when.plus(100, ChronoUnit.MILLIS);

        if (delay > 0 && unit != null)
        {
            when = when.plus(delay, unit);
        }

        return newHandler.schedule(sessionHolder, when);
    }

    protected BackgroundActivityRecord scheduleNextHandler(SessionHolder sessionHolder,
                                                           AbstractBuilderActivityHandler nextHandler) throws
                                                                                                       Exception
    {
        nextHandler.state = state;

        return nextHandler.schedule(sessionHolder, null);
    }

    public BackgroundActivityRecord schedule(SessionHolder sessionHolder,
                                             ZonedDateTime now) throws
                                                                Exception
    {
        return BackgroundActivityRecord.schedule(sessionHolder, this, now);
    }
}
