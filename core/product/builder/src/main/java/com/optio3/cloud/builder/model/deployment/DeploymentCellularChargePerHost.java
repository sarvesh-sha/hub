/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import com.google.common.collect.Maps;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.formatting.TabularField;
import com.optio3.cloud.formatting.TabularReportAsCSV;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.SessionHolder;

public class DeploymentCellularChargePerHost implements Comparable<DeploymentCellularChargePerHost>
{
    static class RowForReport
    {
        @TabularField(order = 0, title = "HostId")
        public String col_hostId;

        @TabularField(order = 1, title = "HostName")
        public String col_hostName;

        @TabularField(order = 2, title = "Timestamp", format = "yyyy-MM-dd")
        public ZonedDateTime col_timestamp;

        @TabularField(order = 3, title = "Upload")
        public long col_upload;

        @TabularField(order = 4, title = "Download")
        public long col_download;

        @TabularField(order = 5, title = "Billed")
        public double col_billed;
    }

    //--//

    public String                   sysId;
    public DeploymentCellularCharge charges;

    @Override
    public int compareTo(DeploymentCellularChargePerHost o)
    {
        return Float.compare(o.charges.total, this.charges.total);
    }

    public static String report(SessionHolder sessionHolder,
                                Map<TypedRecordIdentity<DeploymentHostRecord>, DeploymentCellularCharges> map) throws
                                                                                                               IOException
    {
        DeploymentGlobalDescriptor.Settings settings = new DeploymentGlobalDescriptor.Settings();
        settings.loadDeployments = true;
        DeploymentGlobalDescriptor globalDescriptor = DeploymentGlobalDescriptor.get(sessionHolder, settings);

        TabularReportAsCSV<RowForReport> tr     = new TabularReportAsCSV<>(RowForReport.class);
        ZoneId                           zoneId = ZoneId.of("America/Los_Angeles");

        tr.emit(rowHandler ->
                {
                    for (TypedRecordIdentity<DeploymentHostRecord> ri : map.keySet())
                    {
                        DeploymentHost host = globalDescriptor.getHost(ri);
                        if (host != null)
                        {
                            DeploymentCellularCharges                    charges = map.get(ri);
                            Map<ZonedDateTime, DeploymentCellularCharge> perDay  = Maps.newHashMap();

                            for (DeploymentCellularCharge charge : charges.charges)
                            {
                                DeploymentCellularCharge rollup = perDay.computeIfAbsent(charge.timestamp.truncatedTo(ChronoUnit.DAYS)
                                                                                                         .withZoneSameInstant(zoneId), (key) ->
                                                                                         {
                                                                                             DeploymentCellularCharge entry = new DeploymentCellularCharge();
                                                                                             entry.timestamp = key;
                                                                                             return entry;
                                                                                         });

                                rollup.accumulate(charge);
                            }

                            for (Map.Entry<ZonedDateTime, DeploymentCellularCharge> pair : perDay.entrySet())
                            {
                                RowForReport row = new RowForReport();
                                row.col_hostId    = host.hostId;
                                row.col_hostName  = host.hostName;
                                row.col_timestamp = pair.getKey();

                                DeploymentCellularCharge chargesPerDate = pair.getValue();
                                row.col_upload   = chargesPerDate.upload;
                                row.col_download = chargesPerDate.download;
                                row.col_billed   = chargesPerDate.billed;

                                rowHandler.emitRow(row);
                            }
                        }
                    }
                });

        return String.join("\n", tr.lines);
    }
}
