/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.bacnet;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.sys.BFrozenEnum;
import com.optio3.protocol.model.bacnet.enums.BACnetDeviceStatus;

@ModuleTypeAnnotation(module = "bacnet", type = "BacnetDeviceStatus")
public class BBacnetDeviceStatus extends BFrozenEnum
{
    public int                numericValue;
    public BACnetDeviceStatus enumValue;

    @Override
    public int getOrdinal()
    {
        if (enumValue != null)
        {
            return enumValue.encoding();
        }

        return numericValue;
    }

    @Override
    protected void parse(String value)
    {
        for (BACnetDeviceStatus v : BACnetDeviceStatus.values())
        {
            if (v.name()
                 .equals(value))
            {
                enumValue = v;
                return;
            }
        }

        try
        {
            numericValue = Integer.parseInt(value);
        }
        catch (Exception e)
        {
            // Ignore failures.
        }
    }
}

