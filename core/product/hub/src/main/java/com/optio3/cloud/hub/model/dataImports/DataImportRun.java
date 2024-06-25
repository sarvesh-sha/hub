/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dataImports;

import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class DataImportRun
{
    public String dataImportsId;

    public TypedRecordIdentityList<DeviceRecord> devices;
}
