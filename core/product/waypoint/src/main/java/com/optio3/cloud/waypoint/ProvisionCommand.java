/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint;

import com.optio3.cloud.ProxyFactory;
import com.optio3.cloud.client.deployer.model.DeployerCellularInfo;
import com.optio3.concurrency.Executors;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.logging.Logger;
import com.optio3.serialization.ObjectMappers;
import com.sun.jna.Platform;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class ProvisionCommand extends Command
{
    public static final Logger LoggerInstance = new Logger(ProvisionCommand.class);

    private final WaypointApplication                                    m_app;
    private final com.optio3.cloud.client.waypoint.model.ProvisionReport m_report          = new com.optio3.cloud.client.waypoint.model.ProvisionReport();
    private       String                                                 m_provisionServer = "http://192.170.4.1";
    private       ProxyFactory                                           m_proxyFactory;

    public ProvisionCommand(WaypointApplication app)
    {
        super("provision", "Sends serial numbers to Provision Server");

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
                m_report.boardSerialNumber = firmwareHelper.getSerialNumber();
                m_report.boardHardwareVersion = String.format("0x%04X", firmwareHelper.getHardwareVersion());
                m_report.boardFirmwareVersion = String.format("0x%04X", firmwareHelper.getFirmwareVersion());

                com.optio3.cloud.client.waypoint.api.ProvisionApi proxy = createProxy(com.optio3.cloud.client.waypoint.api.ProvisionApi.class);
                proxy.performCheckin(m_report);
                return;
            }
            catch (Throwable t)
            {
                LoggerInstance.error("Failed to send report, due to %s", t);
            }

            Executors.safeSleep(2000);
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

        m_app.addArgumentToCommand(subparser, "--stationNumber", "ID of programmer station", true, (value) ->
        {
            m_report.stationNumber = value;
        });

        m_app.addArgumentToCommand(subparser, "--stationProgram", "ID of programmer program", true, (value) ->
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
