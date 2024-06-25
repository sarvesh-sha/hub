/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload.peering;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.messagebus.payload.MbData;

@JsonTypeName("ForwardMessage")
public class ForwardMessage extends Peering
{
    public MbData msg;
}
