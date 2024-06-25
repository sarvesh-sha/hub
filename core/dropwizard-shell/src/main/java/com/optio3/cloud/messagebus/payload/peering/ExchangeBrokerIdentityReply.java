/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload.peering;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("ExchangeBrokerIdentityReply")
public class ExchangeBrokerIdentityReply extends PeeringReply
{
    public String brokerIdentity;
}
