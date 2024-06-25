/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard.panes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeName("PaneFieldPathMap")
public class PaneFieldPathMap extends PaneField
{
    public TypedRecordIdentity<DeviceRecord> value;
}
