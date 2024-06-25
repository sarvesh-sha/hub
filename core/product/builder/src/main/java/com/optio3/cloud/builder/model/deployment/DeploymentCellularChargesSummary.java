/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.util.TimeUtils;

public class DeploymentCellularChargesSummary
{
    public int                      count;
    public DeploymentCellularCharge last24Hours = new DeploymentCellularCharge();
    public DeploymentCellularCharge last7Days   = new DeploymentCellularCharge();
    public DeploymentCellularCharge last14Days  = new DeploymentCellularCharge();
    public DeploymentCellularCharge last21Days  = new DeploymentCellularCharge();
    public DeploymentCellularCharge last30Days  = new DeploymentCellularCharge();

    public List<DeploymentCellularChargePerHost> last24HoursPerHost = Lists.newArrayList();
    public List<DeploymentCellularChargePerHost> last7DaysPerHost   = Lists.newArrayList();
    public List<DeploymentCellularChargePerHost> last14DaysPerHost  = Lists.newArrayList();
    public List<DeploymentCellularChargePerHost> last21DaysPerHost  = Lists.newArrayList();
    public List<DeploymentCellularChargePerHost> last30DaysPerHost  = Lists.newArrayList();

    public void compute(Map<TypedRecordIdentity<DeploymentHostRecord>, DeploymentCellularCharges> map,
                        int maxTopHosts)
    {
        ZonedDateTime now = TimeUtils.now()
                                     .truncatedTo(ChronoUnit.HOURS);

        last24Hours.timestamp = now.minusHours(24);
        last7Days.timestamp   = now.minusDays(7);
        last14Days.timestamp  = now.minusDays(14);
        last21Days.timestamp  = now.minusDays(21);
        last30Days.timestamp  = now.minusDays(30);

        for (Map.Entry<TypedRecordIdentity<DeploymentHostRecord>, DeploymentCellularCharges> pair : map.entrySet())
        {
            TypedRecordIdentity<DeploymentHostRecord> ri      = pair.getKey();
            DeploymentCellularCharges                 charges = pair.getValue();

            count++;

            DeploymentCellularCharge last24HoursCharges = DeploymentCellularCharge.accumulate(charges, last24Hours.timestamp, null, 1);
            DeploymentCellularCharge last7DaysCharges   = DeploymentCellularCharge.accumulate(charges, last7Days.timestamp, null, 7);
            DeploymentCellularCharge last14DaysCharges  = DeploymentCellularCharge.accumulate(charges, last14Days.timestamp, null, 14);
            DeploymentCellularCharge last21DaysCharges  = DeploymentCellularCharge.accumulate(charges, last21Days.timestamp, null, 21);
            DeploymentCellularCharge last30DaysCharges  = DeploymentCellularCharge.accumulate(charges, last30Days.timestamp, null, 30);

            this.last24Hours.accumulate(last24HoursCharges);
            this.last7Days.accumulate(last7DaysCharges);
            this.last14Days.accumulate(last14DaysCharges);
            this.last21Days.accumulate(last21DaysCharges);
            this.last30Days.accumulate(last30DaysCharges);

            updateIfNotNull(this.last24HoursPerHost, ri, last24HoursCharges);
            updateIfNotNull(this.last7DaysPerHost, ri, last7DaysCharges);
            updateIfNotNull(this.last14DaysPerHost, ri, last14DaysCharges);
            updateIfNotNull(this.last21DaysPerHost, ri, last21DaysCharges);
            updateIfNotNull(this.last30DaysPerHost, ri, last30DaysCharges);
        }

        sortAndTrim(this.last24HoursPerHost, maxTopHosts);
        sortAndTrim(this.last7DaysPerHost, maxTopHosts);
        sortAndTrim(this.last14DaysPerHost, maxTopHosts);
        sortAndTrim(this.last21DaysPerHost, maxTopHosts);
        sortAndTrim(this.last30DaysPerHost, maxTopHosts);
    }

    private static void sortAndTrim(List<DeploymentCellularChargePerHost> lst,
                                    int maxTopHosts)
    {
        lst.sort(DeploymentCellularChargePerHost::compareTo);

        Iterator<DeploymentCellularChargePerHost> it = lst.iterator();
        while (it.hasNext())
        {
            it.next();

            if (--maxTopHosts < 0)
            {
                it.remove();
            }
        }
    }

    private static void updateIfNotNull(List<DeploymentCellularChargePerHost> lst,
                                        TypedRecordIdentity<DeploymentHostRecord> ri,
                                        DeploymentCellularCharge charges)
    {
        if (charges != null)
        {
            DeploymentCellularChargePerHost entry = new DeploymentCellularChargePerHost();
            entry.sysId   = ri.sysId;
            entry.charges = charges;
            lst.add(entry);
        }
    }
}
