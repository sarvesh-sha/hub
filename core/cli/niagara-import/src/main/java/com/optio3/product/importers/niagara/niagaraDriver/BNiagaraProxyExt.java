/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.niagaraDriver;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.driver.BProxyExt;
import com.optio3.product.importers.niagara.baja.sys.BOrd;
import com.optio3.product.importers.niagara.baja.sys.BValue;

@ModuleTypeAnnotation(module = "niagaraDriver", type = "NiagaraProxyExt")
public class BNiagaraProxyExt extends BProxyExt
{
    public BOrd   pointId;
    public BValue subscriptionStatus;
}
