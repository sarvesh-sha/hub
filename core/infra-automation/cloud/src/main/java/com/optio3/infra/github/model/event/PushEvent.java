/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.github.model.event;

import java.util.List;

import com.optio3.infra.github.model.Commit;
import com.optio3.infra.github.model.CommitAuthor;

public class PushEvent extends CommonEvent
{
    public String ref;
    public String before;
    public String after;

    public boolean created;
    public boolean deleted;
    public boolean forced;
    public String  base_ref;
    public String  compare;

    public List<Commit> commits;
    public Commit       head_commit;

    public CommitAuthor pusher;
}
