/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload.peering;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({ @Type(value = ExchangeBrokerIdentity.class),
                @Type(value = ExchangeBrokerIdentityReply.class),
                @Type(value = ListChannels.class),
                @Type(value = ListChannelsReply.class),
                @Type(value = ListMembers.class),
                @Type(value = ListMembersReply.class),
                @Type(value = ForwardMessage.class), })
public abstract class Peering
{
}
