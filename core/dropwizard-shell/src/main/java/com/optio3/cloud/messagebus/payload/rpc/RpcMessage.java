/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload.rpc;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({ @Type(value = RpcMessage_Ping.class),
                @Type(value = RpcMessage_Ping_Reply.class),
                @Type(value = RpcMessage_Call.class),
                @Type(value = RpcMessage_Call_Reply.class),
                @Type(value = RpcMessage_Callback.class), })
public abstract class RpcMessage
{
}
