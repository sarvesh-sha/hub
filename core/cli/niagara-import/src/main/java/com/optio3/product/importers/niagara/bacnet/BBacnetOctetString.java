/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.bacnet;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.Parser;
import com.optio3.product.importers.niagara.baja.sys.BSimple;
import org.apache.commons.lang3.StringUtils;

@ModuleTypeAnnotation(module = "bacnet", type = "BacnetOctetString")
public final class BBacnetOctetString extends BSimple
{
    public byte[] arr;

    @Override
    public void configureAfterParsing(Parser parser)
    {
        if (value != null && StringUtils.isNotEmpty(value.rawValue))
        {
            String text = value.getDecodedValue();

            if (text.equals("null"))
            {
                arr = null;
            }
            else
            {
                String[] parts = StringUtils.split(text, ' ');
                arr = new byte[parts.length];

                for (int i = 0; i < parts.length; i++)
                {
                    arr[i] = (byte) Integer.parseInt(parts[i], 16);
                }
            }
        }
    }
}
