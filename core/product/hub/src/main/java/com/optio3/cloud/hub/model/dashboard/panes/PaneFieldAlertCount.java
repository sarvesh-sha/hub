/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard.panes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeName("PaneFieldAlertCount")
public class PaneFieldAlertCount extends PaneField
{
    public TypedRecordIdentity<LocationRecord> value;

    public boolean onlyActive;
}
