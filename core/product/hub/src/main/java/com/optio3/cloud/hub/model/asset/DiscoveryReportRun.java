/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class DiscoveryReportRun
{
    public TypedRecordIdentityList<NetworkAssetRecord> networks;

    public DeviceElementFilterRequest filter;
}
