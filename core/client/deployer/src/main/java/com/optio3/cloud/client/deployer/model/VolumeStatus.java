/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

import java.util.HashMap;
import java.util.Map;

public class VolumeStatus
{
    public String              name;
    public Map<String, String> labels = new HashMap<String, String>();

    public String driver;

    public String mountpoint;
}
