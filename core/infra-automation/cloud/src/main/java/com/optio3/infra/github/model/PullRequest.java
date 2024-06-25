/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.github.model;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class PullRequest
{
    public int     id;
    public int     number;
    public boolean locked;
    public String  state;
    public String  title;
    public String  body;

    public ZonedDateTime created_at;
    public ZonedDateTime updated_at;
    public ZonedDateTime closed_at;
    public ZonedDateTime merged_at;
    public String        merge_commit_sha;

    public User    user;
    public boolean merged;
    public boolean mergeable;
    public String  mergeable_state;
    public boolean rebaseable;
    public User    merged_by;
    public boolean maintainer_can_modify;

    public int comments;
    public int review_comments;
    public int commits;
    public int additions;
    public int deletions;
    public int changed_files;

    public String url;
    public String html_url;
    public String diff_url;
    public String patch_url;
    public String issue_url;
    public String commits_url;
    public String review_comments_url;
    public String review_comment_url;
    public String comments_url;
    public String statuses_url;

    public User              assignee;
    public List<User>        assignees;
    public List<User>        requested_reviewers;
    public Milestone         milestone;
    public PullRequestBranch head;
    public PullRequestBranch base;

    public Map<String, Link> _links;
}
