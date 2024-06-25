/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.db;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.AbstractConfigurationWithDatabase;
import com.optio3.util.Exceptions;
import io.dropwizard.Application;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class RefreshLiquibaseCommand<T extends AbstractConfigurationWithDatabase> extends ConfiguredCommand<T>
{
    private final AbstractApplicationWithDatabase<T> m_app;
    private final Class<T>                           m_configurationClass;

    private String  m_dbFlavor;
    private String  m_targetRoot;
    private Integer m_migrationsRevisionLevel;
    private Integer m_migrationsVersionNumber;
    private String  m_migrationsId;

    public RefreshLiquibaseCommand(Application<T> application)
    {
        super("refreshDb", "Runs Liquibase migrations and Hibernate auto-mode, to update migrations file");

        m_configurationClass = application.getConfigurationClass();

        m_app = (AbstractApplicationWithDatabase<T>) application;
    }

    @Override
    protected Class<T> getConfigurationClass()
    {
        return m_configurationClass;
    }

    @Override
    protected void run(Bootstrap<T> bootstrap,
                       Namespace namespace,
                       T configuration) throws
                                        Exception
    {
        Optio3DataSourceFactory dataSourceFactory = configuration.getDataSourceFactory();

        dataSourceFactory.autoHibernateMode = true;
        dataSourceFactory.saveMigrationDelta = m_targetRoot;
        dataSourceFactory.migrationsRevisionLevel = m_migrationsRevisionLevel;
        dataSourceFactory.migrationsVersionNumber = m_migrationsVersionNumber;
        dataSourceFactory.migrationsRoot = m_migrationsId;

        switch (m_dbFlavor)
        {
            case "h2":
                dataSourceFactory.setDriverClass("org.h2.Driver");
                dataSourceFactory.setUrl("jdbc:h2:mem:refresh_liquibase;MODE=DB2;IGNORECASE=TRUE");
                dataSourceFactory.setUser("sa");
                dataSourceFactory.setPassword("sa");
                break;

            case "mysql":
                dataSourceFactory.setDriverClass("org.mariadb.jdbc.Driver");
                dataSourceFactory.setUrl("jdbc:mysql://localhost:3306/refresh_liquibase?createDatabaseIfNotExist=true");
                dataSourceFactory.setUser("root");
                dataSourceFactory.setPassword("");
                break;

            default:
                throw Exceptions.newRuntimeException("Unknown database %s", m_dbFlavor);
        }

        //--//

        try
        {
            String name = bootstrap.getApplication()
                                   .getName();

            MetricRegistry      metricRegistry      = bootstrap.getMetricRegistry();
            HealthCheckRegistry healthCheckRegistry = bootstrap.getHealthCheckRegistry();

            final Environment environment = new Environment(name,
                                                            bootstrap.getObjectMapper(),
                                                            bootstrap.getValidatorFactory(),
                                                            metricRegistry,
                                                            bootstrap.getClassLoader(),
                                                            healthCheckRegistry,
                                                            configuration);

            configuration.getMetricsFactory()
                         .configure(environment.lifecycle(), metricRegistry);
            configuration.getServerFactory()
                         .configure(environment);

            bootstrap.run(configuration, environment);
            m_app.refreshDatabase(configuration, environment);
        }
        finally
        {
            //
            // Let's drop all the objects from the database.
            //
            dataSourceFactory.dropDatabase();
        }
    }

    @Override
    public void configure(Subparser subparser)
    {
        super.configure(subparser);

        m_app.addArgumentToCommand(subparser, "--db", "the database to use as target", true, (value) ->
        {
            m_dbFlavor = value;
        });

        m_app.addArgumentToCommand(subparser, "--targetRoot", "the directory to store the output from Liquibase", true, (value) ->
        {
            m_targetRoot = value;
        });

        m_app.addArgumentToCommand(subparser, "--migrationsRevisionLevel", "the revision level of the target changesets", true, (value) ->
        {
            m_migrationsRevisionLevel = Integer.parseInt(value);
        });

        m_app.addArgumentToCommand(subparser, "--migrationsVersionNumber", "the version of the target changesets", true, (value) ->
        {
            m_migrationsVersionNumber = Integer.parseInt(value);
        });

        m_app.addArgumentToCommand(subparser, "--migrationsId", "root for the new changesets", true, (value) ->
        {
            m_migrationsId = value;
        });
    }
}
