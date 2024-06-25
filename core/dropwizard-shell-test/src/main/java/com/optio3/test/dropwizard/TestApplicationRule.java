/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.dropwizard;

import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.validation.constraints.NotNull;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.base.Stopwatch;
import com.optio3.cloud.AbstractApplication;
import com.optio3.cloud.AbstractConfiguration;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.serialization.Reflection;
import com.optio3.util.function.ConsumerWithException;
import io.dropwizard.Application;
import io.dropwizard.cli.Command;
import io.dropwizard.cli.ServerCommand;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.DropwizardTestSupport;
import org.eclipse.jetty.util.Uptime;
import org.junit.rules.ExternalResource;

public class TestApplicationRule<A extends AbstractApplication<C>, C extends AbstractConfiguration> extends ExternalResource
{
    private static class DummyHealthCheck extends HealthCheck
    {
        @Override
        protected Result check()
        {
            return Result.healthy();
        }
    }

    //--//

    protected DropwizardTestSupport<C> m_testSupport;
    private   Uptime.Impl              m_oldImpl;

    public TestApplicationRule(Class<A> applicationClass,
                               String configurationResource,
                               ConsumerWithException<C> configurationCallback,
                               ConsumerWithException<A> applicationCallback)
    {
        this(applicationClass, configurationResource, configurationCallback, applicationCallback, null);
    }

    public TestApplicationRule(Class<A> applicationClassArg,
                               String configurationResource,
                               ConsumerWithException<C> configurationCallback,
                               ConsumerWithException<A> applicationCallback,
                               Function<Application<C>, Command> commandInstantiator)
    {
        if (commandInstantiator == null)
        {
            commandInstantiator = ServerCommand::new;
        }

        m_testSupport = new DropwizardTestSupport<C>(applicationClassArg, configurationResource, (String) null, commandInstantiator)
        {
            @Override
            public AbstractApplication<C> newApplication()
            {
                AbstractApplication<C> app = (AbstractApplication<C>) Reflection.newInstance(applicationClass);

                app.registerService(AbstractApplication.IConfigurationInspector.class, () -> new AbstractApplication.IConfigurationInspector()
                {
                    @Override
                    public void beforeInitialize(Bootstrap<?> bootstrap)
                    {
                        bootstrap.setConfigurationSourceProvider(new ConfigurationSourceProvider()
                        {
                            @Override
                            public InputStream open(String path)
                            {
                                InputStream result = getResourceAsStream(path);
                                return result == null && path.startsWith("/") ? getResourceAsStream(path.substring(1)) : result;
                            }

                            private InputStream getResourceAsStream(String path)
                            {
                                return applicationClassArg.getClassLoader()
                                                          .getResourceAsStream(path);
                            }
                        });
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public void beforeRun(AbstractConfiguration configuration,
                                          Environment environment) throws
                                                                   Exception
                    {
                        environment.healthChecks()
                                   .register("dummy", new DummyHealthCheck());

                        configuration.markAsUnitTest();

                        if (configurationCallback != null)
                        {
                            configurationCallback.accept((C) configuration);
                        }

                        if (applicationCallback != null)
                        {
                            applicationCallback.accept((A) app);
                        }
                    }
                });

                return app;
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

    public <P> P createProxy(String path,
                             Class<P> cls,
                             Object... varValues)
    {
        return createProxy(baseUri(), path, cls, varValues);
    }

    public <P> P createProxy(URI baseUri,
                             String path,
                             Class<P> cls,
                             Object... varValues)
    {
        A app = getApplication();

        URI fullPath = baseUri.resolve(path);
        return app.createProxy(fullPath.toString(), cls, varValues);
    }

    public <P> P createProxyWithPrincipal(String path,
                                          Class<P> cls,
                                          @NotNull CookiePrincipal principal,
                                          Object... varValues)
    {
        A app = getApplication();

        URI fullPath = baseUri().resolve(path);
        return app.createProxyWithPrincipal(fullPath.toString(), cls, principal, varValues);
    }

    public <P> P createProxyWithCredentials(String path,
                                            Class<P> cls,
                                            String userName,
                                            String password,
                                            Object... varValues)
    {
        A app = getApplication();

        URI fullPath = baseUri().resolve(path);
        return app.createProxyWithCredentials(fullPath.toString(), cls, userName, password, varValues);
    }

    //--//

    public A getApplication()
    {
        @SuppressWarnings("unchecked") A app = (A) m_testSupport.getApplication();

        return app;
    }

    public DropwizardTestSupport<C> getSupport()
    {
        return m_testSupport;
    }

    public void invokeBefore() throws
                               Exception
    {
        before();
    }

    public void invokeAfter()
    {
        after();
    }

    //--//

    @Override
    protected void before() throws
                            Exception
    {
        if (m_oldImpl == null)
        {
            Uptime inst = Uptime.getInstance();
            m_oldImpl = inst.getImpl();

            Stopwatch st = Stopwatch.createStarted();

            inst.setImpl(() -> st.elapsed(TimeUnit.MILLISECONDS));
        }

        m_testSupport.before();
    }

    @Override
    protected void after()
    {
        if (m_oldImpl != null)
        {
            Uptime inst = Uptime.getInstance();
            inst.setImpl(m_oldImpl);
        }

        m_testSupport.after();

        try
        {
            getApplication().cleanupOnShutdown(10, TimeUnit.SECONDS);
        }
        catch (Exception ex)
        {
        }

        m_testSupport = null;
    }
}
