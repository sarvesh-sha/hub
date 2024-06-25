/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.transport;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.messagebus.MessageBusPayloadCallback;
import com.optio3.cloud.messagebus.payload.MbData_Message;

public interface DataTransportWithReplies<TOutbound, TInbound> extends DataTransport<TOutbound>
{
    <T extends TInbound> CompletableFuture<T> dispatchWithReply(MbData_Message data,
                                                                Class<T> replyClass,
                                                                MessageBusPayloadCallback notifyPayloadSize,
                                                                int timeoutForReply,
                                                                TimeUnit timeoutUnit) throws
                                                                                      Exception;

    default <T extends TInbound> CompletableFuture<T> sendWithReply(MbData_Message data,
                                                                    TOutbound payload,
                                                                    Class<T> replyClass,
                                                                    MessageBusPayloadCallback notifyPayloadSize,
                                                                    int timeoutForReply,
                                                                    TimeUnit timeoutUnit) throws
                                                                                          Exception
    {
        data.assignNewId();
        data.convertPayload(payload);

        return dispatchWithReply(data, replyClass, notifyPayloadSize, timeoutForReply, timeoutUnit);
    }
}
