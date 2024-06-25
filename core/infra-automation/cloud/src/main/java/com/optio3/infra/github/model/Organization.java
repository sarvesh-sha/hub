/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.github.model;

import java.time.ZonedDateTime;

public class Organization
{
    public int    id;
    public String type;

    public ZonedDateTime updated_at;
    public ZonedDateTime created_at;

    //--//

    public String login;
    public String description;
    public String name;
    public String company;
    public String blog;
    public String location;
    public String email;

    public String url;
    public String repos_url;
    public String events_url;
    public String hooks_url;
    public String issues_url;
    public String members_url;
    public String public_members_url;
    public String avatar_url;
    public String html_url;

    public boolean has_organization_projects;
    public boolean has_repository_projects;
    public boolean members_can_create_repositories;

    public int public_repos;
    public int public_gists;
    public int followers;
    public int following;

    public int  total_private_repos;
    public int  owned_private_repos;
    public int  private_gists;
    public long disk_usage;
    public int  collaborators;

    public String billing_email;
    public Plan   plan;

    public String default_repository_permission;
}
