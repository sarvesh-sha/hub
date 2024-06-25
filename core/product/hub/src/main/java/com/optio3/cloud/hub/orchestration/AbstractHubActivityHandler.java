/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration;

import static java.util.Objects.requireNonNull;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.persistence.BackgroundActivityChunkRecord;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.HostAssetRecord;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.ConsumerWithException;

public abstract class AbstractHubActivityHandler extends BackgroundActivityHandler<BackgroundActivityRecord, BackgroundActivityChunkRecord, HostAssetRecord>
{
    @JsonIgnore
    protected HubApplication app;

    @JsonIgnore
    protected HubConfiguration appConfig;

    //--//

    @Override
    public void configureContext()
    {
        // Nothing for now.
    }

    @Override
    public void setSessionProvider(SessionProvider sessionProvider)
    {
        super.setSessionProvider(sessionProvider);

        this.app = getServiceNonNull(HubApplication.class);
        this.appConfig = getService(HubConfiguration.class);
    }

    @Override
    protected Class<BackgroundActivityChunkRecord> getChunkClass()
    {
        return BackgroundActivityChunkRecord.class;
    }

    //--//

    public static <T extends AbstractHubActivityHandler> BackgroundActivityRecord scheduleActivity(SessionHolder sessionHolder,
                                                                                                   long delay,
                                                                                                   ChronoUnit unit,
                                                                                                   Class<T> handlerClass,
                                                                                                   ConsumerWithException<T> configure) throws
                                                                                                                                       Exception
    {
        requireNonNull(sessionHolder);

        T newHandler = BackgroundActivityHandler.allocate(handlerClass);

        configure.accept(newHandler);

        // Weird Java visibility rule...
        AbstractHubActivityHandler downcast = newHandler;

        ZonedDateTime when = TimeUtils.now();

        // Start the actual task with some delay, to avoid overlapping with outer transaction.
        when = when.plus(100, ChronoUnit.MILLIS);

        if (delay > 0 && unit != null)
        {
            when = when.plus(delay, unit);
        }

        return downcast.schedule(sessionHolder, when);
    }

    protected BackgroundActivityRecord scheduleNextHandler(SessionHolder sessionHolder,
                                                           AbstractHubActivityHandler nextHandler) throws
                                                                                                   Exception
    {
        nextHandler.state = state;

        return nextHandler.schedule(sessionHolder, null);
    }

    private BackgroundActivityRecord schedule(SessionHolder sessionHolder,
                                              ZonedDateTime now) throws
                                                                 Exception
    {
        return BackgroundActivityRecord.schedule(sessionHolder, this, now);
    }
}
