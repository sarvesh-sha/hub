/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.baja.driver;

import com.optio3.product.importers.niagara.baja.sys.BFacets;
import com.optio3.product.importers.niagara.baja.sys.BValue;

public abstract class BProxyExt extends BAbstractProxyExt
{
    public BValue  status;
    public BValue  faultCause;
    public BValue  enabled;
    public BFacets deviceFacets;
    public BValue  conversion;
    public BValue  tuningPolicyName;
    public BValue  readValue;
    public BValue  writeValue;
}
