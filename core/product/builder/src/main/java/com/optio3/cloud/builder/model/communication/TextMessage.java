/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.communication;

import java.util.List;

import com.google.common.collect.Lists;

public class TextMessage
{
    public boolean      systemGenerated;
    public String       senderId;
    public List<String> phoneNumbers = Lists.newArrayList();
    public String       text;
}
