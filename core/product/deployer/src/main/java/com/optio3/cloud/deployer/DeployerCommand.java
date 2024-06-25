/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.deployer;

import io.dropwizard.Application;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;

public class DeployerCommand extends EnvironmentCommand<DeployerConfiguration>
{
    private final DeployerApplication m_app;

    public DeployerCommand(Application<DeployerConfiguration> application)
    {
        super(application, "deployer", "Runs Deployer logic");

        m_app = (DeployerApplication) application;
    }

    @Override
    protected Class<DeployerConfiguration> getConfigurationClass()
    {
        return DeployerConfiguration.class;
    }

    @Override
    protected void run(Environment environment,
                       Namespace namespace,
                       DeployerConfiguration configuration) throws
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
