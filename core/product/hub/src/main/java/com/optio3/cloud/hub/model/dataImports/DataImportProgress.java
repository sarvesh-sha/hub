/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dataImports;

import com.optio3.cloud.model.scheduler.BaseBackgroundActivityProgress;

public class DataImportProgress extends BaseBackgroundActivityProgress
{
    public int devicesToProcess;
    public int devicesProcessed;
    public int elementsProcessed;
    public int elementsModified;
}

