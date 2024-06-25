/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.config;

import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

public class SkippedBACnetDevice
{
    public int    networkNumber;
    public int    instanceNumber;
    public String transportAddress;
    public int    transportPort;
    public String notes;

    //--//

    @Override
    public boolean equals(Object o)
    {
        SkippedBACnetDevice that = Reflection.as(o, SkippedBACnetDevice.class);
        if (that == null)
        {
            return false;
        }

        return networkNumber == that.networkNumber && instanceNumber == that.instanceNumber && StringUtils.equals(transportAddress, that.transportAddress) && transportPort == that.transportPort;
    }

    @Override
    public int hashCode()
    {
        return instanceNumber;
    }
}
