/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

@FunctionalInterface
public interface MessageBusPayloadCallback
{
    void accept(int length);
}
