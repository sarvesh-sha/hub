/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn;

public interface IpnWorker
{
    void startWorker() throws
                       Exception;

    void stopWorker() throws
                      Exception;

    <T> T accessSubManager(Class<T> clz);
}
