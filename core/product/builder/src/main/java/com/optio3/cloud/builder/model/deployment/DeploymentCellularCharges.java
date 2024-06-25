/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.util.CollectionUtils;
import com.optio3.util.TimeUtils;

public class DeploymentCellularCharges
{
    public       ZonedDateTime                  lastRefresh;
    public final List<DeploymentCellularCharge> charges = Lists.newArrayList();

    public double monthlyFees;
    public long   monthlyQuotaIncludedInFees;
    public double extraCostPerMB;

    //--//

    public ZonedDateTime accessFirstTimestamp()
    {
        final DeploymentCellularCharge charge = CollectionUtils.firstElement(charges);
        return charge != null ? charge.timestamp : null;
    }

    public ZonedDateTime accessLastTimestamp()
    {
        final DeploymentCellularCharge charge = CollectionUtils.lastElement(charges);
        return charge != null ? charge.timestamp : null;
    }

    public void cleanup(ZonedDateTime oldestPeriodToKeep)
    {
        charges.sort((a, b) -> TimeUtils.compare(a.timestamp, b.timestamp));

        charges.removeIf((charge) -> charge.timestamp.isBefore(oldestPeriodToKeep));
    }

    public DeploymentCellularCharge ensureTimestamp(ZonedDateTime timestamp)
    {
        if (timestamp == null)
        {
            return null;
        }

        ZonedDateTime timestampTruncated = timestamp.truncatedTo(ChronoUnit.HOURS);

        DeploymentCellularCharge res = CollectionUtils.findFirst(charges, (charge) -> TimeUtils.sameTimestamp(charge.timestamp, timestampTruncated));
        if (res == null)
        {
            res           = new DeploymentCellularCharge();
            res.timestamp = timestampTruncated;
            charges.add(res);
        }

        return res;
    }
}
