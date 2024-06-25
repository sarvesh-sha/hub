/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint;

import java.util.concurrent.Callable;

import com.google.common.collect.Lists;
import com.optio3.cloud.ProxyFactory;
import com.optio3.cloud.client.deployer.model.DeployerCellularInfo;
import com.optio3.cloud.client.waypoint.model.ProvisionTest;
import com.optio3.cloud.client.waypoint.model.ProvisionTestResult;
import com.optio3.concurrency.Executors;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.logging.Logger;
import com.optio3.serialization.ObjectMappers;
import com.sun.jna.Platform;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.apache.commons.lang3.StringUtils;

public class TesterCommand extends Command
{
    public static final Logger LoggerInstance = new Logger(TesterCommand.class);

    private final WaypointApplication                                    m_app;
    private final com.optio3.cloud.client.waypoint.model.ProvisionReport m_report          = new com.optio3.cloud.client.waypoint.model.ProvisionReport();
    private       String                                                 m_provisionServer = "http://192.170.4.1";
    private       ProxyFactory                                           m_proxyFactory;

    public TesterCommand(WaypointApplication app)
    {
        super("tester", "Runs tests and sends serial numbers to Provision Server");

        m_app = app;
    }

    @Override
    public void run(Bootstrap<?> bootstrap,
                    Namespace namespace) throws
                                         Exception
    {
        while (true)
        {
            try
            {
                if (Platform.isIntel())
                {
                    m_report.architecture = com.optio3.cloud.client.waypoint.model.DockerImageArchitecture.X86;
                }
                else if (Platform.isARM())
                {
                    m_report.architecture = com.optio3.cloud.client.waypoint.model.DockerImageArchitecture.ARMv7;
                }

                final FirmwareHelper firmwareHelper = FirmwareHelper.get();
                m_report.boardSerialNumber    = firmwareHelper.getSerialNumber();
                m_report.boardHardwareVersion = String.format("0x%04X", firmwareHelper.getHardwareVersion());
                m_report.boardFirmwareVersion = String.format("0x%04X", firmwareHelper.getFirmwareVersion());

                LoggerInstance.info("Details: %s", ObjectMappers.prettyPrintAsJson(m_report));

                m_report.tests = Lists.newArrayList();

                runTest("Modem", () -> true, () ->
                {
                    if (StringUtils.isBlank(m_report.imei) || StringUtils.equals(m_report.imei, "N/A"))
                    {
                        return com.optio3.cloud.client.waypoint.model.ProvisionTestResult.Failed;
                    }

                    return com.optio3.cloud.client.waypoint.model.ProvisionTestResult.Passed;
                });

                runTest("Accelerometer", firmwareHelper::supportsAccelerometer, () ->
                {
                    return com.optio3.cloud.client.waypoint.model.ProvisionTestResult.Passed;
                });

                LoggerInstance.info("Tests: %s", ObjectMappers.prettyPrintAsJson(m_report.tests));
                com.optio3.cloud.client.waypoint.api.ProvisionApi proxy = createProxy(com.optio3.cloud.client.waypoint.api.ProvisionApi.class);
                proxy.performCheckin(m_report);

                for (ProvisionTest test : m_report.tests)
                {
                    if (test.result == ProvisionTestResult.Failed)
                    {
                        Runtime.getRuntime()
                               .exit(10);
                    }
                }

                Runtime.getRuntime()
                       .exit(0);
                return;
            }
            catch (Throwable t)
            {
                LoggerInstance.error("Failed to send report, due to %s", t);
            }

            Executors.safeSleep(2000);
        }
    }

    private void runTest(String name,
                         Callable<Boolean> supportCallback,
                         Callable<com.optio3.cloud.client.waypoint.model.ProvisionTestResult> executionCallback)
    {
        try
        {
            if (supportCallback.call())
            {
                var test = new com.optio3.cloud.client.waypoint.model.ProvisionTest();
                test.name = name;

                try
                {
                    test.result = executionCallback.call();
                }
                catch (Throwable t)
                {
                    test.result = com.optio3.cloud.client.waypoint.model.ProvisionTestResult.Failed;
                }

                m_report.tests.add(test);
            }
        }
        catch (Exception e)
        {
            LoggerInstance.error("Failed to run test %s, due to %s", name, e);
        }
    }

    @Override
    public void configure(Subparser subparser)
    {
        m_app.addArgumentToCommand(subparser, "--enableLog", "Enable logging", false, WaypointConfiguration::enableLogLevel);

        m_app.addArgumentToCommand(subparser, "--provisionServer", "URL of Provision Server", false, (value) ->
        {
            m_provisionServer = value;
        });

        m_app.addArgumentToCommand(subparser, "--firmwareVersion", "Version of the firmware", true, (value) ->
        {
            m_report.firmwareVersion = value;
        });

        m_app.addArgumentToCommand(subparser, "--stationNumber", "ID of programmer station", false, (value) ->
        {
            m_report.stationNumber = value;
        });

        m_app.addArgumentToCommand(subparser, "--stationProgram", "ID of programmer program", false, (value) ->
        {
            m_report.stationProgram = value;
        });

        m_app.addArgumentToCommand(subparser, "--modemModule", "Modem's module name", false, (value) ->
        {
            m_report.modemModule = value;
        });

        m_app.addArgumentToCommand(subparser, "--modemRevision", "Modem firmware revision", false, (value) ->
        {
            m_report.modemRevision = value;
        });

        m_app.addArgumentToCommand(subparser, "--hostId", "ID of Waypoint", true, (value) ->
        {
            m_report.hostId = value;
        });

        m_app.addArgumentToCommand(subparser, "--IMSI", "IMSI of Waypoint", false, (value) ->
        {
            m_report.imsi = value;
        });

        m_app.addArgumentToCommand(subparser, "--IMEI", "IMEI of Waypoint", false, (value) ->
        {
            m_report.imei = value;
        });

        m_app.addArgumentToCommand(subparser, "--ICCID", "ICCID of Waypoint", false, (value) ->
        {
            m_report.iccid = DeployerCellularInfo.trimFiller(value);
        });
    }

    public <T> T createProxy(Class<T> clz)
    {
        if (m_proxyFactory == null)
        {
            m_proxyFactory = new ProxyFactory(ObjectMappers.SkipNulls);
        }

        return m_proxyFactory.createProxy(m_provisionServer + "/api/v1", clz);
    }
}
