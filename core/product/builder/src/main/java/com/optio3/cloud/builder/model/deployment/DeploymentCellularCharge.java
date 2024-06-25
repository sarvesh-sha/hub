/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.time.ZonedDateTime;

import com.optio3.util.TimeUtils;

public class DeploymentCellularCharge
{
    public ZonedDateTime timestamp;
    public long          upload;
    public long          download;
    public long          total;
    public double        billed;
    public double        fees;
    public double        feesOverage;

    //--//

    public static DeploymentCellularCharge accumulate(DeploymentCellularCharges data,
                                                      ZonedDateTime start,
                                                      ZonedDateTime end,
                                                      int days)
    {
        DeploymentCellularCharge res = null;

        for (DeploymentCellularCharge entry : data.charges)
        {
            if ((start == null || TimeUtils.compare(start, entry.timestamp) <= 0) && (end == null || TimeUtils.compare(entry.timestamp, end) < 0))
            {
                if (res == null)
                {
                    res = new DeploymentCellularCharge();
                }
                res.download += entry.download;
                res.upload += entry.upload;
                res.total += entry.total;
                res.billed += entry.billed;
            }
        }

        if (res != null)
        {
            res.fees = data.monthlyFees * days / 30;

            if (data.monthlyQuotaIncludedInFees > 0)
            {
                long overage = res.total - data.monthlyQuotaIncludedInFees;
                res.feesOverage = overage * data.extraCostPerMB * days / (1024 * 1024 * 30);
            }
        }

        return res;
    }

    public void accumulate(DeploymentCellularCharge other)
    {
        if (other != null)
        {
            download += other.download;
            upload += other.upload;
            total += other.total;
            billed += other.billed;
            fees += other.fees;
            feesOverage += other.feesOverage;
        }
    }
}
