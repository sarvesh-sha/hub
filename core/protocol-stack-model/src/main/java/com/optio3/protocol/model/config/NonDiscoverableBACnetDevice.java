/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.config;

import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

public class NonDiscoverableBACnetDevice
{
    public int    networkNumber;
    public int    instanceNumber;
    public int    mstpAddress;
    public String networkAddress;
    public int    networkPort;
    public String notes;

    //--//

    @Override
    public boolean equals(Object o)
    {
        NonDiscoverableBACnetDevice that = Reflection.as(o, NonDiscoverableBACnetDevice.class);
        if (that == null)
        {
            return false;
        }

        boolean equal = true;
        equal &= (networkNumber == that.networkNumber);
        equal &= (instanceNumber == that.instanceNumber);
        equal &= (mstpAddress == that.mstpAddress);
        equal &= (StringUtils.equals(networkAddress, that.networkAddress));
        equal &= (networkPort == that.networkPort);

        return equal;
    }

    @Override
    public int hashCode()
    {
        return instanceNumber;
    }
}
