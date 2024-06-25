/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway;

import io.dropwizard.Application;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;

public class GatewayCommand extends EnvironmentCommand<GatewayConfiguration>
{
    private final GatewayApplication m_app;

    public GatewayCommand(Application<GatewayConfiguration> application)
    {
        super(application, "gateway", "Runs Gateway logic");

        m_app = (GatewayApplication) application;
    }

    @Override
    protected Class<GatewayConfiguration> getConfigurationClass()
    {
        return GatewayConfiguration.class;
    }

    @Override
    protected void run(Environment environment,
                       Namespace namespace,
                       GatewayConfiguration configuration) throws
                                                           Exception
    {
        AbstractLifeCycle server = new AbstractLifeCycle()
        {
            @Override
            protected void doStart()
            {
                m_app.startLoop();
            }

            @Override
            protected void doStop()
            {
                m_app.stopLoop();
            }
        };

        server.addLifeCycleListener(new LifeCycleListener());
        server.start();

        cleanupAsynchronously();
    }

    //--//

    private class LifeCycleListener extends AbstractLifeCycle.AbstractLifeCycleListener
    {
        @Override
        public void lifeCycleStopped(LifeCycle event)
        {
            cleanup();
        }
    }
}
