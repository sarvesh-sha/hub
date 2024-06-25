/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.bacnet;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.sys.BStruct;
import com.optio3.product.importers.niagara.baja.sys.BValue;

@ModuleTypeAnnotation(module = "bacnet", type = "BacnetDeviceObjectPropertyReference")
public class BBacnetDeviceObjectPropertyReference extends BStruct
{
    public BBacnetObjectIdentifier objectId;
    public BValue                  propertyId;
    public BValue                  propertyArrayIndex;
    public BBacnetObjectIdentifier deviceId;
}

