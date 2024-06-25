/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model;

import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.LogicalAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class SearchResultSet
{
    public int                                 totalUsers;
    public TypedRecordIdentityList<UserRecord> users = new TypedRecordIdentityList<>();

    public int                                  totalAlerts;
    public TypedRecordIdentityList<AlertRecord> alerts = new TypedRecordIdentityList<>();

    public int                                   totalDevices;
    public TypedRecordIdentityList<DeviceRecord> devices = new TypedRecordIdentityList<>();

    public int                                     totalLocations;
    public TypedRecordIdentityList<LocationRecord> locations = new TypedRecordIdentityList<>();

    public int                                         totalNetworks;
    public TypedRecordIdentityList<NetworkAssetRecord> networks = new TypedRecordIdentityList<>();

    public int                                         totalGateways;
    public TypedRecordIdentityList<GatewayAssetRecord> gateways = new TypedRecordIdentityList<>();

    public int                                          totalDeviceElements;
    public TypedRecordIdentityList<DeviceElementRecord> deviceElements = new TypedRecordIdentityList<>();

    public int                                         totalLogicalGroups;
    public TypedRecordIdentityList<LogicalAssetRecord> logicalGroups = new TypedRecordIdentityList<>();
}
