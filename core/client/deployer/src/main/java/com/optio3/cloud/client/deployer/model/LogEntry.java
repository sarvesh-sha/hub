/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

import java.time.ZonedDateTime;

public class LogEntry
{
    public int           fd;
    public ZonedDateTime timestamp;
    public String        line;
}
