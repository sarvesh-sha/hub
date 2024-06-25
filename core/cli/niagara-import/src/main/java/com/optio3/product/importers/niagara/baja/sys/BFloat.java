/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.baja.sys;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;

@ModuleTypeAnnotation(module = "baja", type = "Float")
public class BFloat extends BNumber
{
    public float numericValue;

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
            case "+inf":
                numericValue = Float.POSITIVE_INFINITY;
                break;

            case "-inf":
                numericValue = Float.NEGATIVE_INFINITY;
                break;

            case "nan":
                numericValue = Float.NaN;
                break;

            default:
                numericValue = Float.parseFloat(value);
                break;
        }
    }
}

