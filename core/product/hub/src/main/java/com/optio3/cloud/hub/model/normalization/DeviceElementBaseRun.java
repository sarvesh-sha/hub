/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.normalization;

import com.optio3.cloud.hub.logic.normalizations.NormalizationRules;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.normalization.NormalizationRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.RecordHelper;

public abstract class DeviceElementBaseRun
{
    public String rulesId;

    public NormalizationRules rules;

    public TypedRecordIdentityList<DeviceRecord> devices;

    //--//

    public boolean ensureRules(RecordHelper<NormalizationRecord> helper) throws
                                                                         Exception
    {
        if (rulesId != null)
        {
            NormalizationRecord rec;

            if ("active".equals(rulesId))
            {
                rec = NormalizationRecord.findActive(helper);
            }
            else
            {
                rec = helper.getOrNull(rulesId);
            }

            if (rec == null)
            {
                return false;
            }

            rules = rec.getRules();
        }

        return true;
    }
}
