/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.normalization;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.metadata.normalization.ImportExportData;

public class ImportedMetadataPayload
{
    public List<ImportExportData> entries = Lists.newArrayList();
}
