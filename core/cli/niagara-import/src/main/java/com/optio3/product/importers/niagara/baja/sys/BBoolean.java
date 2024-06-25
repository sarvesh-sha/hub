/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.baja.sys;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;

@ModuleTypeAnnotation(module = "baja", type = "Boolean")
public class BBoolean extends BEnum
{
    public boolean numericValue;

    @Override
    public boolean isActive()
    {
        return numericValue;
    }

    @Override
    public int getOrdinal()
    {
        return numericValue ? 1 : 0;
    }

    @Override
    protected void parse(String value)
    {
        switch (value)
        {
            case "true":
                numericValue = true;
                break;

            case "false":
                numericValue = false;
                break;
        }
    }
}

