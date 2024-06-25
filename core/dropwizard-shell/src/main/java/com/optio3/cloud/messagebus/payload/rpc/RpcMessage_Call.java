/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload.rpc;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.remoting.RemoteCallDescriptor;

@JsonTypeName("RpcMessageCall") // No underscore in model name, due to Swagger issues.
public class RpcMessage_Call extends RpcMessage
{
    public String instanceId;
    public String callId;

    public RemoteCallDescriptor descriptor;

    public int      timeout;
    public TimeUnit timeoutUnit;
}
