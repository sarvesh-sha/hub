/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.transport;

public interface Endpoint
{
    SystemTransport getTransport();

    void recordConnection();

    void recordIncomingMessage(int size);

    void recordOutgoingMessage(int size);

    void setContext(String sysId,
                    String instanceId);

    String getContextRecordId();

    String getContextInstanceId();

    StableIdentity ensureIdentity(String id);

    StableIdentity getIdentity();

    long getTimestampOfLastIncomingMessage();
}
