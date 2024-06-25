/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.logic;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.RecordForRecurringActivity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.ILogger;

public abstract class RecurringActivityHandlerForTable<R extends RecordForRecurringActivity<R>> extends RecurringActivityHandler implements RecurringActivityHandler.ForTable
{
    private final Class<R> m_entityClass;

    protected RecurringActivityHandlerForTable(Class<R> entityClass)
    {
        m_entityClass = entityClass;
    }

    @Override
    public final Class<R> getEntityClass()
    {
        return m_entityClass;
    }

    @Override
    public CompletableFuture<ZonedDateTime> process(SessionProvider sessionProvider) throws
                                                                                     Exception
    {
        ZonedDateTime nextActivation;

        try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<R> helper = sessionHolder.createHelper(getEntityClass());

            TypedRecordIdentityList<R> list = RecordForRecurringActivity.list(helper, true, false, null);
            for (TypedRecordIdentity<R> ri : list)
            {
                getLogger().debug("Processing recurring activity %s/%s", ri.getTable(), ri.sysId);

                try
                {
                    final RecordLocked<R> locker = helper.getWithLock(ri.sysId, 10, TimeUnit.SECONDS);
                    process(sessionHolder, locker.get());
                }
                catch (Throwable ex)
                {
                    getLogger().error("Recurring activity '%s/%s' returned an error: %s", ri.getTable(), ri.sysId, ex);
                }
            }

            nextActivation = RecordForRecurringActivity.findNextActivation(helper, null);

            sessionHolder.commit();
        }

        return wrapAsync(nextActivation);
    }

    public abstract void process(SessionHolder sessionHolder,
                                 R rec) throws
                                        Exception;

    protected abstract ILogger getLogger();
}
