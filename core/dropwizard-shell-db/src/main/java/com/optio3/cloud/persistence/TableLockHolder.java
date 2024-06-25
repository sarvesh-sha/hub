/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

public abstract class TableLockHolder implements AutoCloseable
{
    @Override
    public final void close()
    {
        closeInner();
    }

    protected abstract void closeInner();
}
