/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload.db;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.persistence.DbEvent;

@JsonTypeName("DbMessageEvent") // No underscore in model name, due to Swagger issues.
public class DbMessage_Event extends DbMessage
{
    public List<DbEvent> events = Lists.newArrayList();
}
