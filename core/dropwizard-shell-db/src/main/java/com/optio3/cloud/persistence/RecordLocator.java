/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

/**
 * An helper class to track records offline.
 *
 * @param <E> the class that this Locator manages
 */
public final class RecordLocator<E>
{
    private final Class<E>      m_entityClass;
    private final Class<?>      m_idClass;
    private final String        m_id;
    private final ZonedDateTime m_lastUpdate;

    @JsonCreator
    public RecordLocator(@JsonProperty("entityClass") Class<E> entityClass,
                         @JsonProperty("idClass") Class<?> idClass,
                         @JsonProperty("id") String id)
    {
        m_entityClass = entityClass;
        m_idClass     = checkForString(idClass);
        m_id          = id;
        m_lastUpdate  = null;
    }

    public RecordLocator(Class<E> entityClass,
                         String id)
    {
        m_entityClass = entityClass;
        m_idClass     = null;
        m_id          = id;
        m_lastUpdate  = null;
    }

    RecordLocator(SessionHolder sessionHolder,
                  E entity)
    {
        @SuppressWarnings("unchecked") Class<E> entityClass = (Class<E>) SessionHolder.getClassOfEntity(entity);

        m_entityClass = entityClass;

        Serializable id = sessionHolder.getIdentifier(entity);
        m_idClass = checkForString(id.getClass());

        if (m_idClass == null)
        {
            m_id = (String) id;
        }
        else
        {
            try
            {
                m_id = ObjectMappers.SkipNulls.writeValueAsString(id);
            }
            catch (JsonProcessingException e)
            {
                throw new RuntimeException(e);
            }
        }

        RecordWithCommonFields record = Reflection.as(entity, RecordWithCommonFields.class);
        m_lastUpdate = RecordWithCommonFields.getUpdatedOnSafe(record, true); // If it's a proxy, don't set lastUpdate, it would cause the record to be fetched!
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        RecordLocator<?> that = Reflection.as(o, RecordLocator.class);
        if (that == null)
        {
            return false;
        }

        return m_entityClass == that.m_entityClass && Objects.equals(m_id, that.m_id);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(m_id);
    }

    //--//

    /**
     * @param rec The target entity
     *
     * @return true if {@code rec} is the same entity tracked by this locator
     */
    public boolean sameRecord(RecordWithCommonFields rec)
    {
        return rec != null && SessionHolder.getClassOfEntity(rec) == m_entityClass && StringUtils.equals(rec.getSysId(), m_id);
    }

    /**
     * Returns a locator for the given record identity, which can be used to retrieve the entity from the database at a later time.
     *
     * @param ri target entity
     *
     * @return the new locator
     */
    public static <T extends RecordWithCommonFields> RecordLocator<T> create(RecordHelper<T> helper,
                                                                             TypedRecordIdentity<T> ri)
    {
        return ri != null ? new RecordLocator<>(helper.getEntityClass(), String.class, ri.sysId) : null;
    }

    /**
     * Returns locators for the given record identities, which can be used to retrieve the entity from the database at a later time.
     *
     * @param riLst target entities
     *
     * @return a list of new locators
     */
    public static <T extends RecordWithCommonFields> List<RecordLocator<T>> createList(RecordHelper<T> helper,
                                                                                       TypedRecordIdentityList<T> riLst)
    {
        List<RecordLocator<T>> res = Lists.newArrayList();

        for (TypedRecordIdentity<T> ri : riLst)
        {
            res.add(create(helper, ri));
        }

        return res;
    }

    @JsonProperty
    public Class<E> getEntityClass()
    {
        return m_entityClass;
    }

    @JsonProperty
    public Class<?> getIdClass()
    {
        return m_idClass;
    }

    @JsonProperty("id")
    public String getIdRaw()
    {
        return m_id;
    }

    @JsonIgnore
    public Serializable getId()
    {
        if (m_idClass == null)
        {
            // We don't encode String Ids.
            return m_id;
        }

        try
        {
            return (Serializable) ObjectMappers.SkipNulls.readValue(m_id, m_idClass);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @JsonIgnore
    public ZonedDateTime getLastUpdate()
    {
        return m_lastUpdate;
    }

    //--//

    /**
     * Returns the entity corresponding to this locator.
     *
     * @param sessionHolder session holder for entity
     *
     * @return the entity
     */
    E get(SessionHolder sessionHolder)
    {
        return sessionHolder.getEntity(m_entityClass, getId());
    }

    /**
     * Returns the entity corresponding to this locator, or {@code null} if the record doesn't exist.
     *
     * @param sessionHolder session holder for entity
     *
     * @return the entity or {@code null}
     */
    E getOrNull(SessionHolder sessionHolder)
    {
        return sessionHolder.getEntityOrNull(m_entityClass, getId());
    }

    /**
     * Returns the entity corresponding to this locator, acquiring a pessimistic write lock.
     *
     * @param sessionHolder session holder for entity
     * @param timeout       wait time for lock
     * @param unit          unit for wait
     *
     * @return the entity
     */
    RecordLocked<E> getWithLock(SessionHolder sessionHolder,
                                long timeout,
                                TimeUnit unit)
    {
        return sessionHolder.getEntityWithLock(m_entityClass, getId(), timeout, unit);
    }

    /**
     * Returns the entity corresponding to this locator, or {@code null} if the record doesn't exist, acquiring a pessimistic write lock.
     *
     * @param sessionHolder session holder for entity
     * @param timeout       wait time for lock
     * @param unit          unit for wait
     *
     * @return the entity or {@code null}
     */
    RecordLocked<E> getWithLockOrNull(SessionHolder sessionHolder,
                                      long timeout,
                                      TimeUnit unit)
    {
        return sessionHolder.getEntityWithLockOrNull(m_entityClass, getId(), timeout, unit);
    }

    //--//

    private Class<?> checkForString(Class<?> idClass)
    {
        return idClass == String.class ? null : idClass;
    }

    //--//

    @Override
    public String toString()
    {
        return String.format("Locator[%s:%s]", m_entityClass.getSimpleName(), m_id);
    }
}
