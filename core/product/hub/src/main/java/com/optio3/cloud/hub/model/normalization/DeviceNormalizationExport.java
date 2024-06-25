/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.normalization;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.metadata.normalization.BACnetImportExportData;

public class DeviceNormalizationExport
{
    public BACnetImportExportData       deviceData;
    public List<BACnetImportExportData> objects = Lists.newArrayList();
}
