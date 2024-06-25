/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

import java.time.ZonedDateTime;

public class MessageBusDatagramSession
{
    public String        sessionId;
    public String        contextSysId;
    public String        displayName;
    public String        udpAddress;
    public ZonedDateTime lastPacket;
    public String        rpcId;

    public MessageBusStatistics statistics;
}
