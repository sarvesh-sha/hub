/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.github.model;

import java.time.OffsetDateTime;
import java.util.List;

public class Commit
{
    public String  id;
    public String  tree_id;
    public boolean distinct;

    public String         message;
    public OffsetDateTime timestamp;
    public String         url;

    public CommitAuthor author;
    public CommitAuthor committer;

    public List<String> added;
    public List<String> modified;
    public List<String> removed;
}
