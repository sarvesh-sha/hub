/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.messagebus.MessageBusPayload;

@JsonSubTypes({ @JsonSubTypes.Type(value = MbControl_ExchangeCapabilities_Reply.class),
                @JsonSubTypes.Type(value = MbControl_GetIdentity_Reply.class),
                @JsonSubTypes.Type(value = MbControl_JoinChannel_Reply.class),
                @JsonSubTypes.Type(value = MbControl_KeepAlive_Reply.class),
                @JsonSubTypes.Type(value = MbControl_LeaveChannel_Reply.class),
                @JsonSubTypes.Type(value = MbControl_ListChannels_Reply.class),
                @JsonSubTypes.Type(value = MbControl_ListMembers_Reply.class),
                @JsonSubTypes.Type(value = MbControl_ListSubscriptions_Reply.class),
                @JsonSubTypes.Type(value = MbControl_UpgradeToUDP_Reply.class) })
@JsonTypeName("MbControlReply") // No underscore in model name, due to Swagger issues.
public abstract class MbControl_Reply extends MessageBusPayload
{
}
