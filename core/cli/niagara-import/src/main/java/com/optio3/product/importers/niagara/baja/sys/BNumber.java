/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.baja.sys;

import com.optio3.product.importers.niagara.Parser;
import org.apache.commons.lang3.StringUtils;

public abstract class BNumber extends BSimple
{
    @Override
    public void configureAfterParsing(Parser parser)
    {
        if (value != null && StringUtils.isNotEmpty(value.rawValue))
        {
            parse(value.getDecodedValue());
        }
    }

    public abstract int getInt();

    public abstract long getLong();

    public abstract float getFloat();

    public abstract double getDouble();

    protected abstract void parse(String value);
}

