/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.modbus;

import com.optio3.product.importers.niagara.baja.driver.BProxyExt;
import com.optio3.product.importers.niagara.baja.sys.BValue;

public abstract class BModbusProxyExt extends BProxyExt
{
    public BValue pollFrequency;
    public BValue dataAddress;
}