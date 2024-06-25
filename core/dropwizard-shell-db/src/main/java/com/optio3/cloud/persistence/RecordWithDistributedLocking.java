/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.LockTimeoutException;
import javax.persistence.MappedSuperclass;

import com.google.common.collect.Sets;
import com.optio3.cloud.AbstractApplication;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;
import com.optio3.serialization.Reflection;
import com.optio3.service.IServiceProvider;
import com.optio3.util.Encryption;
import com.optio3.util.Exceptions;
import com.optio3.util.IdGenerator;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.FunctionWithException;
import org.apache.commons.lang3.StringUtils;

/**
 * A helper class to allow shared locking through the database.
 * <p>
 * <b>IMPORTANT:</b> always call the superclass implementation when overriding the common methods.
 */
@MappedSuperclass
public abstract class RecordWithDistributedLocking
{
    public static final Logger LoggerInstance = new Logger(RecordWithDistributedLocking.class);

    private static class Holder<T extends RecordWithDistributedLocking> extends TableLockHolder
    {
        private final SessionProvider    m_sessionProvider;
        private final Class<T>           m_clzTableForLockRecords;
        private final String             m_id;
        private       String             m_ownerId;
        private       ScheduledFuture<?> m_update;

        Holder(SessionProvider sessionProvider,
               Class<T> clzTableForLockRecords,
               String id,
               String ownerId)
        {
            m_sessionProvider        = sessionProvider;
            m_clzTableForLockRecords = clzTableForLockRecords;
            m_id                     = id;
            m_ownerId                = ownerId;

            scheduleUpdateIfStillOwned();
        }

        @Override
        protected void closeInner()
        {
            doWithLock(m_sessionProvider, m_clzTableForLockRecords, m_id, (rec) ->
            {
                cancelUpdate();

                String ownerId = m_ownerId;
                if (ownerId != null)
                {
                    m_ownerId = null;
                    rec.release(ownerId);
                }

                return true;
            });
        }

        //--//

        private void scheduleUpdateIfStillOwned()
        {
            cancelUpdate();

            synchronized (this)
            {
                m_update = Executors.scheduleOnDefaultPool(this::updateIfStillOwned, 1, TimeUnit.MINUTES);
            }
        }

        private void cancelUpdate()
        {
            synchronized (this)
            {
                if (m_update != null)
                {
                    m_update.cancel(false);
                    m_update = null;
                }
            }
        }

        private void updateIfStillOwned()
        {
            doWithLock(m_sessionProvider, m_clzTableForLockRecords, m_id, (rec) ->
            {
                String ownerId = m_ownerId;
                if (ownerId != null && rec.updateIfHeld(ownerId))
                {
                    scheduleUpdateIfStillOwned();
                    return Boolean.TRUE;
                }

                return null;
            });
        }
    }

    //--//

    @Id
    @Column(name = "lock_id", nullable = false)
    private String lockId;

    @Column(name = "held_by")
    private String heldBy;

    @Column(name = "held_on")
    private ZonedDateTime heldOn;

    //--//

    public String getLockId()
    {
        return lockId;
    }

    //--//

    protected static <T extends RecordWithDistributedLocking> void register(AbstractApplication<?> app,
                                                                            Class<T> clzTableForLockRecords,
                                                                            List<Class<?>> entities)
    {
        Set<String> keysForEntities = Sets.newHashSet();

        for (Class<?> entity : entities)
        {
            keysForEntities.add(createKey(entity, null));
        }

        try (SessionHolder sessionHolder = SessionHolder.createWithNewSessionWithTransaction(app, null, Optio3DbRateLimiter.System))
        {
            RecordHelper<T> helper               = sessionHolder.createHelper(clzTableForLockRecords);
            ZonedDateTime   thresholdForDeletion = TimeUtils.past(1, TimeUnit.HOURS);

            for (T t : helper.listAll())
            {
                RecordWithDistributedLocking base = t;

                if (keysForEntities.contains(base.lockId))
                {
                    // A known table, leave it.
                    continue;
                }

                if (base.heldBy == null && TimeUtils.isBeforeOrNull(base.heldOn, thresholdForDeletion))
                {
                    helper.delete(t);
                }
            }

            sessionHolder.commit();
        }

        for (String keyForEntity : keysForEntities)
        {
            ensureRecord(app, clzTableForLockRecords, keyForEntity);
        }

        ITableLockProvider provider = new ITableLockProvider()
        {
            @Override
            public TableLockHolder lockTable(SessionProvider sessionProvider,
                                             Class<?> classForTableToLock,
                                             long timeout,
                                             TimeUnit unit) throws
                                                            LockTimeoutException
            {
                String id = createKey(classForTableToLock, null);

                return grab(sessionProvider, clzTableForLockRecords, id, timeout, unit);
            }

            @Override
            public TableLockHolder lockRecord(SessionProvider sessionProvider,
                                              Class<?> classForTableToLock,
                                              String subId,
                                              long timeout,
                                              TimeUnit unit) throws
                                                             LockTimeoutException
            {
                String id = createKey(classForTableToLock, subId);

                return grab(sessionProvider, clzTableForLockRecords, id, timeout, unit);
            }

            private TableLockHolder grab(SessionProvider sessionProvider,
                                         Class<T> clzTableForLockRecords,
                                         String id,
                                         long timeout,
                                         TimeUnit unit)
            {
                ensureRecord(sessionProvider, clzTableForLockRecords, id);

                String ownerId = IdGenerator.newGuid() + " - " + Thread.currentThread()
                                                                       .getName();

                MonotonousTime timeoutExpiration = TimeUtils.computeTimeoutExpiration(timeout, unit);
                while (true)
                {
                    if (TimeUtils.isTimeoutExpired(timeoutExpiration))
                    {
                        throw Exceptions.newGenericException(LockTimeoutException.class, "Failed to lock table '%s' due to timeout", clzTableForLockRecords);
                    }

                    TableLockHolder holder = doWithLock(sessionProvider, clzTableForLockRecords, id, (rec) ->
                    {
                        if (!rec.canGrab(ownerId))
                        {
                            return null;
                        }

                        return new Holder<>(sessionProvider, clzTableForLockRecords, id, ownerId);
                    });

                    if (holder != null)
                    {
                        return holder;
                    }

                    Executors.safeSleep(100);
                }
            }
        };

        app.registerService(ITableLockProvider.class, () -> provider);
    }

    private boolean canGrab(String ownerId)
    {
        ZonedDateTime now = TimeUtils.now();

        if (heldBy != null)
        {
            if (TimeUtils.wasUpdatedRecently(heldOn, 5, TimeUnit.MINUTES))
            {
                return false;
            }

            //
            // If it hasn't been updated in X minutes, assume abandoned.
            //
            LoggerInstance.warn("Table Lock '%s' expired (%s)! Moving ownership from '%s' to '%s'", lockId, heldOn, heldBy, ownerId);
        }
        else
        {
            LoggerInstance.debug("Table Lock '%s' acquired by '%s'", lockId, ownerId);
        }

        heldBy = ownerId;
        heldOn = now;
        return true;
    }

    private boolean updateIfHeld(String ownerId)
    {
        if (StringUtils.equals(heldBy, ownerId))
        {
            LoggerInstance.debug("Table Lock '%s' updated for '%s'", lockId, ownerId);
            heldOn = TimeUtils.now();
            return true;
        }
        else
        {
            LoggerInstance.debug("Table Lock '%s' no longer held by '%s', now '%s'", lockId, ownerId, heldBy);
        }

        return false;
    }

    private void release(String ownerId)
    {
        if (StringUtils.equals(heldBy, ownerId))
        {
            LoggerInstance.debug("Table Lock '%s' was released by '%s'", lockId, ownerId);
            heldBy = null;
        }
        else
        {
            LoggerInstance.error("Table Lock '%s' reassigned to new owner! Expected: '%s', Actual Value: '%s'", lockId, ownerId, heldBy);
        }
    }

    private static <T extends RecordWithDistributedLocking> void ensureRecord(IServiceProvider serviceProvider,
                                                                              Class<T> clzTableForLockRecords,
                                                                              String id)
    {
        for (int retry = 0; retry < 10; retry++)
        {
            try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(serviceProvider, null, Optio3DbRateLimiter.System))
            {
                LazyRecordFlusher<T> lazy = holder.ensureEntity(clzTableForLockRecords, id, (h, idNew) ->
                {
                    T                            rec      = Reflection.newInstance(clzTableForLockRecords);
                    RecordWithDistributedLocking recSuper = rec;
                    recSuper.lockId = idNew;
                    return rec;
                });

                if (lazy.isNew())
                {
                    lazy.getAfterPersist();
                    holder.commit();
                }

                return;
            }
            catch (Throwable t)
            {
                Executors.safeSleep(5 + Encryption.generateRandomValue32Bit(50));
            }
        }

        LoggerInstance.error("Failed to create lock record for table '%s", id);
    }

    private static String createKey(Class<?> entity,
                                    String subId)
    {
        if (subId != null)
        {
            return entity.getName() + "#" + subId;
        }
        else
        {
            return entity.getName();
        }
    }

    private static <T extends RecordWithDistributedLocking, R> R doWithLock(SessionProvider sessionProvider,
                                                                            Class<T> clzTableForLockRecords,
                                                                            String id,
                                                                            FunctionWithException<RecordWithDistributedLocking, R> callback)
    {
        try (SessionHolder holder = sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<T> helper = holder.createHelper(clzTableForLockRecords);
            RecordLocked<T> rec    = helper.getWithLock(id, 100, TimeUnit.MILLISECONDS);

            R res = callback.apply(rec.get());
            if (res != null)
            {
                holder.commit();
                return res;
            }
        }
        catch (Throwable t)
        {
            // Fall through...
        }

        return null;
    }
}
