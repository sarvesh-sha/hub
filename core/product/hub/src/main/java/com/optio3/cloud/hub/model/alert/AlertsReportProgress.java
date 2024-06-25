package com.optio3.cloud.hub.model.alert;

import com.optio3.cloud.model.scheduler.BaseBackgroundActivityProgress;

public class AlertsReportProgress extends BaseBackgroundActivityProgress
{
    public int     totalAlerts;
    public int     alertsProcessed;
    public boolean generatingFile;
}
