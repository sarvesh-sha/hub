/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.github.model;

import java.time.ZonedDateTime;

public class Milestone
{
    public int id;
    public int number;

    public String state;
    public String title;
    public String description;
    public User   creator;
    public int    open_issues;
    public int    closed_issues;

    public String url;
    public String html_url;
    public String labels_url;

    public ZonedDateTime created_at;
    public ZonedDateTime updated_at;
    public ZonedDateTime closed_at;
    public ZonedDateTime due_on;
}
