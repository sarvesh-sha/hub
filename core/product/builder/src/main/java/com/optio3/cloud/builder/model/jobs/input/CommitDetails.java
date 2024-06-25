/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs.input;

public class CommitDetails
{
    public String id;

    public String branch;

    public String message;

    public CommitPerson author;
    public CommitPerson committer;

    public String[] parents;
}
