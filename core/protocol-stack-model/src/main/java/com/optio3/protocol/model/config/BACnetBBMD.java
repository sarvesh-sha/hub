/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.config;

import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

public class BACnetBBMD
{
    public String networkAddress;
    public int    networkPort;
    public String notes;

    //--//

    @Override
    public boolean equals(Object o)
    {
        BACnetBBMD that = Reflection.as(o, BACnetBBMD.class);
        if (that == null)
        {
            return false;
        }

        return networkPort == that.networkPort && StringUtils.equals(networkAddress, that.networkAddress);
    }

    @Override
    public int hashCode()
    {
        return networkPort;
    }
}
