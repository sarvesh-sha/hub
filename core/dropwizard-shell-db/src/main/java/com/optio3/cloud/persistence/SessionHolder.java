/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import javax.persistence.TransactionRequiredException;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import com.google.common.collect.Sets;
import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.db.Optio3DataSourceFactory;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.concurrency.Executors;
import com.optio3.concurrency.RateLimiter;
import com.optio3.lang.RunnableWithException;
import com.optio3.logging.Logger;
import com.optio3.serialization.Reflection;
import com.optio3.service.IServiceProvider;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.function.ConsumerWithException;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.TransientObjectException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDB103Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.query.spi.QueryPlanCache;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public class SessionHolder implements AutoCloseable,
                                      IServiceProvider
{
    public static final Logger LoggerInstance = new Logger(SessionHolder.class);

    private final Optio3DbRateLimiter     m_rateLimiter;
    private final RateLimiter.Holder      m_rateLimiterHolder;
    private final IServiceProvider        m_serviceProvider;
    private final String                  m_databaseId;
    private final Optio3DataSourceFactory m_dataFactory;
    private final Session                 m_session;
    private       Transaction             m_inTransaction;
    private       boolean                 m_readonly;
    private       boolean                 m_closed;
    private       int                     m_pendingCount;

    private SessionHolder(IServiceProvider serviceProvider,
                          String databaseId,
                          Optio3DbRateLimiter rateLimiter)
    {
        m_rateLimiter     = requireNonNull(rateLimiter);
        m_serviceProvider = requireNonNull(serviceProvider);
        m_databaseId      = databaseId;

        AbstractApplicationWithDatabase<?> app = serviceProvider.getServiceNonNull(AbstractApplicationWithDatabase.class);

        m_dataFactory       = app.getDataSourceFactory(databaseId);
        m_rateLimiterHolder = m_dataFactory.acquirePermit(rateLimiter, 30, TimeUnit.SECONDS);

        if (!m_rateLimiterHolder.wasAcquired())
        {
            throw Exceptions.newRuntimeException("No JDBC connection available for %s", rateLimiter);
        }

        Session session = null;

        try
        {
            SessionFactory sessionFactory = app.getSessionFactory(databaseId);
            session   = sessionFactory.openSession();
            m_session = session;
        }
        finally
        {
            if (session == null) // In case we fail to acquire a session, release the rate limiter.
            {
                m_rateLimiterHolder.close();
            }
        }
    }

    public static <T extends RecordWithCommonFields> boolean sameEntity(T t1,
                                                                        T t2)
    {
        if (t1 == null || t2 == null)
        {
            return t1 == t2;
        }

        return StringUtils.equals(t1.getSysId(), t2.getSysId());
    }

    public static <T extends RecordWithCommonFields> boolean addIfMissingAndNotNull(List<T> list,
                                                                                    T rec)
    {
        if (CollectionUtils.findFirst(list, (r) -> SessionHolder.sameEntity(rec, r)) == null)
        {
            list.add(rec);
            return true;
        }

        return false;
    }

    public static <T> boolean isPropertyInitialized(T rec,
                                                    String propertyName)
    {
        return rec != null && Hibernate.isPropertyInitialized(rec, propertyName);
    }

    public static <T> T unwrapProxy(T rec)
    {
        if (rec instanceof HibernateProxy || rec instanceof PersistentAttributeInterceptable)
        {
            Hibernate.initialize(rec);
        }

        @SuppressWarnings("unchecked") T recNoProxy = (T) Hibernate.unproxy(rec);
        return recNoProxy;
    }

    public static <T> List<T> unwrapProxies(List<T> lst)
    {
        if (lst != null)
        {
            for (int i = 0; i < lst.size(); i++)
            {
                lst.set(i, unwrapProxy(lst.get(i)));
            }
        }

        return lst;
    }

    public static Class<?> getClassOfEntity(Object rec)
    {
        rec = SessionHolder.unwrapProxy(rec);
        return rec.getClass();
    }

    public static boolean isEntityOfClass(Object rec,
                                          Class<?> clz)
    {
        rec = SessionHolder.unwrapProxy(rec);

        return clz.isInstance(rec);
    }

    public static <T> T asEntityOfClassOrNull(Object rec,
                                              Class<T> clz)
    {
        rec = SessionHolder.unwrapProxy(rec);

        return clz.isInstance(rec) ? clz.cast(rec) : null;
    }

    public static <T> T asEntityOfClass(Object rec,
                                        Class<T> clz)
    {
        rec = SessionHolder.unwrapProxy(rec);

        return clz.cast(rec);
    }

    public static SessionHolder createWithNewSessionWithoutTransaction(IServiceProvider serviceProvider,
                                                                       String databaseId,
                                                                       Optio3DbRateLimiter rateLimiter)
    {
        return new SessionHolder(serviceProvider, databaseId, rateLimiter);
    }

    public static SessionHolder createWithNewSessionWithTransaction(IServiceProvider serviceProvider,
                                                                    String databaseId,
                                                                    Optio3DbRateLimiter rateLimiter)
    {
        SessionHolder sessionHolder = createWithNewSessionWithoutTransaction(serviceProvider, databaseId, rateLimiter);

        sessionHolder.beginTransaction();

        return sessionHolder;
    }

    public static SessionHolder createWithNewReadOnlySession(IServiceProvider serviceProvider,
                                                             String databaseId,
                                                             Optio3DbRateLimiter rateLimiter)
    {
        SessionHolder sessionHolder = createWithNewSessionWithoutTransaction(serviceProvider, databaseId, rateLimiter);

        sessionHolder.makeReadOnly();

        return sessionHolder;
    }

    public SessionHolder spawnNewSessionWithoutTransaction()
    {
        return createWithNewSessionWithoutTransaction(m_serviceProvider, m_databaseId, m_rateLimiter);
    }

    public SessionHolder spawnNewSessionWithTransaction()
    {
        SessionHolder sessionHolder = spawnNewSessionWithoutTransaction();

        sessionHolder.beginTransaction();

        return sessionHolder;
    }

    public SessionHolder spawnNewReadOnlySession()
    {
        SessionHolder sessionHolder = spawnNewSessionWithoutTransaction();

        sessionHolder.makeReadOnly();

        return sessionHolder;
    }

    private void makeReadOnly()
    {
        m_session.setHibernateFlushMode(FlushMode.MANUAL);
        m_session.setDefaultReadOnly(true);

        m_readonly = true;
    }

    public void flushQueryPlanCache()
    {
        SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) m_session.getSessionFactory();

        //
        // Unfortunately, calling cleanup() on the existing QueryPlanCache does *not* clear the eviction stack, which can hold gigabytes...
        // So we have to use Reflection to force a new cache.
        //
        // sessionFactory.getQueryPlanCache()
        //               .cleanup();

        var queryPlanCache = new QueryPlanCache(sessionFactory);

        var accessor = new Reflection.FieldAccessor(SessionFactoryImpl.class, "queryPlanCache");
        accessor.set(sessionFactory, queryPlanCache);
    }

    //--//

    @Override
    public <S> S getService(Class<S> serviceClass)
    {
        return m_serviceProvider.getService(serviceClass);
    }

    public IServiceProvider getServiceProvider()
    {
        return m_serviceProvider;
    }

    public SessionProvider getSessionProvider()
    {
        return new SessionProvider(m_serviceProvider, m_databaseId, m_rateLimiter);
    }

    //--//

    private Dialect m_dialect;

    public Dialect getDialect()
    {
        if (m_dialect == null)
        {
            SessionFactoryImplementor  sessionFactory  = (SessionFactoryImplementor) m_session.getSessionFactory();
            ServiceRegistryImplementor serviceRegistry = sessionFactory.getServiceRegistry();
            JdbcServices               jdbcServices    = serviceRegistry.getService(JdbcServices.class);

            m_dialect = jdbcServices.getDialect();
        }

        return m_dialect;
    }

    //--//

    public Set<String> listVariables()
    {
        Set<String> set = Sets.newHashSet();

        try
        {
            Dialect dialet = getDialect();
            if (dialet instanceof MySQLDialect)
            {
                NativeQuery<Tuple> query = createNativeQuery("show global variables", Tuple.class);
                for (Tuple tuple : query.list())
                {
                    set.add((String) tuple.get("Variable_name"));
                }
            }
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed to list DB variables: %s", t);
        }

        return set;
    }

    public String getVariable(String name)
    {
        try
        {
            Dialect dialet = getDialect();
            if (dialet instanceof MySQLDialect)
            {
                NativeQuery<Tuple> query = createNativeQuery("show variables where Variable_name = :name", Tuple.class);
                query.setParameter("name", name);

                for (Tuple tuple : query.list())
                {
                    return (String) tuple.get("Value");
                }
            }
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed to get DB variable '%s': %s", name, t);
        }

        return null;
    }

    public boolean setVariable(String name,
                               Object value)
    {
        try
        {
            Set<String> validVariables = listVariables();
            if (validVariables.contains(name))
            {
                Dialect dialet = getDialect();
                if (dialet instanceof MySQLDialect)
                {
                    NativeQuery query = createNativeQuery("set global " + name + " = :value");
                    query.setParameter("value", value);
                    query.executeUpdate();
                    return true;
                }
            }
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed to set DB variable '%s': %s", name, t);
        }

        return false;
    }

    //--//

    /**
     * Return the identifier value of the given entity as associated with this
     * session.  An exception is thrown if the given entity instance is transient
     * or detached in relation to this session.
     *
     * @param entity a persistent instance
     *
     * @return the identifier
     *
     * @throws TransientObjectException if the instance is transient or associated with
     *                                  a different session
     */
    public Serializable getIdentifier(Object entity)
    {
        return m_session.getIdentifier(entity);
    }

    /**
     * Create a {@link NaturalIdLoadAccess} instance to retrieve the specified entity by
     * its natural id.
     *
     * @param entityClass The entity type to be retrieved
     *
     * @return load delegate for loading the specified entity type by natural id
     *
     * @throws HibernateException If the specified Class cannot be resolved as a mapped entity
     */
    public <E> NaturalIdLoadAccess<E> byNaturalId(Class<E> entityClass)
    {
        return m_session.byNaturalId(entityClass);
    }

    /**
     * Returns the current {@link CriteriaBuilder}.
     *
     * @return the current criteria builder
     */
    public CriteriaBuilder getCriteriaBuilder()
    {
        return m_session.getCriteriaBuilder();
    }

    public <E> Query<E> createQuery(String query,
                                    Class<E> entityClass)
    {
        return m_session.createQuery(requireNonNull(query), entityClass);
    }

    public <E> Query<E> createQuery(CriteriaQuery<E> criteriaQuery)
    {
        return m_session.createQuery(requireNonNull(criteriaQuery));
    }

    public NativeQuery createNativeQuery(String sqlString)
    {
        return m_session.createNativeQuery(sqlString);
    }

    public <R> NativeQuery<R> createNativeQuery(String sqlString,
                                                Class<R> clz)
    {
        return m_session.createNativeQuery(sqlString, clz);
    }

    public <E> Query<E> createQuery(CriteriaDelete<E> criteriaDelete)
    {
        return m_session.createQuery(requireNonNull(criteriaDelete));
    }

    public FullTextSession getFullTextSession()
    {
        return Search.getFullTextSession(m_session);
    }

    //--//

    /**
     * Flushes the session to the database.
     */
    public void flush()
    {
        if (m_readonly)
        {
            // Nothing to do.
            return;
        }

        final FlushMode flushMode = m_session.getHibernateFlushMode();
        switch (flushMode)
        {
            case ALWAYS:
            case MANUAL:
                m_session.flush();
                break;

            default:
                //
                // If the session is in auto or commit mode, flush() will be a no-op and updates won't be saved to the database.
                //
                m_session.setHibernateFlushMode(FlushMode.MANUAL);
                m_session.flush();
                m_session.setHibernateFlushMode(flushMode);
                break;
        }
    }

    //--//

    public <E, K extends Serializable> LazyRecordFlusher<E> ensureEntity(Class<E> entityClass,
                                                                         K id,
                                                                         BiFunction<SessionHolder, K, E> callback)
    {
        RecordHelper<E> helper = createHelper(entityClass);

        E rec = helper.getOrNull(id);
        if (rec != null)
        {
            return helper.wrapAsExistingRecord(rec);
        }
        else
        {
            rec = callback.apply(this, id);
            return helper.wrapAsNewRecord(rec);
        }
    }

    public <E> E getEntity(Class<E> entityClass,
                           Serializable id)
    {
        return unwrapProxy(m_session.load(entityClass, requireNonNull(id)));
    }

    public <E> E getEntityOrNull(Class<E> entityClass,
                                 Serializable id)
    {
        if (id == null)
        {
            return null;
        }

        return unwrapProxy(m_session.get(entityClass, id));
    }

    public <E> RecordLocked<E> getEntityWithLock(Class<E> entityClass,
                                                 Serializable id,
                                                 long timeout,
                                                 TimeUnit unit)
    {
        if (unit == null)
        {
            E rec = getEntity(entityClass, id);
            return new RecordLocked<>(this, rec);
        }

        return callWithTimeout(timeout, unit, () ->
        {
            E rec = unwrapProxy(m_session.load(entityClass, requireNonNull(id), prepareLockOptions(timeout, unit)));
            return new RecordLocked<>(this, rec);
        });
    }

    public <E> RecordLocked<E> getEntityWithLockOrNull(Class<E> entityClass,
                                                       Serializable id,
                                                       long timeout,
                                                       TimeUnit unit)
    {
        if (unit == null)
        {
            E rec = getEntityOrNull(entityClass, id);
            return rec != null ? new RecordLocked<>(this, rec) : null;
        }

        return callWithTimeout(timeout, unit, () ->
        {
            E rec = unwrapProxy(m_session.get(entityClass, requireNonNull(id), prepareLockOptions(timeout, unit)));
            return rec != null ? new RecordLocked<>(this, rec) : null;
        });
    }

    private static LockOptions prepareLockOptions(long timeout,
                                                  TimeUnit unit)
    {
        int timeoutInMillisecs = Math.max(10, (int) unit.toMillis(timeout));
        return new LockOptions(LockMode.PESSIMISTIC_WRITE).setTimeOut(timeoutInMillisecs);
    }

    public <E2 extends RecordWithCommonFields> RecordLocked<E2> optimisticallyUpgradeToLocked(E2 entity,
                                                                                              long timeout,
                                                                                              TimeUnit unit)
    {
        if (entity == null)
        {
            return null;
        }

        if (unit == null)
        {
            return new RecordLocked<>(this, entity);
        }

        @SuppressWarnings("unchecked") Class<E2> entityClass = (Class<E2>) getClassOfEntity(entity);

        return getEntityWithLock(entityClass, entity.getSysId(), timeout, unit);
    }

    //--//

    public <E> RecordLocked<E> persistEntity(E entity)
    {
        checkInTransaction();

        m_session.persist(requireNonNull(entity));

        return new RecordLocked<>(this, entity);
    }

    public <E> void deleteEntity(E entity)
    {
        checkInTransaction();

        Optio3Lifecycle lifeCycle = Reflection.as(entity, Optio3Lifecycle.class);
        if (lifeCycle != null)
        {
            lifeCycle.onPreDelete(this);
        }

        if (entity != null)
        {
            m_session.delete(entity);
        }
    }

    public <E> void evictEntity(E entity)
    {
        m_session.evict(requireNonNull(entity));

        Optio3Lifecycle lifeCycle = Reflection.as(entity, Optio3Lifecycle.class);
        if (lifeCycle != null)
        {
            lifeCycle.onEviction();
        }
    }

    //--//

    public <E> RecordHelper<E> createHelper(Class<E> entityClass)
    {
        return new RecordHelper<>(this, entityClass);
    }

    public <E> RecordLocator<E> createLocator(E entity)
    {
        return entity != null ? new RecordLocator<E>(this, entity) : null;
    }

    //--//

    public <E> E fromLocator(RecordLocator<E> locator)
    {
        return locator != null ? locator.get(this) : null;
    }

    public <E> E fromLocatorOrNull(RecordLocator<E> locator)
    {
        return locator != null ? locator.getOrNull(this) : null;
    }

    public <E> RecordLocked<E> fromLocatorWithLock(RecordLocator<E> locator,
                                                   long timeout,
                                                   TimeUnit unit)
    {
        return locator != null ? locator.getWithLock(this, timeout, unit) : null;
    }

    public <E> RecordLocked<E> fromLocatorWithLockOrNull(RecordLocator<E> locator,
                                                         long timeout,
                                                         TimeUnit unit)
    {
        return locator != null ? locator.getWithLockOrNull(this, timeout, unit) : null;
    }

    //--//

    public <E extends RecordWithCommonFields> E fromIdentity(TypedRecordIdentity<? extends E> ri)
    {
        if (ri == null || ri.sysId == null)
        {
            return null;
        }

        return getEntity(ri.resolveEntityClass(), ri.sysId);
    }

    public <E extends RecordWithCommonFields> E fromIdentityOrNull(TypedRecordIdentity<E> ri)
    {
        if (ri == null || ri.sysId == null)
        {
            return null;
        }

        return getEntityOrNull(ri.resolveEntityClass(), ri.sysId);
    }

    public <E extends RecordWithCommonFields> RecordLocked<E> fromIdentityWithLock(TypedRecordIdentity<E> ri,
                                                                                   long timeout,
                                                                                   TimeUnit unit)
    {
        if (ri == null || ri.sysId == null)
        {
            return null;
        }

        return getEntityWithLock(ri.resolveEntityClass(), ri.sysId, timeout, unit);
    }

    public <E extends RecordWithCommonFields> RecordLocked<E> fromIdentityWithLockOrNull(TypedRecordIdentity<E> ri,
                                                                                         long timeout,
                                                                                         TimeUnit unit)
    {
        if (ri == null || ri.sysId == null)
        {
            return null;
        }

        return getEntityWithLockOrNull(ri.resolveEntityClass(), ri.sysId, timeout, unit);
    }

    //--//

    void checkInTransaction()
    {
        if (!m_session.isJoinedToTransaction())
        {
            throw new TransactionRequiredException("No transaction in progress");
        }
    }

    public void beginTransaction()
    {
        if (m_inTransaction == null)
        {
            if (m_session.isDefaultReadOnly())
            {
                throw new RuntimeException("Session in read-only mode");
            }

            m_inTransaction = m_session.beginTransaction();
        }
    }

    public void commit()
    {
        if (m_readonly)
        {
            throw new RuntimeException("Session in read-only mode");
        }

        if (m_inTransaction != null)
        {
            m_inTransaction.commit();
            m_inTransaction = null;
        }
    }

    public void rollback()
    {
        if (m_inTransaction != null)
        {
            m_inTransaction.rollback();
            m_inTransaction = null;
        }
    }

    public void commitAndBeginNewTransactionIfNeeded(int limit)
    {
        if (++m_pendingCount >= limit)
        {
            commitAndBeginNewTransaction();
            m_pendingCount = 0;
        }
    }

    public void commitAndBeginNewTransaction()
    {
        commit();
        beginTransaction();
    }

    //--//

    /**
     * Register for callbacks when a transaction is rolled back.
     *
     * @param callback The callback invoked if the transaction gets rolled back.
     */
    public void onTransactionRollback(RunnableWithException callback)
    {
        onTransactionDone((committed) ->
                          {
                              if (!committed)
                              {
                                  callback.run();
                              }
                          });
    }

    /**
     * Register for callbacks when a transaction is committed.
     *
     * @param callback The callback invoked when the transaction gets committed successfully.
     */
    public void onTransactionCommit(RunnableWithException callback)
    {
        onTransactionDone((committed) ->
                          {
                              if (committed)
                              {
                                  callback.run();
                              }
                          });
    }

    /**
     * Register for callbacks when a transaction is closed.
     *
     * @param callback The callback invoked when the transaction is committed or rolled back.
     */
    public void onTransactionDone(ConsumerWithException<Boolean> callback)
    {
        requireNonNull(callback);

        Transaction transaction = m_session.getTransaction();
        transaction.registerSynchronization(new Synchronization()
        {

            @Override
            public void beforeCompletion()
            {
            }

            @Override
            public void afterCompletion(int status)
            {
                try
                {
                    switch (status)
                    {
                        case Status.STATUS_ROLLEDBACK:
                        case Status.STATUS_UNKNOWN:
                            callback.accept(false);
                            break;

                        case Status.STATUS_COMMITTED:
                            callback.accept(true);
                            break;
                    }
                }
                catch (Exception e)
                {
                    // Ignore exceptions generated by notifications.
                }
            }
        });
    }

    public void scheduleOnTransactionCommit(Runnable callback,
                                            int delay,
                                            TimeUnit units)
    {
        onTransactionCommit(() -> Executors.scheduleOnDefaultPool(callback, delay, units));
    }

    //--//

    public boolean isClosed()
    {
        return m_closed;
    }

    @Override
    public void close()
    {
        try
        {
            if (!m_closed)
            {
                m_closed = true;

                rollback();

                m_session.close();
            }
        }
        finally
        {
            // Always release the rate limiter, no matter what happens.
            m_rateLimiterHolder.close();
        }
    }

    //--//

    public class KeepConnectionAliveHolder implements AutoCloseable
    {
        private boolean            m_closed;
        private ScheduledFuture<?> m_timer;

        KeepConnectionAliveHolder()
        {
            queueRefresh();
        }

        @Override
        public void close()
        {
            m_closed = true;

            ScheduledFuture<?> timer = m_timer;
            if (timer != null)
            {
                timer.cancel(false);
            }
        }

        private void queueRefresh()
        {
            if (!m_closed)
            {
                m_timer = Executors.scheduleOnDefaultPool(this::refresh, 1, TimeUnit.MINUTES);
            }
        }

        private void refresh()
        {
            if (m_closed || isClosed())
            {
                return;
            }

            try
            {
                NativeQuery nativeQuery = createNativeQuery("SELECT 1");
                nativeQuery.getSingleResult();
            }
            catch (Throwable t)
            {
                // Ignore failures.
            }

            queueRefresh();
        }
    }

    public KeepConnectionAliveHolder keepConnectionAlive()
    {
        return new KeepConnectionAliveHolder();
    }

    //--//

    private <R> R callWithTimeout(long timeout,
                                  TimeUnit unit,
                                  Callable<R> callback)
    {
        checkInTransaction();

        //
        // Change the lock timeout settings.
        //
        Dialect dialect = getDialect();
        if (dialect instanceof MariaDB103Dialect)
        {
            //
            // Nothing to do, it supports "for update wait <timeout>"
            //
        }
        else if (dialect instanceof MySQLDialect)
        {
            NativeQuery query = createNativeQuery("SET SESSION innodb_lock_wait_timeout = :timeout").setParameter("timeout", Math.max(1, unit.toSeconds(timeout)));
            query.executeUpdate();
        }
        else if (dialect instanceof H2Dialect)
        {
            NativeQuery query = createNativeQuery("SET LOCK_TIMEOUT :timeout").setParameter("timeout", Math.max(1, unit.toMillis(timeout)));
            query.executeUpdate();
        }

        try
        {
            return callback.call();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            //
            // Restore original lock timeout settings.
            //
            if (dialect instanceof MariaDB103Dialect)
            {
                //
                // Nothing to do, it supports "for update wait <timeout>"
                //
            }
            else if (dialect instanceof MySQLDialect)
            {
                NativeQuery query = createNativeQuery("SET SESSION innodb_lock_wait_timeout = @@GLOBAL.innodb_lock_wait_timeout");
                query.executeUpdate();
            }
        }
    }
}
