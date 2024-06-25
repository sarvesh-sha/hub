/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard.panes;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("PaneFieldString")
public class PaneFieldString extends PaneField
{
    public String value;
}
