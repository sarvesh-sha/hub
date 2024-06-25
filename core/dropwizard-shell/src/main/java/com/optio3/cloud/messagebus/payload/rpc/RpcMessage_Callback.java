/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload.rpc;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.remoting.RemoteCallDescriptor;

@JsonTypeName("RpcMessageCallback") // No underscore in model name, due to Swagger issues.
public class RpcMessage_Callback extends RpcMessage
{
    public String               callId;
    public String               callbackId;
    public RemoteCallDescriptor descriptor;
}
