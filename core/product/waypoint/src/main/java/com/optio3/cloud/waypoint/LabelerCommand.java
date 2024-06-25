/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint;

import com.optio3.cloud.provision.imaging.LabelerHelper;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.logging.Logger;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class LabelerCommand extends Command
{
    public static final Logger LoggerInstance = new Logger(LabelerCommand.class);

    private final WaypointApplication                                   m_app;
    private final com.optio3.cloud.client.builder.model.ProvisionReport m_report = new com.optio3.cloud.client.builder.model.ProvisionReport();

    private String m_printZD420t;

    public LabelerCommand(WaypointApplication app)
    {
        super("labeler", "Prints labels for serial numbers");

        m_app = app;
    }

    @Override
    public void run(Bootstrap<?> bootstrap,
                    Namespace namespace) throws
                                         Exception
    {
        final FirmwareHelper firmwareHelper = FirmwareHelper.get();
        m_report.boardSerialNumber = firmwareHelper.getSerialNumber();
        m_report.boardHardwareVersion = String.format("0x%04X", firmwareHelper.getHardwareVersion());
        m_report.boardFirmwareVersion = String.format("0x%04X", firmwareHelper.getFirmwareVersion());

        if (m_printZD420t != null)
        {
            LabelerHelper helper = new LabelerHelper(m_report);
            helper.printZD420t(m_printZD420t);
        }
    }

    @Override
    public void configure(Subparser subparser)
    {
        m_app.addArgumentToCommand(subparser, "--enableLog", "Enable logging", false, WaypointConfiguration::enableLogLevel);

        m_app.addArgumentToCommand(subparser, "--printZD420t", "<Zebra ZD420t printer device>", false, (value) ->
        {
            m_printZD420t = value;
        });

        m_app.addArgumentToCommand(subparser, "--firmwareVersion", "Version of the firmware", false, (value) ->
        {
            m_report.firmwareVersion = value;
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
            m_report.iccid = value;
        });
    }
}
