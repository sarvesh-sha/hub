/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.communication;

import java.util.List;

import com.google.common.collect.Lists;

public class EmailMessage
{
    public boolean              systemGenerated;
    public EmailRecipient       from;
    public List<EmailRecipient> to = Lists.newArrayList();
    public String               subject;
    public String               text;
    public String               html;
}
