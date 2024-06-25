/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.authentication.jwt;

import java.net.URI;

import com.codahale.metrics.health.HealthCheck;
import com.optio3.cloud.authentication.jwt.CookieAuthBundle;
import com.optio3.cloud.authentication.jwt.CookiePrincipalRoleResolver;
import com.optio3.cloud.exception.DetailedApplicationException;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.SimpleServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.DropwizardTestSupport;
import org.junit.rules.ExternalResource;

public class TestApplicationRule extends ExternalResource
{
    protected class FakeApplication extends Application<Configuration>
    {
        private final CookiePrincipalRoleResolver m_resolver;

        public FakeApplication(CookiePrincipalRoleResolver resolver)
        {
            m_resolver = resolver;
        }

        @Override
        public void initialize(Bootstrap<Configuration> bootstrap)
        {
            bootstrap.addBundle(new CookieAuthBundle<>(null, m_resolver));
        }

        @Override
        public void run(Configuration configuration,
                        Environment environment)
        {
            environment.jersey()
                       .register(new DetailedApplicationException.Mapper());

            //choose a random port
            SimpleServerFactory serverConfig = new SimpleServerFactory();
            configuration.setServerFactory(serverConfig);
            HttpConnectorFactory connectorConfig = (HttpConnectorFactory) serverConfig.getConnector();
            connectorConfig.setPort(0);

            //Dummy health check to suppress the startup warning.
            environment.healthChecks()
                       .register("dummy", new HealthCheck()
                       {
                           @Override
                           protected HealthCheck.Result check()
                           {
                               return HealthCheck.Result.healthy();
                           }
                       });

            environment.jersey()
                       .register(new TestResource());
        }
    }

    private final DropwizardTestSupport<Configuration> m_testSupport;

    public TestApplicationRule(CookiePrincipalRoleResolver resolver)
    {
        Configuration configuration = new Configuration();

        m_testSupport = new DropwizardTestSupport<Configuration>(FakeApplication.class, configuration)
        {
            @Override
            public Application<Configuration> newApplication()
            {
                return new FakeApplication(resolver);
            }
        };
    }

    public URI baseUri()
    {
        String path = m_testSupport.getEnvironment()
                                   .getApplicationContext()
                                   .getContextPath();

        return URI.create("http://localhost:" + m_testSupport.getLocalPort() + path);
    }

    public DropwizardTestSupport<Configuration> getSupport()
    {
        return m_testSupport;
    }

    @Override
    protected void before() throws
                            Exception
    {
        m_testSupport.before();
    }

    @Override
    protected void after()
    {
        m_testSupport.after();
    }
}
