/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

/**
 * An helper class to track locked records.
 *
 * @param <E> the class that this Locker manages
 */
public final class RecordLocked<E>
{
    private final SessionHolder m_sessionHolder;
    private final E             m_entity;

    RecordLocked(SessionHolder sessionHolder,
                 E rec)
    {
        m_sessionHolder = sessionHolder;
        m_entity        = rec;
    }

    public E get()
    {
        return m_entity;
    }

    public SessionHolder getSessionHolder()
    {
        return m_sessionHolder;
    }

    public RecordLocator<E> createLocator()
    {
        return m_sessionHolder.createLocator(m_entity);
    }
}
