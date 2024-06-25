/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.github.model.event;

import com.optio3.infra.github.model.Changes;
import com.optio3.infra.github.model.Organization;
import com.optio3.infra.github.model.Repository;
import com.optio3.infra.github.model.User;

public abstract class CommonEvent
{
    public String  action;
    public Changes changes;

    public Repository   repository;
    public Organization organization;
    public User         sender;
}
