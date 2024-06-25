/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model;

import java.time.ZonedDateTime;

public class GatewayQueueStatus
{
    public ZonedDateTime oldestEntry;
    public int           numberOfUnbatchedEntries;
    public int           numberOfBatchedEntries;
    public int           numberOfBatches;
}
