/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.baja.driver;

import java.util.Collections;
import java.util.List;

import com.optio3.product.importers.niagara.baja.sys.BComponent;
import com.optio3.product.importers.niagara.baja.sys.BFolder;
import com.optio3.util.function.BiConsumerWithException;

public abstract class BDeviceNetwork extends BComponent
{
    public <T extends BDevice, F extends BFolder> void enumerateDevices(Class<T> deviceClz,
                                                                        Class<F> folderClz,
                                                                        BiConsumerWithException<T, List<F>> callback) throws
                                                                                                                      Exception
    {
        enumerateWithFolders(deviceClz, folderClz, Collections.emptyList(), callback);
    }
}
