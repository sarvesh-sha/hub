/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digineous.model;

import com.optio3.cloud.annotation.Optio3AutoTrim;
import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;

@Optio3IncludeInApiDefinitions
public class DigineousVibrationMonitorDetails
{
    public int id;
    public int plantId;

    @Optio3AutoTrim()
    public String label;

    @Optio3AutoTrim()
    public String deviceName;
}
