/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.github.model.event;

import com.optio3.infra.github.model.Issue;
import com.optio3.infra.github.model.IssueComment;

public class IssueCommentEvent extends CommonEvent
{
    public Issue        issue;
    public IssueComment comment;
}
