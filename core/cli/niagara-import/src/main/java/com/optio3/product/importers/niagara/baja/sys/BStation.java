/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.baja.sys;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.driver.BDriverContainer;

@ModuleTypeAnnotation(module = "baja", type = "Station")
public class BStation extends BComponent
{
    public BDriverContainer Drivers;
}

