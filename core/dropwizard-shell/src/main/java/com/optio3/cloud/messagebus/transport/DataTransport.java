/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.transport;

import java.util.concurrent.CompletableFuture;

import com.optio3.cloud.messagebus.MessageBusPayloadCallback;
import com.optio3.cloud.messagebus.payload.MbData;

public interface DataTransport<TOutbound>
{
    void close();

    String getChannelName();

    String getEndpointId();

    //--//

    void setTransmitTransport(DataTransportWithReplies<?, ?> transport);

    //--//

    Endpoint getEndpointForDestination(String destination);

    CompletableFuture<Void> dispatchWithNoReply(MbData data,
                                                MessageBusPayloadCallback notifyPayloadSize) throws
                                                                                             Exception;

    default CompletableFuture<Void> sendWithNoReply(MbData data,
                                                    TOutbound payload,
                                                    MessageBusPayloadCallback notifyPayloadSize) throws
                                                                                                 Exception
    {
        data.convertPayload(payload);

        return dispatchWithNoReply(data, notifyPayloadSize);
    }
}
