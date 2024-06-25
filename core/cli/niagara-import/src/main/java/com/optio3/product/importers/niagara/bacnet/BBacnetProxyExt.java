/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.bacnet;

import com.optio3.product.importers.niagara.baja.driver.BProxyExt;
import com.optio3.product.importers.niagara.baja.sys.BFacets;
import com.optio3.product.importers.niagara.baja.sys.BValue;

public abstract class BBacnetProxyExt extends BProxyExt
{
    public BFacets                 deviceFacets;
    public BBacnetObjectIdentifier objectId;
    public BValue                  propertyId;
    public BValue                  propertyArrayIndex;
    public BValue                  dataType;
    public BValue                  readStatus;
    public BValue                  writeStatus;
}