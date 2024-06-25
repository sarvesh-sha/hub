/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload.db;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("DbMessageConfig") // No underscore in model name, due to Swagger issues.
public class DbMessage_Config extends DbMessage
{
    public String  table;
    public boolean active;
}
