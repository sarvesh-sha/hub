/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.communication;

import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentInstance;

public class DeviceDetails
{
    public DeploymentInstance instanceType;
    public String             hostId;

    public String imei;
    public String imsi;
    public String iccid;

    public String productId;
    public String hardwareRevision;
    public String firmwareVersion;
}