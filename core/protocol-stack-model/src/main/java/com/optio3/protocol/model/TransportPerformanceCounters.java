/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optio3.serialization.ObjectMappers;

public class TransportPerformanceCounters extends BaseObjectModel
{
    @FieldModelDescription(description = "Wait For Transport Access", units = EngineeringUnits.milliseconds)
    public int waitForTransportAccess;

    @FieldModelDescription(description = "Request Count", units = EngineeringUnits.counts)
    public int requestCount;

    @FieldModelDescription(description = "Request Roundtrip", units = EngineeringUnits.milliseconds)
    public int requestRoundtrip;

    @FieldModelDescription(description = "Packet Sent", units = EngineeringUnits.counts)
    public int packetTx;

    @FieldModelDescription(description = "Packet Sent Bytes", units = EngineeringUnits.bytes)
    public long packetTxBytes;

    @FieldModelDescription(description = "Packet Sent Timeouts", units = EngineeringUnits.counts)
    public int packetTxTimeouts;

    @FieldModelDescription(description = "Packet Received", units = EngineeringUnits.counts)
    public int packetRx;

    @FieldModelDescription(description = "Packet Received Bytes", units = EngineeringUnits.bytes)
    public long packetRxBytes;

    //--//

    public TransportPerformanceCounters copy()
    {
        TransportPerformanceCounters copy = new TransportPerformanceCounters();
        copy.accumulate(this);
        return copy;
    }

    public void accumulate(TransportPerformanceCounters other)
    {
        waitForTransportAccess += other.waitForTransportAccess;

        requestCount += other.requestCount;
        requestRoundtrip += other.requestRoundtrip;

        packetTx += other.packetTx;
        packetTxBytes += other.packetTxBytes;
        packetTxTimeouts += other.packetTxTimeouts;

        packetRx += other.packetRx;
        packetRxBytes += other.packetRxBytes;
    }

    public boolean hasNoValues()
    {
        boolean empty = true;

        empty &= waitForTransportAccess == 0;

        empty &= requestCount == 0;
        empty &= requestRoundtrip == 0;

        empty &= packetTx == 0;
        empty &= packetTxBytes == 0;
        empty &= packetTxTimeouts == 0;

        empty &= packetRx == 0;
        empty &= packetRxBytes == 0;

        return empty;
    }

    public static TransportPerformanceCounters difference(TransportPerformanceCounters before,
                                                          TransportPerformanceCounters after)
    {
        TransportPerformanceCounters diff = new TransportPerformanceCounters();

        diff.waitForTransportAccess = after.waitForTransportAccess - before.waitForTransportAccess;

        diff.requestCount     = after.requestCount - before.requestCount;
        diff.requestRoundtrip = after.requestRoundtrip - before.requestRoundtrip;

        diff.packetTx         = after.packetTx - before.packetTx;
        diff.packetTxBytes    = after.packetTxBytes - before.packetTxBytes;
        diff.packetTxTimeouts = after.packetTxTimeouts - before.packetTxTimeouts;

        diff.packetRx      = after.packetRx - before.packetRx;
        diff.packetRxBytes = after.packetRxBytes - before.packetRxBytes;

        return diff;
    }

    @Override
    public String toString()
    {
        if (waitForTransportAccess == 0)
        {
            if (packetTxTimeouts == 0)
            {
                return String.format("%d requests in %,dmsec (TX: %d pkts/%,d bytes, RX: %d pkts/%,d bytes)", requestCount, requestRoundtrip, packetTx, packetTxBytes, packetRx, packetRxBytes);
            }

            return String.format("%d requests in %,dmsec (TX: %d pkts/%,d bytes/%d timeouts, RX: %d pkts/%,d bytes)",
                                 requestCount,
                                 requestRoundtrip,
                                 packetTx,
                                 packetTxBytes,
                                 packetTxTimeouts,
                                 packetRx,
                                 packetRxBytes);
        }

        if (packetTxTimeouts == 0)
        {
            return String.format("%d requests in %,dmsec (%,dmsec wait for transport, TX: %d pkts/%,d bytes, RX: %d pkts/%,d bytes)",
                                 requestCount,
                                 requestRoundtrip,
                                 waitForTransportAccess,
                                 packetTx,
                                 packetTxBytes,
                                 packetRx,
                                 packetRxBytes);
        }

        return String.format("%d requests in %,dmsec (%,dmsec wait for transport, TX: %d pkts/%,d bytes/%d timeouts, RX: %d pkts/%,d bytes)",
                             requestCount,
                             requestRoundtrip,
                             waitForTransportAccess,
                             packetTx,
                             packetTxBytes,
                             packetTxTimeouts,
                             packetRx,
                             packetRxBytes);
    }

    //--//

    public static TransportPerformanceCounters deserializeFromJson(String json) throws
                                                                                IOException
    {
        return deserializeInner(getObjectMapper(), TransportPerformanceCounters.class, json);
    }

    @Override
    public ObjectMapper getObjectMapperForInstance()
    {
        return getObjectMapper();
    }

    public static ObjectMapper getObjectMapper()
    {
        return ObjectMappers.SkipDefaults;
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        // Not classified.
    }
}
