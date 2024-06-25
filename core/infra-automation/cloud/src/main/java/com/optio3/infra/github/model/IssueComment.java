/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.github.model;

import java.time.ZonedDateTime;

public class IssueComment
{
    public int    id;
    public String url;
    public String html_url;
    public String issue_url;

    public ZonedDateTime created_at;
    public ZonedDateTime updated_at;

    public String body;
    public User   user;

    public Reactions reactions;
}
