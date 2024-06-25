/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.util.concurrent.TimeUnit;

import javax.persistence.LockTimeoutException;

public interface ITableLockProvider
{
    TableLockHolder lockTable(SessionProvider sessionProvider,
                              Class<?> classForTableToLock,
                              long timeout,
                              TimeUnit unit) throws
                                             LockTimeoutException;

    TableLockHolder lockRecord(SessionProvider sessionProvider,
                               Class<?> classForTableToLock,
                               String subId,
                               long timeout,
                               TimeUnit unit) throws
                                              LockTimeoutException;
}
