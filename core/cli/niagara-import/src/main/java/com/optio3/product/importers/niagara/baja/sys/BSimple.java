/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.baja.sys;

import com.optio3.product.importers.niagara.Parser;

public abstract class BSimple extends BComplex
{
    public abstract void configureAfterParsing(Parser parser);
}
