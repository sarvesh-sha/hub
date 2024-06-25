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

@JsonTypeName("RestPerfDescriptor")
public final class RestPerfDescriptor extends BaseAssetDescriptor
{
    public String path;

    //--//

    @Override
    public boolean equals(Object o)
    {
        RestPerfDescriptor that = Reflection.as(o, RestPerfDescriptor.class);
        if (that == null)
        {
            return false;
        }

        return compare(this, that) == 0;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(path);
    }

    @Override
    public int compareTo(BaseAssetDescriptor o)
    {
        RestPerfDescriptor other = Reflection.as(o, RestPerfDescriptor.class);
        if (other != null)
        {
            return compare(this, other);
        }

        return StringUtils.compareIgnoreCase(this.toString(), o.toString());
    }

    public static int compare(RestPerfDescriptor o1,
                              RestPerfDescriptor o2)
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

        return StringUtils.compare(o1.path, o2.path);
    }

    @Override
    public String toString()
    {
        return path;
    }
}
