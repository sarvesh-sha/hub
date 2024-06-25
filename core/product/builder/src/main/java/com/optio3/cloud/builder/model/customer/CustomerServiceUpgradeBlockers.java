/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.customer;

import java.time.ZonedDateTime;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.builder.persistence.config.UserRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class CustomerServiceUpgradeBlockers
{
    public final List<CustomerServiceUpgradeBlocker> requests = Lists.newArrayList();

    //--//

    public void add(UserRecord rec,
                    ZonedDateTime until)
    {
        CustomerServiceUpgradeBlocker blocker = CollectionUtils.findFirst(requests, (req) -> StringUtils.equals(req.user.sysId, rec.getSysId()));
        if (blocker == null)
        {
            blocker = new CustomerServiceUpgradeBlocker();
            blocker.user = TypedRecordIdentity.newTypedInstance(rec);
            requests.add(blocker);
        }

        blocker.until = until;
    }

    public void remove(UserRecord rec)
    {
        requests.removeIf((req) -> StringUtils.equals(req.user.sysId, rec.getSysId()));
    }
}
