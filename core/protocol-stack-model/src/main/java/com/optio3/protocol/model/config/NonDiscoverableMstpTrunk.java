/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.config;

import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

public class NonDiscoverableMstpTrunk
{
    public String networkAddress;
    public int    networkPort;
    public int    networkNumber;
    public String notes;

    //--//

    @Override
    public boolean equals(Object o)
    {
        NonDiscoverableMstpTrunk that = Reflection.as(o, NonDiscoverableMstpTrunk.class);
        if (that == null)
        {
            return false;
        }

        boolean equal = true;
        equal &= (StringUtils.equals(networkAddress, that.networkAddress));
        equal &= (networkPort == that.networkPort);
        equal &= (networkNumber == that.networkNumber);

        return equal;
    }

    @Override
    public int hashCode()
    {
        return networkNumber;
    }
}
