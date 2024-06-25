/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.baja.sys;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;

@ModuleTypeAnnotation(module = "baja", type = "Double")
public class BDouble extends BNumber
{
    public double numericValue;

    @Override
    public int getInt()
    {
        return (int) numericValue;
    }

    @Override
    public long getLong()
    {
        return (long) numericValue;
    }

    @Override
    public float getFloat()
    {
        return (float) numericValue;
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
            case "+inf":
                numericValue = Double.POSITIVE_INFINITY;
                break;

            case "-inf":
                numericValue = Double.NEGATIVE_INFINITY;
                break;

            case "nan":
                numericValue = Double.NaN;
                break;

            default:
                numericValue = Double.parseDouble(value);
                break;
        }
    }
}

