/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.bacnet;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.driver.BDeviceNetwork;
import com.optio3.product.importers.niagara.baja.sys.BValue;

@ModuleTypeAnnotation(module = "bacnet", type = "BacnetNetwork")
public class BBacnetNetwork extends BDeviceNetwork
{
    public BValue             worker; // BBacnetWorker
    public BValue             writeWorker; // BBacnetWorker
    public BValue             bacnetComm; // BBacnetStack
    public BLocalBacnetDevice localDevice;
    public BValue             tuningPolicies; // BBacnetTuningPolicyMap
    public BValue             covWorker; // BBacnetCovWorker
}

