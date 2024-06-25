/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.bacnet;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.sys.BComponent;
import com.optio3.product.importers.niagara.baja.sys.BTypeSpec;
import com.optio3.product.importers.niagara.baja.sys.BValue;

@ModuleTypeAnnotation(module = "bacnet", type = "BacnetArray")
public class BBacnetArray extends BComponent
{
    public BTypeSpec arrayTypeSpec;
    public BValue    size;
    public BValue    fixedSize;
}

