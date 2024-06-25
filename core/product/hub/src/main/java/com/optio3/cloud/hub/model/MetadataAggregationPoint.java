/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model;

import java.util.Collection;

public class MetadataAggregationPoint
{
    public String pointId;
    public String pointName;
    public String pointNameRaw;
    public String pointNameBackup;
    public String identifier;
    public String pointClassId;
    public String buildingId;
    public String locationSysId;
    public String equipmentId;

    public Collection<String> tags;
}