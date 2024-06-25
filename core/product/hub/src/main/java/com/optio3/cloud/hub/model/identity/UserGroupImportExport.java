/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.identity;

import java.util.List;

import com.google.common.collect.Lists;

public class UserGroupImportExport
{
    public List<Role> roles = Lists.newArrayList();

    public List<UserGroup> groups = Lists.newArrayList();
}
