/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.bacnet;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.sys.BNumber;

@ModuleTypeAnnotation(module = "bacnet", type = "BacnetUnsigned")
public class BBacnetUnsigned extends BNumber
{
    public long numericValue;

    @Override
    public int getInt()
    {
        return (int) numericValue;
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
                numericValue = Long.MIN_VALUE;
                break;

            case "max":
                numericValue = Long.MAX_VALUE;
                break;

            default:
                numericValue = Long.parseLong(value);
                break;
        }
    }
}
