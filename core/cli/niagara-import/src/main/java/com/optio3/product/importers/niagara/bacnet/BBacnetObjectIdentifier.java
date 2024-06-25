/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.bacnet;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.Parser;
import com.optio3.product.importers.niagara.baja.sys.BSimple;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import org.apache.commons.lang3.StringUtils;

@ModuleTypeAnnotation(module = "bacnet", type = "BacnetObjectIdentifier")
public class BBacnetObjectIdentifier extends BSimple
{
    public BACnetObjectIdentifier id;

    @Override
    public void configureAfterParsing(Parser parser)
    {
        if (value != null && StringUtils.isNotEmpty(value.rawValue))
        {
            StringBuilder sb           = new StringBuilder();
            String        valueDecoded = value.getDecodedValue();

            final String proprietary = "proprietary";
            if (valueDecoded.startsWith(proprietary))
            {
                valueDecoded = valueDecoded.substring(proprietary.length());
            }

            for (int i = 0; i < valueDecoded.length(); i++)
            {
                char c = valueDecoded.charAt(i);

                if (Character.isUpperCase(c))
                {
                    sb.append('_');
                    c = Character.toLowerCase(c);
                }
                else if (c == ':')
                {
                    c = '/';
                }

                sb.append(c);
            }

            id = new BACnetObjectIdentifier(sb.toString());
        }
    }
}

