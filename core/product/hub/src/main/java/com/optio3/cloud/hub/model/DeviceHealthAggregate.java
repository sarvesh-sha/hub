/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model;

import com.optio3.cloud.hub.model.alert.AlertSeverity;
import com.optio3.cloud.hub.model.alert.AlertType;

public class DeviceHealthAggregate
{
    public AlertType     type;
    public AlertSeverity maxSeverity;
    public int           count;
}
