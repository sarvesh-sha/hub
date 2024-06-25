/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.bacnet;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.sys.BComponent;
import com.optio3.product.importers.niagara.baja.sys.BTypeSpec;

@ModuleTypeAnnotation(module = "bacnet", type = "BacnetListOf")
public class BBacnetListOf extends BComponent
{
    public BTypeSpec listTypeSpec;
}

