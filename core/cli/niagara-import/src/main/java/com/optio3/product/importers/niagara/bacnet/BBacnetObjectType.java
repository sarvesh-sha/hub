/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.bacnet;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.sys.BFrozenEnum;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import org.apache.commons.lang3.StringUtils;

@ModuleTypeAnnotation(module = "bacnet", type = "BacnetObjectType")
public class BBacnetObjectType extends BFrozenEnum
{
    public BACnetObjectTypeOrUnknown enumValue;

    @Override
    public int getOrdinal()
    {
        return (int) enumValue.asLongValue();
    }

    @Override
    protected void parse(String value)
    {
        if (!StringUtils.isEmpty(value))
        {
            enumValue = BACnetObjectTypeOrUnknown.parse(value);
        }
    }
}

