/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.persistence.asset.LogicalAssetRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeName("AlertEngineValueLogicalAsset")
public class AlertEngineValueLogicalAsset extends AlertEngineValueAsset<LogicalAssetRecord>
{
    public static AlertEngineValueLogicalAsset createTyped(TypedRecordIdentity<LogicalAssetRecord> record)
    {
        if (!TypedRecordIdentity.isValid(record))
        {
            return null;
        }

        AlertEngineValueLogicalAsset res = new AlertEngineValueLogicalAsset();
        res.record = record;
        return res;
    }
}
