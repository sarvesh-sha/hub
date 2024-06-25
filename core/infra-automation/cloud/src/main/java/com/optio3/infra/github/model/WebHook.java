/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.github.model;

import java.time.ZonedDateTime;
import java.util.List;

import com.google.common.collect.Lists;

public class WebHook
{
    public int    id;
    public String type;

    public ZonedDateTime updated_at;
    public ZonedDateTime created_at;

    //--//

    public String  name;
    public boolean active;

    public List<String> events = Lists.newArrayList();

    public WebHookConfiguration config = new WebHookConfiguration();

    public String url;
    public String test_url;
    public String ping_url;

    public WebHookResponse last_response;
}
