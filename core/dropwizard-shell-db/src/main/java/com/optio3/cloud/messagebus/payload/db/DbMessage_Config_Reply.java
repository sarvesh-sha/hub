/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload.db;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("DbMessageConfigReply") // No underscore in model name, due to Swagger issues.
public class DbMessage_Config_Reply extends DbMessageReply
{
    public boolean       success;
    public ZonedDateTime lastUpdate;
}
