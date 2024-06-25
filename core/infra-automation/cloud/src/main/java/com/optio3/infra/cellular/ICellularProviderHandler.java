/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.cellular;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Semaphore;

public interface ICellularProviderHandler
{
    enum Status
    {
        NEW("new"),
        READY("ready"),
        ACTIVE("active"),
        INACTIVE("inactive"),
        SUSPENDED("suspended"),
        DEACTIVATED("deactivated"),
        CANCELED("canceled"),
        SCHEDULED("scheduled"),
        UPDATING("updating");

        private final String value;

        Status(final String value)
        {
            this.value = value;
        }

        public String toString()
        {
            return value;
        }
    }

    class SimInfo
    {
        public String id;
        public String iccid;
        public String name;
        public Status status;
        public String plan;
    }

    class SimCharges
    {
        public ZonedDateTime timestamp;
        public long          upload;
        public long          download;
        public long          total;
        public double        billed;
    }

    public class SimConnectionStatus
    {
        public boolean isOnline;
        public boolean isTransferring;
    }

    class SimDataSession
    {
        public ZonedDateTime start;
        public ZonedDateTime end;
        public ZonedDateTime lastUpdated;
        public int           packetsDownloaded;
        public int           packetsUploaded;

        public String cellId;
        public String operator;
        public String operatorCountry;
        public String radioLink;
        public double estimatedLongitude;
        public double estimatedLatitude;
    }

    class SimDataExchange
    {
        public String ip;
        public int    daysAgo;
        public int    bytes;
    }

    SimInfo lookupSim(String iccid);

    void updateStatus(SimInfo si,
                      String productId);

    List<SimCharges> getCharges(String id,
                                ZonedDateTime start,
                                ZonedDateTime end);

    SimConnectionStatus getConnectionStatus(String id);

    List<SimDataSession> getDataSessions(String id,
                                         ZonedDateTime start,
                                         ZonedDateTime end);

    List<SimDataExchange> getDataExchanges(Semaphore rateLimiter,
                                           String id,
                                           int days);
}
