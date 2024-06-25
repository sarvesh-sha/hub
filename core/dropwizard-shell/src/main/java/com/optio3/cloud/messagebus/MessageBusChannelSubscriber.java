/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

import com.optio3.cloud.annotation.Optio3MessageBusChannel;
import com.optio3.util.Exceptions;

public abstract class MessageBusChannelSubscriber<TRequest, TReply> extends MessageBusChannelHandler<TRequest, TReply>
{
    protected MessageBusChannelSubscriber(String channelName)
    {
        super(channelName);
    }

    protected MessageBusChannelSubscriber(Class<? extends MessageBusChannelProvider<TRequest, TReply>> clzProvider)
    {
        super(extractChannelName(clzProvider));
    }

    private static <TRequest, TReply> String extractChannelName(Class<? extends MessageBusChannelProvider<TRequest, TReply>> clzProvider)
    {
        Optio3MessageBusChannel anno = clzProvider.getAnnotation(Optio3MessageBusChannel.class);
        if (anno == null)
        {
            throw Exceptions.newRuntimeException("Missing @Optio3MessageBusChannel on type '%s'", clzProvider);
        }

        return anno.name();
    }
}
