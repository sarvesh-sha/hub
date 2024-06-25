/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.baja.driver;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.sys.BComponent;
import com.optio3.util.function.ConsumerWithException;

@ModuleTypeAnnotation(module = "driver", type = "DriverContainer")
public class BDriverContainer extends BComponent
{
    public void enumerateNetworks(ConsumerWithException<BDeviceNetwork> callback) throws
                                                                                  Exception
    {
        enumerate(BDeviceNetwork.class, callback::accept);
    }
}
