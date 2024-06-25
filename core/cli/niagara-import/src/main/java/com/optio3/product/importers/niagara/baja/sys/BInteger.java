/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.baja.sys;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;

@ModuleTypeAnnotation(module = "baja", type = "Integer")
public class BInteger extends BNumber
{
    public int numericValue;

    @Override
    public int getInt()
    {
        return numericValue;
    }

    @Override
    public long getLong()
    {
        return numericValue;
    }

    @Override
    public float getFloat()
    {
        return numericValue;
    }

    @Override
    public double getDouble()
    {
        return numericValue;
    }

    @Override
    protected void parse(String value)
    {
        switch (value)
        {
            case "min":
                numericValue = Integer.MIN_VALUE;
                break;

            case "max":
                numericValue = Integer.MAX_VALUE;
                break;

            default:
                numericValue = Integer.parseInt(value);
                break;
        }
    }
}

