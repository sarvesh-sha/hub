/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.util.function.Consumer;

import com.optio3.service.IServiceProvider;

/**
 * An helper class for delaying database updates on Hibernate entities.
 *
 * @param <E> the class that this helper manages
 */
public final class LazyRecordFlusher<E> implements IServiceProvider,
                                                   AutoCloseable
{
    private final RecordHelper<E> m_helper;
    private final E               m_record;
    private final Consumer<E>     m_notifyOnPersist;
    private final boolean         m_isNew;
    private       boolean         m_needsPersisting;

    LazyRecordFlusher(RecordHelper<E> helper,
                      E rec,
                      boolean isNew,
                      Consumer<E> notifyOnPersist)
    {
        m_helper          = helper;
        m_record          = rec;
        m_notifyOnPersist = notifyOnPersist;
        m_isNew           = isNew;

        m_needsPersisting = isNew;
    }

    @Override
    public void close()
    {
        persistIfNeeded();
    }

    @Override
    public <S> S getService(Class<S> serviceClass)
    {
        return m_helper.getService(serviceClass);
    }

    public boolean isNew()
    {
        return m_isNew;
    }

    public E getAfterPersist()
    {
        persistIfNeeded();

        return get();
    }

    public E get()
    {
        return m_record;
    }

    public void persistIfNeeded()
    {
        if (m_needsPersisting)
        {
            m_helper.persist(m_record);
            m_needsPersisting = false;

            if (m_notifyOnPersist != null)
            {
                m_notifyOnPersist.accept(m_record);
            }
        }
    }
}
