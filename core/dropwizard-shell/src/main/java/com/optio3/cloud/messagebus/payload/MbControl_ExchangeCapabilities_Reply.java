/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("MbControlExchangeCapabilitiesReply") // No underscore in model name, due to Swagger issues.
public class MbControl_ExchangeCapabilities_Reply extends MbControl_Reply
{
    public Set<String> available;
}
