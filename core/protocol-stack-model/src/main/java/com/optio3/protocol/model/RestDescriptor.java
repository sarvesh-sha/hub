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

@JsonTypeName("RestDescriptor")
public final class RestDescriptor extends BaseAssetDescriptor
{
    public String sysId;

    //--//

    @Override
    public boolean equals(Object o)
    {
        RestDescriptor that = Reflection.as(o, RestDescriptor.class);
        if (that == null)
        {
            return false;
        }

        return StringUtils.equals(sysId, that.sysId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(sysId);
    }

    @Override
    public int compareTo(BaseAssetDescriptor o)
    {
        RestDescriptor other = Reflection.as(o, RestDescriptor.class);
        if (other != null)
        {
            return compare(this, other);
        }

        return StringUtils.compareIgnoreCase(this.toString(), o.toString());
    }

    public static int compare(RestDescriptor o1,
                              RestDescriptor o2)
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

        return StringUtils.compare(o1.sysId, o2.sysId);
    }

    @Override
    public String toString()
    {
        return sysId;
    }
}
