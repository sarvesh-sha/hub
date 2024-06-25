/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import java.util.List;

import com.optio3.cloud.model.SortCriteria;

public class ReportFilterRequest
{
    public List<String> definitionIds;

    public List<SortCriteria> sortBy;
}
