/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.normalization;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3DontMap;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.model.BaseModel;
import com.optio3.metadata.normalization.ImportExportData;

public class ImportedMetadata extends BaseModel
{
    @Optio3MapAsReadOnly
    public int version;

    @Optio3MapAsReadOnly
    public boolean active;

    @Optio3DontMap
    public List<ImportExportData> metadata = Lists.newArrayList();
}
