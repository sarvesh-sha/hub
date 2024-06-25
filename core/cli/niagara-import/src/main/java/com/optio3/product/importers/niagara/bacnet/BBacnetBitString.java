/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.bacnet;

import com.optio3.product.importers.niagara.EncodedString;
import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.Parser;
import com.optio3.product.importers.niagara.baja.sys.BFacets;
import com.optio3.product.importers.niagara.baja.sys.BSimple;
import org.apache.commons.lang3.StringUtils;

@ModuleTypeAnnotation(module = "bacnet", type = "BacnetBitString")
public class BBacnetBitString extends BSimple
{
    public boolean[] bits;
    public BFacets   facets;

    @Override
    public void configureAfterParsing(Parser parser)
    {
        if (value != null && StringUtils.isNotEmpty(value.rawValue))
        {
            EncodedString bitsText;

            int pos = value.rawValue.indexOf(':');
            if (pos < 0)
            {
                bitsText = value;
            }
            else
            {
                bitsText = new EncodedString(value.rawValue.substring(0, pos));

                facets = new BFacets();
                facets.value = new EncodedString(value.rawValue.substring(pos + 1));
                facets.configureAfterParsing(parser);
            }

            String bitsDecoded = bitsText.getDecodedValue();

            bits = new boolean[bitsDecoded.length()];
            for (int i = 0; i < bitsDecoded.length(); i++)
            {
                bits[i] = bitsDecoded.charAt(i) == '1';
            }
        }
    }
}

