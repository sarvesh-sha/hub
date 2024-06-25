/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.baja.sys;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.Parser;

@ModuleTypeAnnotation(module = "baja", type = "String")
public class BString extends BSimple
{
    @Override
    public void configureAfterParsing(Parser parser)
    {
    }
}

