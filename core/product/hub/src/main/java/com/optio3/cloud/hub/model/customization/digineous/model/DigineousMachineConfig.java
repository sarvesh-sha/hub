/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digineous.model;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3AutoTrim;
import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;

@Optio3IncludeInApiDefinitions
public class DigineousMachineConfig
{
    @Optio3AutoTrim()
    public String machineId;

    @Optio3AutoTrim()
    public String machineName;

    //--//

    public String machineTemplate;

    public final List<DigineousDeviceConfig> devices = Lists.newArrayList();
}
