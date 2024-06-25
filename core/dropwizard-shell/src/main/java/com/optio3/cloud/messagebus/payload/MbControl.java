/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.messagebus.MessageBusPayload;

@JsonSubTypes({ @JsonSubTypes.Type(value = MbControl_ExchangeCapabilities.class),
                @JsonSubTypes.Type(value = MbControl_GetIdentity.class),
                @JsonSubTypes.Type(value = MbControl_JoinChannel.class),
                @JsonSubTypes.Type(value = MbControl_KeepAlive.class),
                @JsonSubTypes.Type(value = MbControl_LeaveChannel.class),
                @JsonSubTypes.Type(value = MbControl_ListChannels.class),
                @JsonSubTypes.Type(value = MbControl_ListMembers.class),
                @JsonSubTypes.Type(value = MbControl_ListSubscriptions.class),
                @JsonSubTypes.Type(value = MbControl_UpgradeToUDP.class) })
public abstract class MbControl extends MessageBusPayload
{
}
