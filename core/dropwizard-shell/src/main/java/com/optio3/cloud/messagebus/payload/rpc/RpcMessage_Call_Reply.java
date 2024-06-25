/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload.rpc;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.remoting.RemoteResult;

@JsonTypeName("RpcMessageCallReply") // No underscore in model name, due to Swagger issues.
public class RpcMessage_Call_Reply extends RpcMessageReply
{
    public RemoteResult result;
}
