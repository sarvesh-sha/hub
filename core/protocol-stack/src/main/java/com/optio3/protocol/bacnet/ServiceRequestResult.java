/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet;

import com.optio3.protocol.model.TransportPerformanceCounters;

public class ServiceRequestResult<U> extends TransportPerformanceCounters
{
    public U              value;
    public Exception      failure;
    public DeviceIdentity failureTarget;
    public Class<?>       failureContext;
}
