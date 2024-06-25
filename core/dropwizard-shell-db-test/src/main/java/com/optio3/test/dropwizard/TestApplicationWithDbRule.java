/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.dropwizard;

import java.util.function.Function;

import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.AbstractConfigurationWithDatabase;
import com.optio3.cloud.db.Optio3DataSourceFactory;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.function.ConsumerWithException;
import io.dropwizard.Application;
import io.dropwizard.cli.Command;

public class TestApplicationWithDbRule<A extends AbstractApplicationWithDatabase<C>, C extends AbstractConfigurationWithDatabase> extends TestApplicationRule<A, C>
{
    private boolean m_dropDatabaseOnExit = true;

    public TestApplicationWithDbRule(Class<A> applicationClass,
                                     String configurationResource,
                                     ConsumerWithException<C> configurationCallback,
                                     ConsumerWithException<A> applicationCallback)
    {
        super(applicationClass, configurationResource, configurationCallback, applicationCallback);
    }

    public TestApplicationWithDbRule(Class<A> applicationClassArg,
                                     String configurationResource,
                                     ConsumerWithException<C> configurationCallback,
                                     ConsumerWithException<A> applicationCallback,
                                     Function<Application<C>, Command> commandInstantiator)
    {
        super(applicationClassArg, configurationResource, configurationCallback, applicationCallback, commandInstantiator);
    }

    //--//

    public void drainDatabaseEvents()
    {
        getApplication().drainDatabaseEvents(null);
    }

    public SessionHolder openSessionWithoutTransaction()
    {
        return SessionHolder.createWithNewSessionWithoutTransaction(getApplication(), null, Optio3DbRateLimiter.Normal);
    }

    public SessionHolder openSessionWithTransaction()
    {
        return SessionHolder.createWithNewSessionWithTransaction(getApplication(), null, Optio3DbRateLimiter.Normal);
    }

    public void dropDatabaseOnExit(boolean drop)
    {
        m_dropDatabaseOnExit = drop;
    }

    //--//

    @Override
    protected void after()
    {
        C cfg = m_testSupport.getConfiguration();

        super.after();

        if (m_dropDatabaseOnExit)
        {
            //
            // After a test, we drop all the objects from the database.
            //
            try
            {
                Optio3DataSourceFactory dataSourceFactory = cfg.getDataSourceFactory();

                dataSourceFactory.dropDatabase();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
