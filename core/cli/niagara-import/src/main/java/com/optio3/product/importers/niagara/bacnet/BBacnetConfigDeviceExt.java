/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.bacnet;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.driver.BDeviceExt;

@ModuleTypeAnnotation(module = "bacnet", type = "BacnetConfigDeviceExt")
public class BBacnetConfigDeviceExt extends BDeviceExt
{
    public BBacnetDeviceObject deviceObject;
}
