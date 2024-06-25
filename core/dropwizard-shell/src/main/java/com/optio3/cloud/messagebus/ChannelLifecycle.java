/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

import com.optio3.cloud.messagebus.transport.SystemTransport;

public interface ChannelLifecycle
{
    void onJoin(SystemTransport transport);

    void onLeave(SystemTransport transport);
}
