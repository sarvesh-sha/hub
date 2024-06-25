/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.bcsv3;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.bacnet.BBacnetDevice;
import com.optio3.product.importers.niagara.baja.sys.BValue;

@ModuleTypeAnnotation(module = "bcsv3", type = "BcpBacnetDevice")
public class BBcpBacnetDevice extends BBacnetDevice
{
    public BValue restStatus;
    public BValue BcpParameters;
    public BValue maxCovSubscriptions;
}

