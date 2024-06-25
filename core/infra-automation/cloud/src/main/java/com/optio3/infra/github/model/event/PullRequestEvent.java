/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.github.model.event;

import com.optio3.infra.github.model.PullRequest;
import com.optio3.infra.github.model.User;

public class PullRequestEvent extends CommonEvent
{
    public int number;

    public PullRequest pull_request;

    public User assignee;
}
