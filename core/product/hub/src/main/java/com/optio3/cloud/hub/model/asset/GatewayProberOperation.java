
/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import java.time.ZonedDateTime;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.client.gateway.model.prober.ProberOperation;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.model.BaseModelWithHeartbeat;
import com.optio3.cloud.model.TypedRecordIdentity;

public class GatewayProberOperation extends BaseModelWithHeartbeat
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<GatewayAssetRecord> gateway;

    //--//

    public ProberOperation inputDetails;

    @Optio3MapAsReadOnly
    public ProberOperation.BaseResults outputDetails;

    //--//

    @Optio3MapAsReadOnly
    public ZonedDateTime lastOutput;

    @Optio3MapAsReadOnly
    public int lastOffset;

    //--//

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<BackgroundActivityRecord> currentActivity;
}
