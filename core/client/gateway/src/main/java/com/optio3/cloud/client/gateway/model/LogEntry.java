/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model;

import java.time.ZonedDateTime;

import com.optio3.logging.Severity;

public class LogEntry
{
    public ZonedDateTime timestamp;
    public String        thread;
    public String        selector;
    public Severity      level;
    public String        line;
}
