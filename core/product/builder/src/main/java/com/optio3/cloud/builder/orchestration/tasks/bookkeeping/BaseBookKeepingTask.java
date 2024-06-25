/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.bookkeeping;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;

import com.optio3.cloud.builder.orchestration.AbstractBuilderActivityHandler;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.persistence.SessionHolder;

public abstract class BaseBookKeepingTask extends AbstractBuilderActivityHandler
{
    public static <T extends BaseBookKeepingTask> BackgroundActivityRecord scheduleActivity(SessionHolder sessionHolder,
                                                                                            Class<T> handlerClass,
                                                                                            Consumer<T> configure) throws
                                                                                                                   Exception
    {
        requireNonNull(sessionHolder);

        T newHandler = BackgroundActivityHandler.allocate(handlerClass);

        configure.accept(newHandler);

        return newHandler.schedule(sessionHolder, null);
    }
}
