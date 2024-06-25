/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.baja.sys;

import java.util.Map;

import com.google.common.collect.Maps;
import com.optio3.product.importers.niagara.EncodedString;
import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.Parser;
import org.apache.commons.lang3.StringUtils;

@ModuleTypeAnnotation(module = "baja", type = "Ord")
public class BOrd extends BSimple
{
    public Map<String, EncodedString> parts = Maps.newHashMap();

    @Override
    public void configureAfterParsing(Parser parser)
    {
        if (value != null && StringUtils.isNotEmpty(value.rawValue))
        {
            for (String part : StringUtils.split(value.rawValue, '|'))
            {
                String partKey;
                String partValue;

                int pos = part.indexOf(':');
                if (pos < 0)
                {
                    partKey = part;
                    partValue = null;
                }
                else
                {
                    partKey = part.substring(0, pos);
                    partValue = part.substring(pos + 1);
                }

                parts.put(new EncodedString(partKey).getDecodedValue(), new EncodedString(partValue));
            }
        }
    }
}

