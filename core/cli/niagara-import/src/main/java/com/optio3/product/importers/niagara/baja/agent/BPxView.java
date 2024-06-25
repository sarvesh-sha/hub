/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.baja.agent;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.sys.BComponent;
import com.optio3.product.importers.niagara.baja.sys.BOrd;
import com.optio3.product.importers.niagara.baja.sys.BTypeSpec;
import com.optio3.product.importers.niagara.baja.sys.BValue;

@ModuleTypeAnnotation(module = "baja", type = "PxView")
public class BPxView extends BComponent
{
    public BValue    icon; // BIcon
    public BValue    requiredPermissions; // BPermissions
    public BTypeSpec media;
    public BOrd      pxFile;
}
