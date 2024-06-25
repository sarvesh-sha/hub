/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.logging.Logger;
import com.optio3.serialization.Reflection;
import com.optio3.service.IServiceProvider;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

@MappedSuperclass
public abstract class RecordForFixupProcessing<T extends RecordForFixupProcessing<T>> extends RecordWithCommonFields
{
    public static abstract class Handler
    {
        public enum Result
        {
            Done,
            RunAgainAtBoot,
        }

        public abstract Result process(Logger logger,
                                       SessionHolder sessionHolder) throws
                                                                    Exception;
    }

    public static final Logger LoggerInstance = new Logger(RecordForFixupProcessing.class);

    //--//

    @Column(name = "handler", nullable = false)
    private String handler;

    //--//

    protected static <T extends RecordForFixupProcessing<T>> void executeAllHandlers(IServiceProvider serviceProvider,
                                                                                     String packagePrefix,
                                                                                     Class<T> clzEntity) throws
                                                                                                         Exception
    {
        Reflections                   reflections = new Reflections(packagePrefix, new SubTypesScanner(false));
        Set<Class<? extends Handler>> candidates  = reflections.getSubTypesOf(Handler.class);

        ITableLockProvider provider = serviceProvider.getService(ITableLockProvider.class);
        if (provider == null)
        {
            throw new RuntimeException("Table-level locking not supported");
        }

        SessionProvider sessionProvider = new SessionProvider(serviceProvider, null, Optio3DbRateLimiter.Normal);

        try (var lock = provider.lockTable(sessionProvider, clzEntity, 30, TimeUnit.MINUTES))
        {
            try (SessionHolder holder = sessionProvider.newSessionWithTransaction())
            {
                RecordHelper<T> helper = holder.createHelper(clzEntity);

                // Remove the candidates that have already been processed.
                for (T rec : helper.listAll())
                {
                    try
                    {
                        RecordForFixupProcessing<?>                             recBase = rec;
                        @SuppressWarnings("unchecked") Class<? extends Handler> clz     = (Class<? extends Handler>) Class.forName(recBase.handler);
                        candidates.remove(clz);
                    }
                    catch (Throwable t)
                    {
                        // Stale record (class renamed/deleted?), remove record.
                        helper.delete(rec);
                    }
                }

                holder.commit();
            }

            // Process candidates and record execution.
            for (Class<? extends Handler> candidate : candidates)
            {
                var logger = LoggerInstance.createSubLogger(candidate);
                logger.info("Processing Fixup handler '%s'", candidate.getName());

                Handler        handler = Reflection.newInstance(candidate);
                Handler.Result result;

                Stopwatch st = Stopwatch.createStarted();
                try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
                {
                    result = handler.process(logger, sessionHolder);

                    sessionHolder.commit();
                }
                st.stop();

                logger.info("Processed Fixup handler '%s' in %s, with result '%s'", candidate.getName(), st.elapsed(), result);

                if (result == Handler.Result.Done)
                {
                    //
                    // Record in the database that we have processed this fixup.
                    //
                    try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
                    {
                        T                           rec_new     = Reflection.newInstance(clzEntity);
                        RecordForFixupProcessing<?> rec_newBase = rec_new;
                        rec_newBase.handler = candidate.getName();
                        sessionHolder.persistEntity(rec_new);

                        sessionHolder.commit();
                    }
                }
            }
        }
    }

    protected static <T extends RecordForFixupProcessing<T>> Set<Class<? extends Handler>> listExecutedHandlers(IServiceProvider serviceProvider,
                                                                                                                Class<T> clzEntity)
    {
        Set<Class<? extends Handler>> results = Sets.newHashSet();

        try (SessionHolder holder = SessionHolder.createWithNewSessionWithoutTransaction(serviceProvider, null, Optio3DbRateLimiter.Normal))
        {
            RecordHelper<T> helper = holder.createHelper(clzEntity);

            // Remove the candidates that have already been processed.
            for (T rec : helper.listAll())
            {
                try
                {
                    RecordForFixupProcessing                                recBase = rec;
                    @SuppressWarnings("unchecked") Class<? extends Handler> clz     = (Class<? extends Handler>) Class.forName(recBase.handler);
                    results.add(clz);
                }
                catch (Throwable t)
                {
                    // Stale record (class renamed/deleted?), remove record.
                }
            }
        }

        return results;
    }
}
