/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

public class AzureDigitalTwinSyncProgress extends AssetReportProgress
{
    public int networksToProcess;
    public int devicesProcessed;
    public int elementsProcessed;

    public int twinsFound;
    public int relationshipsFound;

    public int twinsProcessed;
    public int relationshipsProcessed;
}

