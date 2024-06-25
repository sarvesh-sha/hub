/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.bacnet;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.sys.BInteger;
import com.optio3.product.importers.niagara.baja.sys.BStruct;

@ModuleTypeAnnotation(module = "bacnet", type = "BacnetAddress")
public final class BBacnetAddress extends BStruct
{
    public BInteger           addressType   = new BInteger();
    public BInteger           networkNumber = new BInteger();
    public BBacnetOctetString macAddress;
}
