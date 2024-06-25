/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

public abstract class MessageBusChannelProvider<TRequest, TReply> extends MessageBusChannelHandler<TRequest, TReply>
{
    protected MessageBusChannelProvider(String channelName)
    {
        super(channelName);
    }
}
