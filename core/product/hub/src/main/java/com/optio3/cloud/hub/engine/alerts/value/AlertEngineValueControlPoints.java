/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;

@JsonTypeName("AlertEngineValueControlPoints")
public class AlertEngineValueControlPoints extends AlertEngineValueAbstractAssets<DeviceElementRecord>
{
}
