/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.service.IServiceProvider;
import com.optio3.util.function.ConsumerWithException;
import com.optio3.util.function.FunctionWithException;

public final class SessionProvider implements IServiceProvider
{
    private final IServiceProvider    m_serviceProvider;
    private final String              m_databaseId;
    private final Optio3DbRateLimiter m_rateLimiter;

    public SessionProvider(IServiceProvider serviceProvider,
                           String databaseId,
                           Optio3DbRateLimiter rateLimiter)
    {
        m_serviceProvider = serviceProvider;
        m_databaseId      = databaseId;
        m_rateLimiter     = rateLimiter;
    }

    public SessionHolder newReadOnlySession()
    {
        return SessionHolder.createWithNewReadOnlySession(m_serviceProvider, m_databaseId, m_rateLimiter);
    }

    public SessionHolder newSessionWithoutTransaction()
    {
        return SessionHolder.createWithNewSessionWithoutTransaction(m_serviceProvider, m_databaseId, m_rateLimiter);
    }

    public SessionHolder newSessionWithTransaction()
    {
        SessionHolder sessionHolder = newSessionWithoutTransaction();
        sessionHolder.beginTransaction();
        return sessionHolder;
    }

    //--//

    public <T> T computeInReadOnlySession(FunctionWithException<SessionHolder, T> callback) throws
                                                                                            Exception
    {
        try (SessionHolder sessionHolder = newReadOnlySession())
        {
            return callback.apply(sessionHolder);
        }
    }

    public <T> T computeInSessionWithoutTransaction(FunctionWithException<SessionHolder, T> callback) throws
                                                                                                      Exception
    {
        try (SessionHolder sessionHolder = newReadOnlySession())
        {
            return callback.apply(sessionHolder);
        }
    }

    public <T> T computeInSessionWithAutoCommit(FunctionWithException<SessionHolder, T> callback) throws
                                                                                                  Exception
    {
        try (SessionHolder sessionHolder = newReadOnlySession())
        {
            T res = callback.apply(sessionHolder);

            sessionHolder.commit();

            return res;
        }
    }

    //--//

    public void callWithReadOnlySession(ConsumerWithException<SessionHolder> callback) throws
                                                                                       Exception
    {
        try (SessionHolder sessionHolder = newReadOnlySession())
        {
            callback.accept(sessionHolder);
        }
    }

    public void callWithSessionWithoutTransaction(ConsumerWithException<SessionHolder> callback) throws
                                                                                                 Exception
    {
        try (SessionHolder sessionHolder = newSessionWithoutTransaction())
        {
            callback.accept(sessionHolder);
        }
    }

    public void callWithSessionWithAutoCommit(ConsumerWithException<SessionHolder> callback) throws
                                                                                             Exception
    {
        try (SessionHolder sessionHolder = newSessionWithTransaction())
        {
            callback.accept(sessionHolder);

            sessionHolder.commit();
        }
    }

    //--//

    @Override
    public <S> S getService(Class<S> serviceClass)
    {
        return m_serviceProvider.getService(serviceClass);
    }
}
