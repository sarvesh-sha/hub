/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.provision.imaging;

import java.io.FileOutputStream;
import java.io.OutputStream;

import com.optio3.util.BoxingUtils;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import org.apache.commons.io.IOUtils;

public class LabelerHelper
{
    enum ConfigVariable implements IConfigVariable
    {
        BoardSerialNumber("BOARD_SN"),
        FirmwareVersion("FIRMWARE_VER"),
        StationNumber("STATION_NUMBER"),
        StationProgram("STATION_PROGRAM"),
        HostId("HOST_ID"),
        IMSI("IMSI"),
        IMEI("IMEI"),
        ICCID("ICCID");

        private final String m_variable;

        ConfigVariable(String variable)
        {
            m_variable = variable;
        }

        public String getVariable()
        {
            return m_variable;
        }
    }

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_label  = s_configValidator.newTemplate(LabelerHelper.class, "labels/DT420t_label1.prn", "${", "}");

    private final com.optio3.cloud.client.builder.model.ProvisionReport m_report;

    public LabelerHelper(com.optio3.cloud.client.builder.model.ProvisionReport report)
    {
        m_report = report;
    }

    public void printZD420t(String driverFile) throws
                                               Exception
    {
        ConfigVariables<ConfigVariable> parameters = s_template_label.allocate();

        parameters.setValue(ConfigVariable.BoardSerialNumber, BoxingUtils.get(m_report.boardSerialNumber, "N/A"));
        parameters.setValue(ConfigVariable.FirmwareVersion, BoxingUtils.get(m_report.firmwareVersion, "N/A"));
        parameters.setValue(ConfigVariable.StationNumber, BoxingUtils.get(m_report.stationNumber, "N/A"));
        parameters.setValue(ConfigVariable.StationProgram, BoxingUtils.get(m_report.stationProgram, "N/A"));
        parameters.setValue(ConfigVariable.HostId, fixupHostId(m_report.hostId));
        parameters.setValue(ConfigVariable.IMSI, m_report.imsi);
        parameters.setValue(ConfigVariable.IMEI, m_report.imei);
        parameters.setValue(ConfigVariable.ICCID, m_report.iccid);

        try (OutputStream stream = new FileOutputStream(driverFile))
        {
            IOUtils.write(parameters.convert(), stream, (String) null);
        }
    }

    private String fixupHostId(String hostId)
    {
        int pos = hostId.indexOf('-');
        if (pos > 0)
        {
            return hostId.substring(pos + 1);
        }

        return hostId;
    }
}
