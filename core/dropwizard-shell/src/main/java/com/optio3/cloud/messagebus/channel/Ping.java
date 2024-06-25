/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.channel;

import java.util.concurrent.CompletableFuture;

import com.optio3.cloud.annotation.Optio3MessageBusChannel;
import com.optio3.cloud.messagebus.MessageBusChannelProvider;
import com.optio3.cloud.messagebus.payload.MbData_Message;
import com.optio3.service.IServiceProvider;

@Optio3MessageBusChannel(name = "SYS.PING")
public class Ping extends MessageBusChannelProvider<String, String>
{
    public Ping(IServiceProvider serviceProvider,
                String channelName)
    {
        super(channelName);
    }

    @Override
    protected CompletableFuture<Void> receivedMessage(MbData_Message data,
                                                      String obj) throws
                                                                  Exception
    {
        obj = String.format("Got: %s", obj);
        return replyToMessage(data, obj, null);
    }
}
