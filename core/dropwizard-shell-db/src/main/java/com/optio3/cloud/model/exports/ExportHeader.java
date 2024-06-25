/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model.exports;

import java.util.List;

import com.google.common.collect.Lists;

public class ExportHeader
{
    public       String             sheetName;
    public final List<ExportColumn> columns = Lists.newArrayList();
}