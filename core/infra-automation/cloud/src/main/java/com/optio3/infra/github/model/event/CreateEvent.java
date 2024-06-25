/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.github.model.event;

public class CreateEvent extends CommonEvent
{
    public String ref;
    public String ref_type;
    public String master_branch;
    public String description;
    public String pusher_type;
}
