/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.channel;

import com.optio3.cloud.messagebus.transport.StableIdentity;

public abstract class RpcOrigin
{
    public abstract String getRpcId();

    public abstract StableIdentity getIdentity(String id);

    public abstract void setContext(String sysId,
                                    String instanceId);

    public abstract String getContextRecordId();

    public abstract String getContextInstanceId();
}
