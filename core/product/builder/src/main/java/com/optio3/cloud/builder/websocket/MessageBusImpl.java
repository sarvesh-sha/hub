/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.websocket;

import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3WebSocketEndpoint;
import com.optio3.cloud.messagebus.MessageBusServerWebSocket;
import com.optio3.logging.Severity;

@Optio3WebSocketEndpoint(name = "Message Bus WebSocket Servlet", timeout = 30 * 60 * 1000, urlPatterns = { "/v1/message-bus" }) // For Optio3 Shell
@Optio3RequestLogLevel(Severity.Debug)
public class MessageBusImpl extends MessageBusServerWebSocket
{
}
