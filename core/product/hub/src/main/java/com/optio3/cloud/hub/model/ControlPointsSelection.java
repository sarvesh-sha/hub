/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model;

import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class ControlPointsSelection
{
    public TypedRecordIdentityList<DeviceElementRecord> identities = new TypedRecordIdentityList<>();
}
