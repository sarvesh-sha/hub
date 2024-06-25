/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.config;

import java.util.Objects;

import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

public class FilteredSubnet
{
    public String cidr;
    public String notes;

    //--//

    @Override
    public boolean equals(Object o)
    {
        FilteredSubnet that = Reflection.as(o, FilteredSubnet.class);
        if (that == null)
        {
            return false;
        }

        return StringUtils.equals(cidr, that.cidr);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(cidr);
    }
}
