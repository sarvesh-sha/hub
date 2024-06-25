/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.github.model;

import java.util.List;

public class TextMatch
{
    public String      object_url;
    public String      object_type;
    public String      property;
    public String      fragment;
    public List<Match> matches;
}
