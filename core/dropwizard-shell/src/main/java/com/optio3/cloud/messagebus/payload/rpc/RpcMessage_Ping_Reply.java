/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload.rpc;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("RpcMessagePingReply") // No underscore in model name, due to Swagger issues.
public class RpcMessage_Ping_Reply extends RpcMessageReply
{
}
