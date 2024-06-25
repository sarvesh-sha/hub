/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.communication;

import java.time.ZonedDateTime;

public class CrashReport
{
    public ZonedDateTime timestamp;
    public String        site;
    public String        page;
    public String        user;
    public String        stack;
}
