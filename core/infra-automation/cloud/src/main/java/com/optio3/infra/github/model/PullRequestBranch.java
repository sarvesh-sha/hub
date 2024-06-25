/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.github.model;

public class PullRequestBranch
{
    public String     label;
    public String     ref;
    public String     sha;
    public Repository repo;
    public User       user;
}
