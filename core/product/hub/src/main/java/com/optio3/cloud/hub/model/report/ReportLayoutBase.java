/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = ReportLayoutColumn.class), @JsonSubTypes.Type(value = ReportLayoutItem.class), @JsonSubTypes.Type(value = ReportLayoutRow.class) })
public abstract class ReportLayoutBase
{
    public List<ReportLayoutBase> children;
    public int                    widthRatio;
}
