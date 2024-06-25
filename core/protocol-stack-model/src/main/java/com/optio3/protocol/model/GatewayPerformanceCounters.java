/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.MonotonousTime;

public class GatewayPerformanceCounters extends BaseObjectModel
{
    @FieldModelDescription(description = "Memory Free", units = EngineeringUnits.bytes)
    public long freeMemory;

    @FieldModelDescription(description = "Memory Total", units = EngineeringUnits.bytes)
    public long totalMemory;

    @FieldModelDescription(description = "Memory In Use", units = EngineeringUnits.bytes)
    public long inUseMemory;

    @FieldModelDescription(description = "Threads", units = EngineeringUnits.counts)
    public int threads;

    @FieldModelDescription(description = "CPU User", units = EngineeringUnits.percent)
    public double cpuUsageUser = Double.NaN;

    @FieldModelDescription(description = "CPU System", units = EngineeringUnits.percent)
    public double cpuUsageSystem = Double.NaN;

    @FieldModelDescription(description = "CPU Temperature", units = EngineeringUnits.degrees_celsius)
    public double cpuTemperature = Double.NaN;

    @FieldModelDescription(description = "Input Voltage", units = EngineeringUnits.volts)
    public double inputVoltage = Double.NaN;

    @FieldModelDescription(description = "Entities Uploaded", units = EngineeringUnits.counts)
    public int entitiesUploaded;

    @FieldModelDescription(description = "Entities Uploaded Retries", units = EngineeringUnits.counts)
    public int entitiesUploadedRetries;

    @FieldModelDescription(description = "Queue Length", units = EngineeringUnits.counts)
    public int pendingQueueLength;

    @FieldModelDescription(description = "Number Of RPC Connections", units = EngineeringUnits.counts)
    public int numberOfConnections;

    @FieldModelDescription(description = "TX Packets", units = EngineeringUnits.counts)
    public int packetTx;

    @FieldModelDescription(description = "TX Bytes", units = EngineeringUnits.bytes)
    public long packetTxBytes;

    @FieldModelDescription(description = "TX UDP Bytes", units = EngineeringUnits.bytes)
    public long packetTxBytesUDP;

    @FieldModelDescription(description = "RX Packets", units = EngineeringUnits.counts)
    public int packetRx;

    @FieldModelDescription(description = "RX Bytes", units = EngineeringUnits.bytes)
    public long packetRxBytes;

    @FieldModelDescription(description = "RX UDP Bytes", units = EngineeringUnits.bytes)
    public long packetRxBytesUDP;

    @FieldModelDescription(description = "MessageBus TX Packets", units = EngineeringUnits.counts)
    public int mbPacketTx;

    @FieldModelDescription(description = "MessageBus TX Bytes", units = EngineeringUnits.bytes)
    public long mbPacketTxBytes;

    @FieldModelDescription(description = "MessageBus TX Bytes (retransmission)", units = EngineeringUnits.bytes)
    public long mbPacketTxBytesResent;

    @FieldModelDescription(description = "MessageBus RX Packets", units = EngineeringUnits.counts)
    public int mbPacketRx;

    @FieldModelDescription(description = "MessageBus RX Bytes", units = EngineeringUnits.bytes)
    public long mbPacketRxBytes;

    @FieldModelDescription(description = "MessageBus RX Bytes (retransmission)", units = EngineeringUnits.bytes)
    public long mbPacketRxBytesResent;

    @JsonIgnore
    public MonotonousTime staleTimeout;

    @JsonIgnore
    public boolean shouldReport;

    //--//

    public static GatewayPerformanceCounters deserializeFromJson(String json) throws
                                                                              IOException
    {
        return deserializeInner(getObjectMapper(), GatewayPerformanceCounters.class, json);
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
