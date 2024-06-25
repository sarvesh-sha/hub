/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("IpnDeviceDescriptor")
public final class IpnDeviceDescriptor extends BaseAssetDescriptor
{
    public String name;

    //--//

    @Override
    public boolean equals(Object o)
    {
        IpnDeviceDescriptor that = Reflection.as(o, IpnDeviceDescriptor.class);
        if (that == null)
        {
            return false;
        }

        return StringUtils.equals(name, that.name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(name);
    }

    @Override
    public int compareTo(BaseAssetDescriptor o)
    {
        IpnDeviceDescriptor other = Reflection.as(o, IpnDeviceDescriptor.class);
        if (other != null)
        {
            return compare(this, other);
        }

        return StringUtils.compareIgnoreCase(this.toString(), o.toString());
    }

    public static int compare(IpnDeviceDescriptor o1,
                              IpnDeviceDescriptor o2)
    {
        if (o1 == o2)
        {
            return 0;
        }

        if (o1 == null)
        {
            return 1;
        }

        if (o2 == null)
        {
            return -1;
        }

        return StringUtils.compare(o1.name, o2.name);
    }

    @Override
    public String toString()
    {
        return name;
    }
}
