/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

public class DeviceElementReportProgress extends AssetReportProgress
{
    public int     totalDeviceElements;
    public int     deviceElementsProcessed;
    public boolean generatingFile;
}
