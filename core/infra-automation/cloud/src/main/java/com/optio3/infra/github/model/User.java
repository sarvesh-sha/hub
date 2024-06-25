/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.github.model;

public class User
{
    public int    id;
    public String type;

    public String  name;
    public String  email;
    public String  login;
    public boolean site_admin;

    public String avatar_url;
    public String gravatar_id;

    public String url;
    public String html_url;
    public String followers_url;
    public String following_url;
    public String gists_url;
    public String starred_url;
    public String subscriptions_url;
    public String organizations_url;
    public String repos_url;
    public String events_url;
    public String received_events_url;
}
