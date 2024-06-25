/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("GatewayPerfDescriptor")
public final class GatewayPerfDescriptor extends BaseAssetDescriptor
{
    public String transportAddress;

    //--//

    @Override
    public boolean equals(Object o)
    {
        GatewayPerfDescriptor that = Reflection.as(o, GatewayPerfDescriptor.class);
        if (that == null)
        {
            return false;
        }

        return compare(this, that) == 0;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(transportAddress);
    }

    @Override
    public int compareTo(BaseAssetDescriptor o)
    {
        GatewayPerfDescriptor other = Reflection.as(o, GatewayPerfDescriptor.class);
        if (other != null)
        {
            return compare(this, other);
        }

        return StringUtils.compareIgnoreCase(this.toString(), o.toString());
    }

    public static int compare(GatewayPerfDescriptor o1,
                              GatewayPerfDescriptor o2)
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

        return StringUtils.compare(o1.transportAddress, o2.transportAddress);
    }

    @Override
    public String toString()
    {
        return transportAddress == null ? "Global" : transportAddress;
    }
}
