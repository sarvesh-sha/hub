/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara;

import java.util.Objects;

import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

public class ParsedType
{
    public final String module;
    public final String type;

    public ParsedType(String module,
                      String type)
    {
        this.module = module;
        this.type = type;
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        ParsedType that = Reflection.as(o, ParsedType.class);
        if (that == null)
        {
            return false;
        }

        return StringUtils.equals(module, that.module) && StringUtils.equals(type, that.type);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(module, type);
    }

    @Override
    public String toString()
    {
        return module + ':' + type;
    }
}
