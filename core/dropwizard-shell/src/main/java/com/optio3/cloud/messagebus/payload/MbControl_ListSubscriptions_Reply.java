/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("MbControlListSubscriptionsReply") // No underscore in model name, due to Swagger issues.
public class MbControl_ListSubscriptions_Reply extends MbControl_Reply
{
    public List<String> subscribedChannels;
}
