/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.baja.sys;

import com.optio3.product.importers.niagara.Parser;
import org.apache.commons.lang3.StringUtils;

public abstract class BEnum extends BSimple
{
    @Override
    public void configureAfterParsing(Parser parser)
    {
        if (value != null && StringUtils.isNotEmpty(value.rawValue))
        {
            parse(value.getDecodedValue());
        }
    }

    public abstract boolean isActive();

    public abstract int getOrdinal();

    protected abstract void parse(String value);
}

