/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.baja.sys;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.ParsedType;
import com.optio3.product.importers.niagara.Parser;
import org.apache.commons.lang3.StringUtils;

@ModuleTypeAnnotation(module = "baja", type = "TypeSpec")
public class BTypeSpec extends BSimple
{
    public ParsedType parsedValue;

    @Override
    public void configureAfterParsing(Parser parser)
    {
        if (value != null && StringUtils.isNotEmpty(value.rawValue))
        {
            parsedValue = parser.getParsedType(value.getDecodedValue());
        }
    }
}

