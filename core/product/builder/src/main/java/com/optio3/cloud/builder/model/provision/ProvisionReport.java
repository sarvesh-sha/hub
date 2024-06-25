/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.provision;

import java.time.ZonedDateTime;
import java.util.List;

import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;

public class ProvisionReport
{
    public ZonedDateTime timestamp;

    public String manufacturingLocation;
    public String stationNumber;
    public String stationProgram;

    public String boardHardwareVersion;
    public String boardFirmwareVersion;
    public String boardSerialNumber;

    public String modemModule;
    public String modemRevision;

    public String                  firmwareVersion;
    public DockerImageArchitecture architecture;
    public String                  hostId;
    public String                  imsi;
    public String                  imei;
    public String                  iccid;

    public List<ProvisionTest> tests;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setFailures(List<String> failures)
    {
    }
}
