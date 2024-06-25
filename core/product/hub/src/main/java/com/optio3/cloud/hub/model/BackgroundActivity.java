/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.annotation.Optio3MapToPersistence;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.scheduler.BaseBackgroundActivity;

public class BackgroundActivity extends BaseBackgroundActivity
{
    @Optio3MapAsReadOnly
    public HostAsset worker;

    @Optio3MapAsReadOnly
    @Optio3MapToPersistence("waitingActivitiesForModel")
    public TypedRecordIdentityList<BackgroundActivityRecord> waitingActivities;

    @Optio3MapAsReadOnly
    @Optio3MapToPersistence("subActivitiesForModel")
    public TypedRecordIdentityList<BackgroundActivityRecord> subActivities;
}
