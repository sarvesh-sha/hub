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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.persistence.EntityExistsException;
import javax.persistence.LockTimeoutException;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.TransactionRequiredException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.db.Optio3DataSourceFactory;
import com.optio3.service.IServiceProvider;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.NaturalIdLoadAccess;

/**
 * An helper class for Hibernate entities.
 *
 * @param <E> the class that this helper manages
 */
public final class RecordHelper<E> implements IServiceProvider
{
    private static final ConcurrentMap<Class<?>, String> s_entityTables         = Maps.newConcurrentMap();
    private static final ConcurrentMap<String, Class<?>> s_entityTablesReverse  = Maps.newConcurrentMap();
    private static final Multimap<Class<?>, Class<?>>    s_entityTableHierarchy = HashMultimap.create();

    private final SessionHolder m_sessionHolder;
    private final Class<E>      m_entityClass;

    private String  m_entityTable;
    private boolean m_lockedTable;

    //--//

    /**
     * Creates a new {@link RecordHelper} with a given session.
     *
     * @param sessionHolder a SessionHolder to access session and application's services
     * @param entityClass   the target Entity class
     */
    RecordHelper(SessionHolder sessionHolder,
                 Class<E> entityClass)
    {
        m_sessionHolder = requireNonNull(sessionHolder);
        m_entityClass   = requireNonNull(entityClass);
    }

    //--//

    @Override
    public <S> S getService(Class<S> serviceClass)
    {
        return m_sessionHolder.getService(serviceClass);
    }

    //--//

    /**
     * Returns a new {@link RecordHelper} for a different Entity class.
     *
     * @param entityClass the target Entity class
     *
     * @return the new helper
     */
    public <E2> RecordHelper<E2> wrapFor(Class<E2> entityClass)
    {
        return new RecordHelper<>(m_sessionHolder, entityClass);
    }

    /**
     * Returns a locator for the given entity, which can be used to retrieve the entity from the database at a later time.
     *
     * @param entity target entity
     *
     * @return the new locator
     */
    public RecordLocator<E> asLocator(E entity)
    {
        return m_sessionHolder.createLocator(entity);
    }

    /**
     * Returns a lazy flusher for an existing record, an helper that reduces the number of DB updates by delaying inserts.
     *
     * @param rec The existing record to wrap
     *
     * @return the new lazy helper
     */
    public LazyRecordFlusher<E> wrapAsExistingRecord(E rec)
    {
        return new LazyRecordFlusher<>(this, rec, false, null);
    }

    /**
     * Returns a lazy flusher for a new record, an helper that reduces the number of DB updates by delaying inserts.
     *
     * @param rec The new record to wrap
     *
     * @return the new lazy helper
     */
    public LazyRecordFlusher<E> wrapAsNewRecord(E rec)
    {
        return wrapAsNewRecord(rec, null);
    }

    /**
     * Returns a lazy flusher for a new record, an helper that reduces the number of DB updates by delaying inserts.
     *
     * @param rec The new record to wrap
     *
     * @return the new lazy helper
     */
    public LazyRecordFlusher<E> wrapAsNewRecord(E rec,
                                                Consumer<E> notifyOnPersist)
    {
        return new LazyRecordFlusher<>(this, rec, true, notifyOnPersist);
    }

    /**
     * Returns the entity associated with a locator.
     *
     * @param locator locator for entity
     *
     * @return the entity
     */
    public E fromLocator(RecordLocator<E> locator)
    {
        return m_sessionHolder.fromLocator(locator);
    }

    /**
     * Returns the entity associated with a locator, or {@code null} if the record doesn't exist.
     *
     * @param locator locator for entity
     *
     * @return the entity or {@code null}
     */
    public E fromLocatorOrNull(RecordLocator<? extends E> locator)
    {
        return m_sessionHolder.fromLocatorOrNull(locator);
    }

    //--//

    /**
     * Returns the current {@link SessionHolder}.
     *
     * @return the current session holder
     */
    public SessionHolder currentSessionHolder()
    {
        return m_sessionHolder;
    }

    /**
     * Returns the {@link SessionProvider} for the current session.
     *
     * @return the associated session provider
     */
    public SessionProvider getSessionProvider()
    {
        return m_sessionHolder.getSessionProvider();
    }

    //--//

    public void queueDefragmentation()
    {
        AbstractApplicationWithDatabase<?> app         = getServiceNonNull(AbstractApplicationWithDatabase.class);
        Optio3DataSourceFactory            dataFactory = app.getDataSourceFactory(null);

        queueHierarchicalDefragmentation(dataFactory, m_entityClass);
    }

    private static void queueHierarchicalDefragmentation(Optio3DataSourceFactory dataFactory,
                                                         Class<?> clz)
    {
        dataFactory.queueDefragmentation(clz);

        synchronized (s_entityTableHierarchy)
        {
            for (Class<?> childClz : s_entityTableHierarchy.get(clz))
            {
                queueHierarchicalDefragmentation(dataFactory, childClz);
            }
        }
    }

    //--//

    /**
     * Returns a new typed query criteria.
     *
     * @return the new query criteria
     */
    public CriteriaQuery<E> createQuery()
    {
        return m_sessionHolder.getCriteriaBuilder()
                              .createQuery(m_entityClass);
    }

    /**
     * Returns a typed {@link Query<E>}
     *
     * @param queryString JPQL query
     *
     * @return typed query
     */
    public TypedQuery<E> query(String queryString)
    {
        return m_sessionHolder.createQuery(queryString, getEntityClass());
    }

    /**
     * Returns the entity class managed by this helper.
     *
     * @return the entity class managed by this helper
     */
    public Class<E> getEntityClass()
    {
        return m_entityClass;
    }

    /**
     * Returns the name of the table associated with the class managed by this helper.
     *
     * @return the name of the table associated with this entity
     */
    public String getEntityTable()
    {
        if (m_entityTable == null)
        {
            m_entityTable = getEntityTable(m_entityClass);
        }

        return m_entityTable;
    }

    public static String registerEntityTable(Class<?> entityClass)
    {
        String foundTable = null;

        Class<?> clzChild = null;

        for (Class<?> clzPtr = entityClass; clzPtr != null; clzPtr = clzPtr.getSuperclass())
        {
            Table anno = clzPtr.getAnnotation(Table.class);
            if (anno != null && StringUtils.isNotBlank(anno.name()))
            {
                Optio3TableInfo anno2 = clzPtr.getAnnotation(Optio3TableInfo.class);
                if (anno2 == null)
                {
                    throw Exceptions.newRuntimeException("Invalid class: cannot find @Optio3TableInfo for Entity from class %s", clzPtr);
                }

                String table      = anno.name();
                String externalId = anno2.externalId();

                s_entityTablesReverse.putIfAbsent(table, clzPtr);
                s_entityTablesReverse.putIfAbsent(externalId, clzPtr);
                s_entityTables.putIfAbsent(clzPtr, table);

                if (clzChild != null)
                {
                    synchronized (s_entityTableHierarchy)
                    {
                        s_entityTableHierarchy.put(clzPtr, clzChild);
                    }
                }

                clzChild = clzPtr;

                if (foundTable == null)
                {
                    foundTable = table;
                }
            }
        }

        if (foundTable == null)
        {
            throw Exceptions.newRuntimeException("Invalid class: cannot find table for Entity from class %s", entityClass);
        }

        // Register the original class, in case it's a proxy.
        s_entityTables.putIfAbsent(entityClass, foundTable);

        return foundTable;
    }

    /**
     * Returns the name of the table associated with the input class.
     *
     * @return the name of the table associated with the entity class
     */
    public static String getEntityTable(Class<?> entityClass)
    {
        String table = s_entityTables.get(entityClass);
        if (table == null)
        {
            table = registerEntityTable(entityClass);
        }

        return table;
    }

    /**
     * Returns all the Entity tables.
     *
     * @return set of all registered Entity tables.
     */
    public static Set<Class<?>> getEntityTables()
    {
        Set<Class<?>> set = Sets.newHashSet();
        set.addAll(s_entityTables.keySet());
        return set;
    }

    /**
     * Returns the class of the table associated with the input table name.
     *
     * @return the class of the table associated with the entity name
     */
    public static Class<?> resolveEntityClass(String tableName)
    {
        return tableName != null ? s_entityTablesReverse.get(tableName) : null;
    }

    //--//

    /**
     * Create a {@link NaturalIdLoadAccess} instance to retrieve the specified entity by
     * its natural id.
     *
     * @return load delegate for loading the specified entity type by natural id
     *
     * @throws HibernateException If the specified Class cannot be resolved as a mapped entity
     */
    public NaturalIdLoadAccess<E> byNaturalId()
    {
        return m_sessionHolder.byNaturalId(m_entityClass);
    }

    /**
     * Convenience method to return a single instance that matches the criteria query,
     * or {@code null} if the criteria returns no results.
     *
     * @param criteriaQuery the {@link CriteriaQuery} query to run
     *
     * @return the single result or {@code null}
     */
    public E uniqueResult(CriteriaQuery<E> criteriaQuery)
    {
        return uniqueResult(m_sessionHolder.createQuery(requireNonNull(criteriaQuery)));
    }

    /**
     * Convenience method to return a single instance that matches the query, or {@code null} if the query
     * returns no results.
     *
     * @param query the query to run
     *
     * @return the single result or {@code null}
     *
     * @throws HibernateException if there is more than one matching result
     * @see TypedQuery#getSingleResult()
     */
    public E uniqueResult(TypedQuery<E> query)
    {
        return SessionHolder.unwrapProxy(requireNonNull(query).getSingleResult());
    }

    /**
     * Gets the results of a {@link CriteriaQuery} query.
     *
     * @param criteria the {@link CriteriaQuery} query to run
     *
     * @return the list of matched query results
     */
    public List<E> list(CriteriaQuery<E> criteria)
    {
        return list(m_sessionHolder.createQuery(criteria));
    }

    /**
     * Gets all the records in an Entity.
     *
     * @return the list of all the records in an Entity
     */
    public List<E> listAll()
    {
        CriteriaQuery<E> qdef = createQuery();
        qdef.from(m_entityClass);
        return list(qdef);
    }

    /**
     * Get the results of a query.
     *
     * @param query the query to run
     *
     * @return the list of matched query results
     *
     * @see Query#getResultList()
     */
    public List<E> list(TypedQuery<E> query)
    {
        return SessionHolder.unwrapProxies(requireNonNull(query).getResultList());
    }

    /**
     * Returns the persistent instance of {@code <E>} with the given identifier, or {@code null} if
     * there is no such persistent instance. (If the instance, or a proxy for the instance, is
     * already associated with the session, return that instance or proxy.)
     *
     * @param id an identifier
     *
     * @return a persistent instance or {@code null}
     *
     * @see SessionHolder#getEntityOrNull(Class, Serializable)
     */
    public E getOrNull(Serializable id)
    {
        return m_sessionHolder.getEntityOrNull(m_entityClass, id);
    }

    /**
     * Returns the persistent instance of {@code <E>} with the given identifier, acquiring a pessimistic write lock, or {@code null} if
     * there is no such persistent instance.
     * (If the instance, or a proxy for the instance, is already associated with the session, return that instance or proxy.)
     *
     * @param id      an identifier
     * @param timeout wait time for lock
     * @param unit    unit for wait
     *
     * @return a persistent instance or {@code null}
     *
     * @see SessionHolder#getEntityOrNull(Class, Serializable)
     */
    public RecordLocked<E> getWithLockOrNull(Serializable id,
                                             long timeout,
                                             TimeUnit unit)
    {
        return m_sessionHolder.getEntityWithLockOrNull(m_entityClass, id, timeout, unit);
    }

    /**
     * Returns the persistent instance of {@code <E>} with the given identifier.
     * (If the instance, or a proxy for the instance, is already associated with the session, return that instance or proxy.)
     *
     * @param id an identifier
     *
     * @return a persistent instance
     *
     * @see SessionHolder#getEntity(Class, Serializable)
     */
    public E get(Serializable id)
    {
        return m_sessionHolder.getEntity(m_entityClass, id);
    }

    /**
     * Returns the persistent instance of {@code <E>} with the given identifier, acquiring a pessimistic write lock.
     * (If the instance, or a proxy for the instance, is already associated with the session, return that instance or proxy.)
     *
     * @param id      an identifier
     * @param timeout wait time for lock
     * @param unit    unit for wait
     *
     * @return a persistent instance
     */
    public RecordLocked<E> getWithLock(Serializable id,
                                       long timeout,
                                       TimeUnit unit)
    {
        return m_sessionHolder.getEntityWithLock(m_entityClass, id, timeout, unit);
    }

    public <E2 extends RecordWithCommonFields> RecordLocked<E2> optimisticallyUpgradeToLocked(E2 entity,
                                                                                              long timeout,
                                                                                              TimeUnit unit)
    {
        return m_sessionHolder.optimisticallyUpgradeToLocked(entity, timeout, unit);
    }

    /**
     * Makes an instance managed and persistent.
     *
     * @param entity entity instance
     *
     * @throws EntityExistsException        if the entity already exists.
     *                                      (If the entity already exists, the <code>EntityExistsException</code> may
     *                                      be thrown when the persist operation is invoked, or the
     *                                      <code>EntityExistsException</code> or another <code>PersistenceException</code> may be
     *                                      thrown at flush or commit time.)
     * @throws IllegalArgumentException     if the instance is not an
     *                                      entity
     * @throws TransactionRequiredException if invoked on a container-managed entity manager of type
     *                                      <code>PersistenceContextType.TRANSACTION</code> and there is
     *                                      no transaction
     */
    public RecordLocked<E> persist(E entity)
    {
        return m_sessionHolder.persistEntity(entity);
    }

    /**
     * Deletes an instance from the database.
     *
     * @param entity entity instance
     *
     * @throws IllegalArgumentException     if the instance is not an entity
     * @throws TransactionRequiredException if invoked on a container-managed entity manager of type
     *                                      <code>PersistenceContextType.TRANSACTION</code> and there is
     *                                      no transaction
     */
    public E delete(E entity)
    {
        checkInTransaction();

        m_sessionHolder.deleteEntity(entity);

        return entity;
    }

    /**
     * Remove this instance from the session cache.
     *
     * @param entity entity instance
     */
    public void evict(E entity)
    {
        m_sessionHolder.evictEntity(entity);
    }

    /**
     * Flushes this instance and then removes it from the session cache.
     *
     * @param entity entity instance
     */
    public void flushAndEvict(E entity)
    {
        flush();

        evict(entity);
    }

    /**
     * Locks a table in the database.
     *
     * @param timeout wait time for lock
     * @param unit    unit for wait
     *
     * @return An autoclosable object that will release the lock when closed
     *
     * @throws LockTimeoutException if it fails to grab a lock on the target table
     */
    public TableLockHolder lockTable(long timeout,
                                     TimeUnit unit) throws
                                                    LockTimeoutException
    {
        ITableLockProvider provider = getService(ITableLockProvider.class);
        if (provider == null)
        {
            throw new RuntimeException("Table-level locking not supported");
        }

        return provider.lockTable(getSessionProvider(), getEntityClass(), timeout, unit);
    }

    /**
     * Locks parts of a table in the database.
     *
     * @param subId   The section of the table to lock
     * @param timeout wait time for lock
     * @param unit    unit for wait
     *
     * @return An autoclosable object that will release the lock when closed
     *
     * @throws LockTimeoutException if it fails to grab a lock on the target table
     */
    public TableLockHolder lockTableAndRecord(String subId,
                                              long timeout,
                                              TimeUnit unit) throws
                                                             LockTimeoutException
    {
        ITableLockProvider provider = getService(ITableLockProvider.class);
        if (provider == null)
        {
            throw new RuntimeException("Table-level locking not supported");
        }

        return provider.lockRecord(getSessionProvider(), getEntityClass(), subId, timeout, unit);
    }

    /**
     * Locks a table in the database until the end of the current transaction.
     *
     * @param timeout wait time for lock
     * @param unit    unit for wait
     *
     * @throws LockTimeoutException if it fails to grab a lock on the target table
     */
    public void lockTableUntilEndOfTransaction(long timeout,
                                               TimeUnit unit) throws
                                                              LockTimeoutException
    {
        if (!m_lockedTable)
        {
            checkInTransaction();

            TableLockHolder lockHolder = lockTable(timeout, unit);

            m_lockedTable = true;
            m_sessionHolder.onTransactionDone((committed) ->
                                              {
                                                  lockHolder.close();
                                                  m_lockedTable = false;
                                              });
        }
    }

    /**
     * Flushes the current session to the database.
     *
     * @throws HibernateException Indicates problems flushing the session or talking to the database.
     */
    public void flush()
    {
        m_sessionHolder.flush();
    }

    //--//

    private void checkInTransaction()
    {
        m_sessionHolder.checkInTransaction();
    }
}
