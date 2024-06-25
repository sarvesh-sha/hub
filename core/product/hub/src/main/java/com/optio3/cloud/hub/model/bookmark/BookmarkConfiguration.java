/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.bookmark;

import java.time.ZonedDateTime;

import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;
import com.optio3.cloud.hub.model.bookmark.enums.BookmarkType;

@Optio3IncludeInApiDefinitions
public class BookmarkConfiguration
{
    public ZonedDateTime createdOn;

    public String id;
    public String parentID;

    public String recordID;
    public String parentRecordID;

    public String name;
    public String description;

    public BookmarkType type;

    public String url;

    public ViewStateSerialized stateSerialized;
}

