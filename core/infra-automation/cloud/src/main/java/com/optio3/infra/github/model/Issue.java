/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.github.model;

import java.time.ZonedDateTime;
import java.util.List;

public class Issue
{
    public static class PullRequestLinks
    {
        public String url;
        public String html_url;
        public String diff_url;
        public String patch_url;
    }

    public int         id;
    public int         number;
    public String      state;
    public boolean     locked;
    public String      title;
    public String      body;
    public User        user;
    public List<Label> labels;
    public User        assignee;
    public List<User>  assignees;

    public ZonedDateTime created_at;
    public ZonedDateTime updated_at;
    public ZonedDateTime closed_at;
    public User          closed_by;

    public String url;
    public String repository_url;
    public String labels_url;
    public String comments_url;
    public String events_url;
    public String html_url;

    public Milestone        milestone;
    public int              comments;
    public PullRequestLinks pull_request;
    public Repository       repository;
    public Reactions        reactions;

    // TextMatches is only populated from search results that request text matches
    // See: search.go and https://developer.github.com/v3/search/#text-match-metadata
    public List<TextMatch> text_matches;
}
