/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import java.util.List;

import com.optio3.cloud.model.scheduler.BaseBackgroundActivityProgress;

public abstract class AssetReportProgress extends BaseBackgroundActivityProgress
{
    public List<String> report;
}
