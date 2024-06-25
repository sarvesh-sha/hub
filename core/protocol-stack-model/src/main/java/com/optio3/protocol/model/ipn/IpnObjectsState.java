/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn;

import com.optio3.serialization.Reflection;
import com.optio3.util.function.ConsumerWithException;

public interface IpnObjectsState
{
    void enumerateValues(boolean sorted,
                         ConsumerWithException<IpnObjectModel> callback) throws
                                                                         Exception;

    int size();

    IpnObjectModel set(IpnObjectModel object);

    IpnObjectModel getById(IpnObjectModel object);

    <T extends IpnObjectModel> T getByClass(Class<T> clz);

    default <T extends IpnObjectModel> T ensure(Class<T> clz)
    {
        T res = getByClass(clz);
        if (res == null)
        {
            res = Reflection.newInstance(clz);
            set(res);
        }

        return res;
    }
}
